package com.example.core.workflows;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.core.workflows.Fakes.fake;
import static com.example.core.workflows.Fakes.processArgs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProposalTest {

    private static final String VALID =
        "A practical guide to planning a weekend of surfing on the Portuguese coast.";

    private final Map<String, Object> pageContent = new HashMap<>();
    private final List<String> commits = new ArrayList<>();

    private final ResourceResolver resolver = fake(ResourceResolver.class, Map.of(
        "adaptTo", fake(PageManager.class, Map.of("getContainingPage", fake(Page.class, Map.of(
            "getTitle", "Surf Camp", "getDescription", "", "getPath", "/content/site/en/page")))),
        "getResource", fake(Resource.class, Map.of("adaptTo", Fakes.modifiableValueMap(pageContent))),
        "commit", (java.util.function.Function<Object[], Object>) a -> commits.add("commit") ? null : null));

    @Test
    void contractAcceptsOnlyTheExpectedShape() throws Exception {
        assertEquals(VALID, DescriptionRules.fromModelReply("{\"description\": \"" + VALID + "\"}"));
        assertThrows(InvalidProposalException.class, () -> DescriptionRules.fromModelReply("Sure! Here it is"));
        assertThrows(InvalidProposalException.class, () -> DescriptionRules.fromModelReply("{\"text\": \"" + VALID + "\"}"));
        assertThrows(InvalidProposalException.class, () -> DescriptionRules.validate("<b>" + VALID + "</b>"));
        assertThrows(InvalidProposalException.class, () -> DescriptionRules.validate("Short."));
        assertThrows(InvalidProposalException.class, () -> DescriptionRules.validate("x".repeat(161)));
    }

    @Test
    void invalidReplyIsRetriedWithCompactError() throws Exception {
        List<String> prompts = new ArrayList<>();
        AiProposeDescriptionProcess propose = withClient(prompts, "not json", "{\"description\": \"" + VALID + "\"}");
        Fakes.Step step = Fakes.Step.of("wi-10", List.of(), resolver);

        propose.execute(step.item(), step.session(), processArgs(null));

        assertEquals("ready", step.workflowMetadata().get("aiProposalStatus"));
        assertEquals(VALID, step.workflowMetadata().get("aiProposal"));
        assertEquals(AiProposeDescriptionProcess.PROMPT_VERSION, step.workflowMetadata().get("aiPromptVersion"));
        assertEquals(2, prompts.size());
        assertTrue(prompts.get(1).contains("previous reply was rejected"));
    }

    @Test
    void retriedStepDoesNotCallTheModelAgain() throws Exception {
        List<String> prompts = new ArrayList<>();
        AiProposeDescriptionProcess propose = withClient(prompts, "{\"description\": \"" + VALID + "\"}");
        Fakes.Step step = Fakes.Step.of("wi-11", List.of(), resolver);

        propose.execute(step.item(), step.session(), processArgs(null));
        propose.execute(step.item(), step.session(), processArgs(null));

        assertEquals(1, prompts.size());
    }

    @Test
    void attemptsAreBoundedAndExhaustionGoesToTheReviewer() throws Exception {
        List<String> prompts = new ArrayList<>();
        AiProposeDescriptionProcess propose = withClient(prompts, "a", "b", "c", "d");
        Fakes.Step step = Fakes.Step.of("wi-12", List.of(), resolver);

        propose.execute(step.item(), step.session(), processArgs(null));

        assertEquals(3, prompts.size());
        assertEquals("invalid", step.workflowMetadata().get("aiProposalStatus"));
    }

    @Test
    void unapprovedProposalIsNotApplied() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-20", List.of(), resolver);
        step.workflowMetadata().put("aiProposal", VALID);

        new ApplyApprovedDescriptionProcess().execute(step.item(), step.session(), processArgs(null));

        assertEquals("skipped: not approved", step.workflowMetadata().get("aiApplyOutcome"));
        assertTrue(commits.isEmpty());
    }

    @Test
    void approvedProposalIsApplied() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-21", List.of(), resolver);
        step.workflowMetadata().put("lastDecision", "approve");
        step.workflowMetadata().put("aiProposal", VALID);

        new ApplyApprovedDescriptionProcess().execute(step.item(), step.session(), processArgs(null));

        assertEquals("applied", step.workflowMetadata().get("aiApplyOutcome"));
        assertEquals(VALID, pageContent.get("jcr:description"));
    }

    @Test
    void reviewerEditFromTheDialogWinsAndIsRemoved() throws Exception {
        String edit = "Reviewer wording: a relaxed surf weekend on the Portuguese coast, lessons included.";
        pageContent.put("aiEditedDescription", edit);
        Fakes.Step step = Fakes.Step.of("wi-22", List.of(), resolver);
        step.workflowMetadata().put("lastDecision", "approve");
        step.workflowMetadata().put("aiProposal", VALID);

        new ApplyApprovedDescriptionProcess().execute(step.item(), step.session(), processArgs(null));

        assertEquals("applied (edited by reviewer)", step.workflowMetadata().get("aiApplyOutcome"));
        assertEquals(edit, pageContent.get("jcr:description"));
        assertFalse(pageContent.containsKey("aiEditedDescription"), "review data must not be published");
    }

    @Test
    void reviewerEditsAreValidatedToo() throws Exception {
        pageContent.put("aiEditedDescription", "<script>x</script>");
        Fakes.Step step = Fakes.Step.of("wi-23", List.of(), resolver);
        step.workflowMetadata().put("lastDecision", "approve");

        new ApplyApprovedDescriptionProcess().execute(step.item(), step.session(), processArgs(null));

        assertTrue(String.valueOf(step.workflowMetadata().get("aiApplyOutcome")).startsWith("rejected"));
        assertFalse(pageContent.containsKey("jcr:description"));
    }

    private static AiProposeDescriptionProcess withClient(List<String> prompts, String... replies)
            throws ReflectiveOperationException {
        Deque<String> queue = new ArrayDeque<>(List.of(replies));
        AiProposeDescriptionProcess process = new AiProposeDescriptionProcess();
        Field field = AiProposeDescriptionProcess.class.getDeclaredField("modelClient");
        field.setAccessible(true);
        field.set(process, new ContentModelClient() {
            @Override
            public String complete(String systemPrompt, String userPrompt) {
                prompts.add(userPrompt);
                return queue.poll();
            }

            @Override
            public String modelId() {
                return "fake-model";
            }
        });
        return process;
    }
}
