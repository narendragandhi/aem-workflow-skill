package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.WorkItem;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class LongRunningJobs {

    static final String TOPIC = "com/example/workflow/longrunning";
    static final String RESULTS_ROOT = "/var/example/workflow-job-results";
    static final String PROP_KEY = "idempotencyKey";
    static final String PROP_PAYLOAD = "payload";
    static final String STATUS_SUCCEEDED = "succeeded";
    static final String STATUS_FAILED = "failed";

    private LongRunningJobs() {
    }

    /**
     * Stable across retries of the same step: the same workflow instance and model
     * node always produce the same key. If the step sits inside a loop (e.g. a
     * revise-and-resubmit cycle), add a loop counter from workflow metadata so
     * each pass gets its own key.
     */
    static String keyFor(WorkItem workItem) throws WorkflowException {
        String raw = workItem.getWorkflow().getId() + "|" + workItem.getNode().getId();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new WorkflowException("SHA-256 unavailable", e);
        }
    }

    static Resource result(ResourceResolver resolver, String key) {
        return resolver.getResource(RESULTS_ROOT + "/" + key);
    }
}
