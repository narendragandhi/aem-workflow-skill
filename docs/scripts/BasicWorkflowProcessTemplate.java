package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic workflow process template for AEM as a Cloud Service.
 * Replace "BasicWorkflowProcess" with your actual process name.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Basic Workflow Process Template"
    }
)
public class BasicWorkflowProcessTemplate implements WorkflowProcess {
    
    private static final Logger LOG = LoggerFactory.getLogger(BasicWorkflowProcessTemplate.class);
    
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        LOG.info("Starting workflow process execution");
        
        try {
            // 1. Get ResourceResolver from WorkflowSession
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resolver == null) {
                throw new WorkflowException("Unable to obtain ResourceResolver from WorkflowSession");
            }
            
            // 2. Get the workflow payload
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Processing workflow for payload: {}", payloadPath);
            
            // 3. Get the resource
            Resource resource = resolver.getResource(payloadPath);
            if (resource == null) {
                LOG.warn("Resource not found at path: {}", payloadPath);
                return;
            }
            
            // 4. Read process arguments (if any)
            String processArgs = metaDataMap.get("PROCESS_ARGS", String.class);
            LOG.debug("Process arguments: {}", processArgs);
            
            // 5. Access workflow metadata (shared across all steps)
            MetaDataMap workflowMetadata = workItem.getWorkflow().getMetaDataMap();
            
            // 6. TODO: Implement your custom workflow logic here
            performCustomProcessing(resource, resolver, processArgs);
            
            // 7. Store data for next workflow step (if needed)
            workflowMetadata.put("processedBy", this.getClass().getSimpleName());
            workflowMetadata.put("processedAt", System.currentTimeMillis());
            
            LOG.info("Workflow processing completed successfully for: {}", payloadPath);
            
        } catch (WorkflowException e) {
            // Re-throw workflow exceptions
            throw e;
            
        } catch (Exception e) {
            LOG.error("Error in workflow process", e);
            // Throwing WorkflowException will cause workflow to retry
            throw new WorkflowException("Failed to process workflow", e);
        }
    }
    
    /**
     * Implement your custom processing logic here.
     * 
     * @param resource The workflow payload resource
     * @param resolver The resource resolver
     * @param processArgs Arguments passed to the process step
     */
    private void performCustomProcessing(Resource resource, ResourceResolver resolver, String processArgs) {
        // TODO: Add your custom implementation
        LOG.info("Performing custom processing on resource: {}", resource.getPath());
    }
}
