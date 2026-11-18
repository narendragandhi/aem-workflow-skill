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
import java.util.concurrent.TimeUnit;

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

        MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();

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
