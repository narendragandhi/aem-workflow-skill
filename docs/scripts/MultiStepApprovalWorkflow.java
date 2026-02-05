package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Multi-Step Approval Workflow Components for AEM as a Cloud Service.
 *
 * This file contains multiple components for implementing hierarchical
 * approval workflows with escalation support:
 *
 * 1. HierarchicalApprovalChooser - Routes to appropriate approvers based on level
 * 2. ApprovalDecisionRecorder - Records approval/rejection decisions
 * 3. EscalationCheckProcess - Checks for approval timeouts and escalates
 *
 * Workflow Model Structure:
 * [Start] → [Initial Review] → [Dept Approval] → [Final Approval] → [Publish] → [End]
 *               ↓                    ↓                  ↓
 *           [Reject] → → → → → [Notify Author & Revise] ← ← ←
 */

// ============================================================================
// COMPONENT 1: Hierarchical Participant Chooser
// ============================================================================

@Component(
    service = ParticipantStepChooser.class,
    property = {
        "chooser.label=Hierarchical Approval Chooser"
    }
)
public class HierarchicalApprovalChooser implements ParticipantStepChooser {

    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalApprovalChooser.class);

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession,
                                 MetaDataMap metaDataMap) throws WorkflowException {

        try {
            // Get current approval level from workflow metadata
            MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();
            int approvalLevel = wfMetadata.get("approvalLevel", 1);

            // Get content path for department routing
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            String department = extractDepartment(payloadPath);

            // Determine approver based on level and department
            String approverGroup = getApproverGroup(approvalLevel, department);

            // Record step start time for escalation tracking
            wfMetadata.put("currentStepStartTime", new Date());
            wfMetadata.put("currentStepLevel", approvalLevel);

            // Increment approval level for next step
            wfMetadata.put("approvalLevel", approvalLevel + 1);

            LOG.info("Routing to {} for level {} approval of {}", approverGroup, approvalLevel, payloadPath);
            return approverGroup;

        } catch (Exception e) {
            LOG.error("Failed to determine approver", e);
            return "administrators"; // Fallback
        }
    }

    /**
     * Extract department from content path.
     * Customize this based on your content structure.
     *
     * Examples:
     * /content/mysite/marketing/campaigns/page → marketing
     * /content/mysite/hr/policies/page → hr
     */
    private String extractDepartment(String path) {
        if (path == null) {
            return "default";
        }
        String[] segments = path.split("/");
        if (segments.length > 3) {
            String department = segments[3];
            return sanitizeGroupName(department);
        }
        return "default";
    }

    /**
     * Sanitize group name to prevent injection attacks.
     * Only allows alphanumeric characters, hyphens, and underscores.
     */
    private String sanitizeGroupName(String input) {
        if (input == null) {
            return "default";
        }
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }

    /**
     * Determine approver group based on approval level and department.
     *
     * Level 1: Department reviewers (peer review)
     * Level 2: Department managers (management approval)
     * Level 3: Content governance (final approval)
     */
    private String getApproverGroup(int level, String department) {
        String sanitizedDept = sanitizeGroupName(department);
        switch (level) {
            case 1:
                return sanitizedDept + "-reviewers";
            case 2:
                return sanitizedDept + "-managers";
            case 3:
                return "content-governance";
            default:
                return "administrators";
        }
    }
}

// ============================================================================
// COMPONENT 2: Approval Decision Recorder
// ============================================================================

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

        try {
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
            MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();

            // Get decision from process arguments
            // Format: DECISION:approve|reject,COMMENTS:optional comments
            String args = metaDataMap.get("PROCESS_ARGS", "");
            String decision = extractArg(args, "DECISION", "unknown");
            String comments = extractArg(args, "COMMENTS", "");

            // Get approver information
            String approver = workItem.getCurrentAssignee();
            if (approver == null) {
                approver = workItem.getWorkflow().getInitiator();
            }

            // Record the decision
            recordApprovalDecision(wfMetadata, workItem, approver, decision, comments);

            // Set route for workflow model (determines next step)
            if ("reject".equalsIgnoreCase(decision)) {
                wfMetadata.put("workflowRoute", "reject");
                wfMetadata.put("rejectionReason", comments);
                LOG.info("Content rejected by {} - routing to revision", approver);
            } else {
                wfMetadata.put("workflowRoute", "approve");
                LOG.info("Content approved by {} - proceeding to next level", approver);
            }

        } catch (Exception e) {
            LOG.error("Failed to record approval decision", e);
            throw new WorkflowException("Decision recording failed", e);
        }
    }

    private void recordApprovalDecision(MetaDataMap metadata, WorkItem workItem,
                                         String approver, String decision, String comments) {
        // Build approval history
        String historyKey = "approvalHistory";
        String existingHistory = metadata.get(historyKey, "");

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

        String updatedHistory = existingHistory.isEmpty() ? newEntry : existingHistory + "\n" + newEntry;
        metadata.put(historyKey, updatedHistory);

        // Store latest decision info
        metadata.put("lastApprover", approver);
        metadata.put("lastDecision", decision);
        metadata.put("lastDecisionTime", new Date());

        LOG.debug("Recorded approval: {}", newEntry);
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

// ============================================================================
// COMPONENT 3: Escalation Check Process
// ============================================================================

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Escalation Check Process"
    }
)
public class EscalationCheckProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationCheckProcess.class);

    // Default escalation threshold (can be overridden via process args)
    private static final long DEFAULT_ESCALATION_HOURS = 48;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();

        // Get configurable threshold from process args
        String args = metaDataMap.get("PROCESS_ARGS", "");
        long thresholdHours = parseThreshold(args, DEFAULT_ESCALATION_HOURS);

        // Get step start time
        Date stepStartTime = wfMetadata.get("currentStepStartTime", Date.class);
        if (stepStartTime == null) {
            // First check - record start time
            stepStartTime = new Date();
            wfMetadata.put("currentStepStartTime", stepStartTime);
            wfMetadata.put("escalated", false);
            LOG.debug("Initialized escalation tracking for workflow: {}", workItem.getWorkflow().getId());
            return;
        }

        // Calculate time elapsed
        long hoursElapsed = TimeUnit.MILLISECONDS.toHours(
            System.currentTimeMillis() - stepStartTime.getTime()
        );

        if (hoursElapsed >= thresholdHours && !wfMetadata.get("escalated", false)) {
            LOG.warn("Workflow {} exceeded {} hour threshold (elapsed: {} hours), escalating",
                workItem.getWorkflow().getId(), thresholdHours, hoursElapsed);

            // Mark for escalation
            wfMetadata.put("escalated", true);
            wfMetadata.put("escalationTime", new Date());
            wfMetadata.put("escalationReason",
                String.format("Approval timeout: %d hours exceeded threshold of %d hours",
                    hoursElapsed, thresholdHours));

            // Get current approval level for escalation target
            int currentLevel = wfMetadata.get("currentStepLevel", 1);
            wfMetadata.put("escalationTarget", getEscalationTarget(currentLevel));

            // Record in history
            String history = wfMetadata.get("approvalHistory", "");
            String escalationEntry = String.format("[%tF %tT] ESCALATION: Timeout after %d hours",
                new Date(), new Date(), hoursElapsed);
            wfMetadata.put("approvalHistory", history + "\n" + escalationEntry);

            LOG.info("Escalation recorded for workflow: {}", workItem.getWorkflow().getId());
        } else {
            LOG.debug("No escalation needed - {} hours elapsed of {} hour threshold",
                hoursElapsed, thresholdHours);
        }
    }

    private long parseThreshold(String args, long defaultValue) {
        if (args != null && !args.isEmpty()) {
            try {
                for (String part : args.split(",")) {
                    if (part.startsWith("THRESHOLD_HOURS:")) {
                        return Long.parseLong(part.substring("THRESHOLD_HOURS:".length()).trim());
                    }
                }
            } catch (NumberFormatException e) {
                LOG.warn("Invalid threshold in process args, using default: {}", defaultValue);
            }
        }
        return defaultValue;
    }

    private String getEscalationTarget(int currentLevel) {
        // Escalate to the next level's approvers or administrators
        switch (currentLevel) {
            case 1:
                return "department-managers";
            case 2:
                return "content-governance";
            default:
                return "administrators";
        }
    }
}

// ============================================================================
// COMPONENT 4: Workflow Completion Notifier
// ============================================================================

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

        MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();
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
