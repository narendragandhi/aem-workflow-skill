package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Approval Workflow Completion Notifier"
    }
)
public class ApprovalCompletionNotifier implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalCompletionNotifier.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();
        String payload = workItem.getWorkflowData().getPayload().toString();

        // Get approval history
        String approvalHistory = wfMetadata.get("approvalHistory", "No history available");
        String initiator = workItem.getWorkflow().getInitiator();
        boolean wasEscalated = wfMetadata.get("escalated", false);

        // Determine final outcome
        String lastDecision = wfMetadata.get("lastDecision", "unknown");
        boolean approved = "approve".equalsIgnoreCase(lastDecision);

        LOG.info("Workflow completed for {}: outcome={}, escalated={}",
            payload, approved ? "APPROVED" : "REJECTED", wasEscalated);

        // Build notification content
        StringBuilder notification = new StringBuilder();
        notification.append("Workflow Completed\n");
        notification.append("==================\n");
        notification.append("Content: ").append(payload).append("\n");
        notification.append("Outcome: ").append(approved ? "APPROVED" : "REJECTED").append("\n");
        notification.append("Escalated: ").append(wasEscalated ? "Yes" : "No").append("\n");
        notification.append("\nApproval History:\n").append(approvalHistory);

        // Store notification for potential email service integration
        wfMetadata.put("completionNotification", notification.toString());
        wfMetadata.put("workflowCompleted", true);
        wfMetadata.put("workflowCompletedTime", new Date());
        wfMetadata.put("workflowOutcome", approved ? "approved" : "rejected");

        // In production: integrate with email service
        // emailService.sendNotification(initiator, "Workflow Complete", notification.toString());

        LOG.debug("Completion notification prepared for: {}", initiator);
    }
}
