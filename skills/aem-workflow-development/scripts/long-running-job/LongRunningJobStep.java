package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowExternalProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Component(
    service = WorkflowExternalProcess.class,
    property = {
        "process.label=Run Long-Running Job and Wait"
    }
)
public class LongRunningJobStep implements WorkflowExternalProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LongRunningJobStep.class);

    @Reference
    private JobManager jobManager;

    @Override
    public Serializable execute(WorkItem workItem, WorkflowSession session, MetaDataMap args)
            throws WorkflowException {

        String key = LongRunningJobs.keyFor(workItem);
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);

        // Retry after the job already finished: nothing to queue, just wait on the result
        if (LongRunningJobs.result(resolver, key) != null) {
            return key;
        }

        // Retry while the job is still queued or running: reuse it
        Map<String, Object> match = Map.of(LongRunningJobs.PROP_KEY, key);
        if (jobManager.getJob(LongRunningJobs.TOPIC, match) == null) {
            Map<String, Object> props = new HashMap<>(match);
            props.put(LongRunningJobs.PROP_PAYLOAD, workItem.getWorkflowData().getPayload().toString());
            if (jobManager.addJob(LongRunningJobs.TOPIC, props) == null) {
                throw new WorkflowException("Could not queue job for " + key);
            }
        }

        workItem.getWorkflowData().getMetaDataMap().put("longRunningJobKey", key);
        return key; // the handle passed to hasFinished/handleResult
    }

    @Override
    public boolean hasFinished(Serializable handle, WorkItem workItem, WorkflowSession session,
                               MetaDataMap args) {
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
        resolver.refresh(); // see results the consumer committed in another session
        return LongRunningJobs.result(resolver, handle.toString()) != null;
    }

    @Override
    public void handleResult(Serializable handle, WorkItem workItem, WorkflowSession session,
                             MetaDataMap args) throws WorkflowException {
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
        Resource result = LongRunningJobs.result(resolver, handle.toString());
        if (result == null) {
            throw new WorkflowException("Result disappeared for job " + handle);
        }

        ValueMap props = result.getValueMap();
        MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();
        String status = props.get("status", LongRunningJobs.STATUS_FAILED);
        wfMetadata.put("jobOutcome", status);

        // A permanent failure is a business outcome, not an exception: record a
        // short reason and let an OR Split route on jobOutcome to a person.
        // Throwing here would only make the engine retry something that cannot succeed.
        if (!LongRunningJobs.STATUS_SUCCEEDED.equals(status)) {
            wfMetadata.put("jobFailureReason", props.get("reason", "unknown"));
            LOG.warn("Job {} failed permanently: {}", handle, props.get("reason", "unknown"));
        }
    }
}
