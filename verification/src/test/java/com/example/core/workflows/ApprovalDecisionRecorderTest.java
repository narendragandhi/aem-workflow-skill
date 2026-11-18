package com.example.core.workflows;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.core.workflows.Fakes.processArgs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalDecisionRecorderTest {

    private final ApprovalDecisionRecorder recorder = new ApprovalDecisionRecorder();

    @Test
    void approveBranchRecordsReviewerAndComment() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-1", List.of("author1|submitted", "reviewer-jane|looks good"), null);

        recorder.execute(step.item(), step.session(), processArgs("DECISION:approve"));

        assertEquals("approve", step.workflowMetadata().get("lastDecision"));
        assertEquals("approve", step.workflowMetadata().get("workflowRoute"));
        assertEquals("reviewer-jane", step.workflowMetadata().get("lastApprover"),
            "the reviewer is the last participant in history, not this process step");
        assertTrue(String.valueOf(step.workflowMetadata().get("approvalHistory")).contains("looks good"));
    }

    @Test
    void rejectBranchUsesTheCommentAsReason() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-2", List.of("reviewer-jane|needs work"), null);

        recorder.execute(step.item(), step.session(), processArgs("DECISION:reject"));

        assertEquals("reject", step.workflowMetadata().get("lastDecision"));
        assertEquals("needs work", step.workflowMetadata().get("rejectionReason"));
    }

    @Test
    void retryOfSameWorkItemDoesNotDuplicateHistory() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-3", List.of("reviewer-jane"), null);
        recorder.execute(step.item(), step.session(), processArgs("DECISION:approve"));
        Object history = step.workflowMetadata().get("approvalHistory");

        recorder.execute(step.item(), step.session(), processArgs("DECISION:approve"));

        assertEquals(history, step.workflowMetadata().get("approvalHistory"));
    }

    @Test
    void missingDecisionArgumentFailsClosed() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-4", List.of("reviewer-jane"), null);

        recorder.execute(step.item(), step.session(), processArgs(null));

        assertEquals("reject", step.workflowMetadata().get("lastDecision"));
        assertTrue(String.valueOf(step.workflowMetadata().get("rejectionReason")).contains("misconfigured"));
    }

    @Test
    void misspelledDecisionArgumentFailsClosed() throws Exception {
        Fakes.Step step = Fakes.Step.of("wi-5", List.of("reviewer-jane"), null);

        recorder.execute(step.item(), step.session(), processArgs("DECISION:aprove"));

        assertEquals("reject", step.workflowMetadata().get("lastDecision"));
    }
}
