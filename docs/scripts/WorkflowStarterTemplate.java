package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Service template for programmatically starting workflows in AEM as a Cloud Service.
 * Use this pattern when you need to trigger workflows from:
 * - Scheduled jobs
 * - Event listeners
 * - Servlets
 * - Custom services
 */
@Component(service = WorkflowStarter.class)
public class WorkflowStarterTemplate {
    
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowStarterTemplate.class);
    
    // Common workflow model paths in AEM Cloud Service
    // NOTE: DAM Update Asset workflow replaced by Asset Microservices in Cloud Service
    // Use post-processing workflows for custom asset operations
    private static final String REQUEST_FOR_ACTIVATION = "/var/workflow/models/request_for_activation";
    private static final String REQUEST_FOR_DEACTIVATION = "/var/workflow/models/request_for_deactivation";
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
    /**
     * Start a workflow for a given resource path.
     * 
     * @param resourcePath The JCR path of the resource to process
     * @param workflowModelPath The path to the workflow model
     * @param metadata Optional metadata to pass to the workflow
     * @return The started workflow ID, or null if failed
     */
    public String startWorkflow(String resourcePath, String workflowModelPath, Map<String, Object> metadata) {
        ResourceResolver resolver = null;
        
        try {
            // Get service resource resolver
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "workflow-service");
            
            resolver = resolverFactory.getServiceResourceResolver(authInfo);
            
            // Get workflow session
            WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);
            if (workflowSession == null) {
                LOG.error("Unable to obtain WorkflowSession");
                return null;
            }
            
            // Get workflow model
            WorkflowModel workflowModel = workflowSession.getModel(workflowModelPath);
            if (workflowModel == null) {
                LOG.error("Workflow model not found: {}", workflowModelPath);
                return null;
            }
            
            // Create workflow data (payload)
            WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", resourcePath);
            
            // Start the workflow
            Workflow workflow = workflowSession.startWorkflow(workflowModel, workflowData, metadata);
            
            String workflowId = workflow.getId();
            LOG.info("Successfully started workflow: {} for resource: {}", workflowId, resourcePath);
            
            return workflowId;
            
        } catch (LoginException e) {
            LOG.error("Failed to obtain resource resolver", e);
            return null;
            
        } catch (WorkflowException e) {
            LOG.error("Failed to start workflow for resource: {}", resourcePath, e);
            return null;
            
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }
    
    /**
     * Start a workflow with custom metadata.
     * 
     * @param resourcePath The resource to process
     * @param workflowModelPath The workflow model path
     * @return Workflow ID or null
     */
    public String startWorkflowWithMetadata(String resourcePath, String workflowModelPath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("initiatedBy", "automated-service");
        metadata.put("priority", "high");
        metadata.put("timestamp", System.currentTimeMillis());
        
        return startWorkflow(resourcePath, workflowModelPath, metadata);
    }
    
    /**
     * IMPORTANT: DAM Update Asset workflow is replaced by Asset Microservices in AEM Cloud Service.
     * This method is for reference only - asset processing happens automatically via microservices.
     * 
     * For custom asset processing:
     * 1. Configure post-processing workflows per folder in DAM
     * 2. Use Asset Compute workers for advanced processing
     * 3. Workflows run AFTER Asset Microservices complete
     * 
     * @param assetPath Path to the DAM asset
     * @return null (workflow not applicable in Cloud Service)
     */
    @Deprecated
    public String startDamUpdateAssetWorkflow(String assetPath) {
        LOG.warn("DAM Update Asset workflow is deprecated in AEM Cloud Service");
        LOG.warn("Asset processing is handled by Asset Microservices");
        LOG.warn("Configure post-processing workflows for custom operations");
        return null;
    }
    
    /**
     * Start activation workflow for a page.
     * 
     * @param pagePath Path to the page to activate
     * @return Workflow ID or null
     */
    public String startActivationWorkflow(String pagePath) {
        LOG.info("Starting activation workflow for: {}", pagePath);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("replicateAsParticipant", false); // Don't require user approval
        
        return startWorkflow(pagePath, REQUEST_FOR_ACTIVATION, metadata);
    }
    
    /**
     * Bulk start workflows for multiple resources.
     * 
     * @param resourcePaths List of resource paths
     * @param workflowModelPath The workflow model to use
     * @return Number of successfully started workflows
     */
    public int bulkStartWorkflows(java.util.List<String> resourcePaths, String workflowModelPath) {
        LOG.info("Starting bulk workflows for {} resources", resourcePaths.size());
        
        int successCount = 0;
        
        for (String resourcePath : resourcePaths) {
            String workflowId = startWorkflow(resourcePath, workflowModelPath, null);
            if (workflowId != null) {
                successCount++;
            }
        }
        
        LOG.info("Successfully started {} out of {} workflows", successCount, resourcePaths.size());
        return successCount;
    }
    
    /**
     * Get workflow status information.
     * 
     * @param workflowId The workflow instance ID
     * @return Workflow state or null
     */
    public String getWorkflowStatus(String workflowId) {
        ResourceResolver resolver = null;
        
        try {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "workflow-service");
            
            resolver = resolverFactory.getServiceResourceResolver(authInfo);
            WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);
            
            if (workflowSession != null) {
                Workflow workflow = workflowSession.getWorkflow(workflowId);
                if (workflow != null) {
                    return workflow.getState();
                }
            }
            
        } catch (Exception e) {
            LOG.error("Failed to get workflow status for: {}", workflowId, e);
            
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
        
        return null;
    }
    
    /**
     * Terminate a running workflow.
     * 
     * @param workflowId The workflow instance ID
     * @return true if successfully terminated
     */
    public boolean terminateWorkflow(String workflowId) {
        ResourceResolver resolver = null;
        
        try {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "workflow-service");
            
            resolver = resolverFactory.getServiceResourceResolver(authInfo);
            WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);
            
            if (workflowSession != null) {
                Workflow workflow = workflowSession.getWorkflow(workflowId);
                if (workflow != null) {
                    workflowSession.terminateWorkflow(workflow);
                    LOG.info("Successfully terminated workflow: {}", workflowId);
                    return true;
                }
            }
            
        } catch (Exception e) {
            LOG.error("Failed to terminate workflow: {}", workflowId, e);
            
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
        
        return false;
    }
}
