package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Workflow process template for processing DAM assets in AEM as a Cloud Service.
 * Includes common patterns for asset manipulation.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=DAM Asset Processing Template"
    }
)
public class DamAssetWorkflowProcessTemplate implements WorkflowProcess {
    
    private static final Logger LOG = LoggerFactory.getLogger(DamAssetWorkflowProcessTemplate.class);
    
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        LOG.info("Starting DAM asset workflow process");
        
        try {
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resolver == null) {
                throw new WorkflowException("Unable to obtain ResourceResolver");
            }
            
            // Get payload (asset path)
            String assetPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Processing asset: {}", assetPath);
            
            // Get asset resource
            Resource assetResource = resolver.getResource(assetPath);
            if (assetResource == null) {
                LOG.warn("Asset resource not found: {}", assetPath);
                return;
            }
            
            // Adapt to Asset
            Asset asset = assetResource.adaptTo(Asset.class);
            if (asset == null) {
                LOG.warn("Resource is not a DAM asset: {}", assetPath);
                return;
            }
            
            // Read process arguments
            String processArgs = metaDataMap.get("PROCESS_ARGS", String.class);
            LOG.debug("Process arguments: {}", processArgs);
            
            // Process the asset
            processAsset(asset, assetResource, resolver, processArgs);
            
            // Update workflow metadata
            MetaDataMap workflowMetadata = workItem.getWorkflow().getMetaDataMap();
            workflowMetadata.put("assetProcessed", assetPath);
            workflowMetadata.put("processedAt", new Date());
            
            LOG.info("Asset processing completed: {}", assetPath);
            
        } catch (Exception e) {
            LOG.error("Error processing asset in workflow", e);
            throw new WorkflowException("Asset processing failed", e);
        }
    }
    
    /**
     * Process the DAM asset. Common operations include:
     * - Reading/updating asset metadata
     * - Processing renditions
     * - Extracting metadata
     * - Generating custom renditions
     * 
     * @param asset The DAM asset
     * @param assetResource The asset resource
     * @param resolver The resource resolver
     * @param processArgs Process arguments
     */
    private void processAsset(Asset asset, Resource assetResource, ResourceResolver resolver, String processArgs) {
        LOG.info("Processing asset: {}", asset.getPath());
        
        // Example 1: Access asset metadata
        String mimeType = asset.getMimeType();
        String assetName = asset.getName();
        LOG.debug("Asset mime type: {}, name: {}", mimeType, assetName);
        
        // Example 2: Get and process original rendition
        Rendition original = asset.getOriginal();
        if (original != null) {
            LOG.debug("Original rendition size: {} bytes", original.getSize());
            // Process original rendition if needed
        }
        
        // Example 3: Iterate through all renditions
        for (Rendition rendition : asset.getRenditions()) {
            String renditionName = rendition.getName();
            long renditionSize = rendition.getSize();
            LOG.debug("Rendition: {}, size: {} bytes", renditionName, renditionSize);
            
            // TODO: Process individual renditions if needed
        }
        
        // Example 4: Update asset metadata
        Resource metadataResource = assetResource.getChild("jcr:content/metadata");
        if (metadataResource != null) {
            ModifiableValueMap metadata = metadataResource.adaptTo(ModifiableValueMap.class);
            if (metadata != null) {
                // Add custom metadata
                metadata.put("customProcessed", true);
                metadata.put("customProcessedDate", new Date());
                metadata.put("customProcessor", this.getClass().getSimpleName());
                
                try {
                    resolver.commit();
                    LOG.info("Asset metadata updated successfully");
                } catch (Exception e) {
                    LOG.error("Failed to commit metadata changes", e);
                }
            }
        }
        
        // Example 5: Check asset properties
        if (asset.getMetadata("dc:title") != null) {
            LOG.debug("Asset title: {}", asset.getMetadata("dc:title"));
        }
        
        // TODO: Add your custom asset processing logic here
        performCustomAssetProcessing(asset, processArgs);
    }
    
    /**
     * Implement your custom asset processing logic.
     * 
     * @param asset The DAM asset to process
     * @param processArgs Arguments from the workflow step
     */
    private void performCustomAssetProcessing(Asset asset, String processArgs) {
        // TODO: Implement custom logic
        // Examples:
        // - Extract and index custom metadata
        // - Generate custom renditions
        // - Integrate with external systems
        // - Perform image analysis
        // - Tag assets automatically
        
        LOG.info("Custom asset processing for: {}", asset.getPath());
    }
}
