package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=AI: Propose Page Description"
    }
)
public class AiProposeDescriptionProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AiProposeDescriptionProcess.class);

    // The prompt lives in your code and is versioned with it. Change the text,
    // change the version, so every stored proposal names the prompt that made it.
    static final String PROMPT_VERSION = "page-description/v1";
    static final String SYSTEM_PROMPT = """
        You write meta descriptions for web pages.
        Reply with JSON only, exactly: {"description": "<text>"}
        The description is plain text, 50-160 characters, with no HTML.
        Use only the page facts provided. Do not invent product claims.
        """;

    // Bounded attempts at producing a valid reply within one step execution
    private static final int MAX_ATTEMPTS = 3;

    @Reference
    private ContentModelClient modelClient;

    @Override
    public void execute(WorkItem workItem, WorkflowSession session, MetaDataMap args)
            throws WorkflowException {

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();

        // Idempotent: a retried step must not pay for (or change) a proposal
        // that already exists and may already be in front of a reviewer.
        if ("ready".equals(meta.get("aiProposalStatus", String.class))) {
            return;
        }

        String path = workItem.getWorkflowData().getPayload().toString();
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page page = pageManager == null ? null : pageManager.getContainingPage(path);
        if (page == null) {
            recordInvalid(meta, "payload is not a page: " + path);
            return;
        }

        // Own the context window: send only the facts this task needs, not the
        // whole page tree or the full workflow history.
        String pageFacts = "Title: " + nullToEmpty(page.getTitle())
            + "\nCurrent description: " + nullToEmpty(page.getDescription())
            + "\nPath: " + page.getPath();

        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // Compact errors into context: tell the model what was wrong, briefly
            String userPrompt = lastError == null ? pageFacts
                : pageFacts + "\n\nYour previous reply was rejected: " + lastError
                    + ". Reply again with valid JSON.";

            String reply;
            try {
                reply = modelClient.complete(SYSTEM_PROMPT, userPrompt);
            } catch (IOException e) {
                // Transient: let the workflow engine retry the step later
                throw new WorkflowException("Model call failed for " + path, e);
            }

            try {
                String description = DescriptionRules.fromModelReply(reply);
                meta.put("aiProposal", description);
                meta.put("aiPromptVersion", PROMPT_VERSION);
                meta.put("aiModel", modelClient.modelId());
                meta.put("aiProposalStatus", "ready");
                return;
            } catch (InvalidProposalException e) {
                lastError = e.getMessage();
                LOG.info("Attempt {} for {} rejected: {}", attempt, path, lastError);
            }
        }

        // Out of attempts: do not fail the workflow. The reviewer sees why and
        // writes the description by hand in the dialog.
        recordInvalid(meta, lastError);
    }

    private static void recordInvalid(MetaDataMap meta, String reason) {
        meta.put("aiProposalStatus", "invalid");
        meta.put("aiProposalError", reason);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
