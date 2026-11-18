package com.example.core.workflows;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + LongRunningJobs.TOPIC
    }
)
public class LongRunningJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(LongRunningJobConsumer.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public JobResult process(Job job) {
        String key = job.getProperty(LongRunningJobs.PROP_KEY, String.class);
        String payload = job.getProperty(LongRunningJobs.PROP_PAYLOAD, String.class);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(
                Map.of(ResourceResolverFactory.SUBSERVICE, "workflow-job-results"))) {

            // Already done (a retry after a crash between work and acknowledgement)
            if (LongRunningJobs.result(resolver, key) != null) {
                return JobResult.OK;
            }

            try {
                // Pass the key to external systems that accept an idempotency key,
                // so a repeated call cannot repeat the external effect.
                performHeavyProcessing(payload, key);
            } catch (PermanentJobException e) {
                // Retrying cannot help: record the failure and stop retrying.
                writeResult(resolver, key, LongRunningJobs.STATUS_FAILED, e.getMessage());
                return JobResult.CANCEL;
            } catch (Exception e) {
                if (job.getRetryCount() >= job.getNumberOfRetries()) {
                    // Last attempt: record the failure so the workflow does not wait forever.
                    writeResult(resolver, key, LongRunningJobs.STATUS_FAILED,
                        "Retries exhausted: " + e.getMessage());
                    return JobResult.CANCEL;
                }
                LOG.warn("Transient failure for {} (attempt {}), will retry",
                    payload, job.getRetryCount() + 1, e);
                return JobResult.FAILED; // Sling retries per the queue configuration
            }

            writeResult(resolver, key, LongRunningJobs.STATUS_SUCCEEDED, null);
            return JobResult.OK;

        } catch (LoginException | PersistenceException e) {
            LOG.error("Could not record job result for {}", payload, e);
            return JobResult.FAILED;
        }
    }

    private void writeResult(ResourceResolver resolver, String key, String status, String reason)
            throws PersistenceException {
        Map<String, Object> props = new HashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("status", status);
        if (reason != null) {
            // Keep it short: this lands in workflow metadata and the Inbox
            props.put("reason", reason.length() > 500 ? reason.substring(0, 500) : reason);
        }
        ResourceUtil.getOrCreateResource(resolver, LongRunningJobs.RESULTS_ROOT + "/" + key,
            props, "sling:Folder", true);
    }

    private void performHeavyProcessing(String payloadPath, String idempotencyKey)
            throws PermanentJobException {
        // Video transcoding, large file processing, external API calls, etc.
        // Throw PermanentJobException for errors a retry cannot fix
        // (invalid input, 4xx from an external API).
    }

    /** Errors that retrying cannot fix. */
    public static class PermanentJobException extends Exception {

        private static final long serialVersionUID = 1L;

        public PermanentJobException(String message) {
            super(message);
        }
    }
}
