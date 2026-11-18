package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;

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
            MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();
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
