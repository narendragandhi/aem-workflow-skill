package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.List;

/**
 * Records the reviewer's decision at the start of an OR Split branch.
 *
 * Model each approval level as a participant step followed by an OR Split. The reviewer
 * picks the branch as "Next Step" when completing the item in the Inbox; put this step
 * first on each branch with PROCESS_ARGS DECISION:approve or DECISION:reject. The argument
 * is fixed per branch, but which branch runs is the reviewer's choice at run time.
 *
 * The reviewer and comment come from workflow history (the Inbox "Complete" dialog stores
 * the comment there). Results go to workItem.getWorkflowData().getMetaDataMap(), which the
 * engine persists between steps.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Approval Decision Recorder"
    }
)
public class ApprovalDecisionRecorder implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalDecisionRecorder.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();

        // Idempotency: the engine re-runs this work item when a step fails and is
        // retried. Without this guard each retry appends a duplicate history entry.
        if (workItem.getId().equals(wfMetadata.get("lastRecordedWorkItem", String.class))) {
            LOG.debug("Decision for work item {} already recorded; skipping", workItem.getId());
            return;
        }

        String decision = extractArg(metaDataMap.get("PROCESS_ARGS", ""), "DECISION", "")
            .trim().toLowerCase();
        HistoryItem review = findLastReview(workItem, workflowSession);
        String approver = review == null || review.getUserId() == null ? "unknown" : review.getUserId();
        String comments = review == null || review.getComment() == null ? "" : review.getComment();

        // Fail closed: anything other than an explicit "approve" is a rejection.
        // A missing or misspelled argument must never publish content.
        boolean approved = "approve".equals(decision);
        if (!approved && !"reject".equals(decision)) {
            comments = "Recorder misconfigured (DECISION='" + decision
                + "'); treated as rejection. " + comments;
            decision = "reject";
            LOG.warn("Workflow {}: no valid DECISION process argument, failing closed",
                workItem.getWorkflow().getId());
        }

        recordApprovalDecision(wfMetadata, workItem, approver, decision, comments);
        wfMetadata.put("workflowRoute", approved ? "approve" : "reject");
        if (!approved) {
            wfMetadata.put("rejectionReason", comments);
        }
        wfMetadata.put("lastRecordedWorkItem", workItem.getId());

        LOG.info("Content {} by {}", approved ? "approved" : "rejected", approver);
    }

    /** The most recent completed participant step: the review this branch follows. */
    private HistoryItem findLastReview(WorkItem workItem, WorkflowSession workflowSession) {
        try {
            List<HistoryItem> history = workflowSession.getHistory(workItem.getWorkflow());
            for (int i = history.size() - 1; i >= 0; i--) {
                HistoryItem item = history.get(i);
                WorkItem completed = item.getWorkItem();
                String type = completed == null ? null : completed.getNode().getType();
                if (WorkflowNode.TYPE_PARTICIPANT.equals(type)
                        || WorkflowNode.TYPE_DYNAMIC_PARTICIPANT.equals(type)) {
                    return item;
                }
            }
        } catch (WorkflowException e) {
            LOG.warn("Could not read workflow history to identify the reviewer", e);
        }
        return null;
    }

    private void recordApprovalDecision(MetaDataMap metadata, WorkItem workItem,
                                         String approver, String decision, String comments) {
        String existingHistory = metadata.get("approvalHistory", "");

        String stepTitle = "Unknown Step";
        try {
            stepTitle = workItem.getNode().getTitle();
        } catch (Exception e) {
            LOG.warn("Could not get step title", e);
        }

        String newEntry = String.format("[%tF %tT] %s: %s by %s%s",
            new Date(), new Date(),
            stepTitle,
            decision.toUpperCase(),
            approver,
            comments.isEmpty() ? "" : " - " + comments
        );

        metadata.put("approvalHistory", existingHistory.isEmpty() ? newEntry : existingHistory + "\n" + newEntry);
        metadata.put("lastApprover", approver);
        metadata.put("lastDecision", decision);
        metadata.put("lastDecisionTime", new Date());
    }

    private String extractArg(String args, String key, String defaultValue) {
        if (args == null || args.isEmpty()) {
            return defaultValue;
        }
        for (String part : args.split(",")) {
            if (part.startsWith(key + ":")) {
                return part.substring(key.length() + 1).trim();
            }
        }
        return defaultValue;
    }
}
