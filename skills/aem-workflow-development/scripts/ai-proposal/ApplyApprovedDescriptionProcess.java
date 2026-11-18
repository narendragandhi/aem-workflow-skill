package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the reviewed description. Place it on the Approve branch, after
 * {@link ApprovalDecisionRecorder} with DECISION:approve.
 *
 * Reviewer edits are optional. A Dialog Participant Step's fields are posted by the Inbox to
 * the payload, so a field named ./jcr:content/aiEditedDescription arrives as a property on
 * the page's jcr:content. This step uses it instead of the model's proposal, then removes it
 * so it is never published. Override the property name with PROCESS_ARGS EDIT_PROPERTY:name.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Apply Approved Description"
    }
)
public class ApplyApprovedDescriptionProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ApplyApprovedDescriptionProcess.class);
    private static final String DEFAULT_EDIT_PROPERTY = "aiEditedDescription";

    @Override
    public void execute(WorkItem workItem, WorkflowSession session, MetaDataMap args)
            throws WorkflowException {

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();

        // Fail closed: only a recorded approval applies anything, even if the model
        // routes here by mistake
        if (!"approve".equals(meta.get("lastDecision", ""))) {
            meta.put("aiApplyOutcome", "skipped: not approved");
            return;
        }

        String path = workItem.getWorkflowData().getPayload().toString();
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
        Resource content = resolver.getResource(path + "/jcr:content");
        ModifiableValueMap props = content == null ? null : content.adaptTo(ModifiableValueMap.class);
        if (props == null) {
            meta.put("aiApplyOutcome", "rejected: no writable jcr:content at " + path);
            return;
        }

        // A reviewer's edit wins over the model's proposal; both pass the same rules
        String editProperty = editProperty(args.get("PROCESS_ARGS", ""));
        String edited = props.get(editProperty, "").trim();
        String value;
        try {
            value = DescriptionRules.validate(edited.isEmpty() ? meta.get("aiProposal", "") : edited);
        } catch (InvalidProposalException e) {
            meta.put("aiApplyOutcome", "rejected: " + e.getMessage());
            return;
        }

        // Idempotent: setting the same value twice is a no-op
        boolean changed = !value.equals(props.get("jcr:description", String.class));
        if (changed) {
            props.put("jcr:description", value);
        }
        if (props.containsKey(editProperty)) {
            props.remove(editProperty); // review data does not belong in published content
            changed = true;
        }
        if (changed) {
            try {
                resolver.commit();
            } catch (PersistenceException e) {
                throw new WorkflowException("Could not save description for " + path, e);
            }
        }
        meta.put("aiApplyOutcome", edited.isEmpty() ? "applied" : "applied (edited by reviewer)");
        LOG.info("Applied reviewed description to {}", path);
    }

    private static String editProperty(String args) {
        for (String part : args == null ? new String[0] : args.split(",")) {
            if (part.startsWith("EDIT_PROPERTY:") && part.length() > "EDIT_PROPERTY:".length()) {
                return part.substring("EDIT_PROPERTY:".length()).trim();
            }
        }
        return DEFAULT_EDIT_PROPERTY;
    }
}
