package com.example.aem.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Example: Custom asset metadata extractor workflow process.
 * 
 * This workflow step extracts custom metadata from assets and stores
 * it in the asset's metadata node. Designed to run as a post-processing
 * workflow after Asset Microservices complete.
 * 
 * Usage:
 * 1. Create workflow model with Process Step
 * 2. Select this process in the step
 * 3. Configure as post-processing workflow in Processing Profile
 * 4. Apply profile to DAM folder
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Example: Custom Metadata Extractor",
        "service.description=Extracts custom metadata from assets"
    }
)
public class CustomMetadataExtractorWorkflow implements WorkflowProcess {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomMetadataExtractorWorkflow.class);
    
    // Custom metadata properties
    private static final String PROP_EXTRACTED_DATE = "customExtractedDate";
    private static final String PROP_PROCESSING_STATUS = "customProcessingStatus";
    private static final String PROP_FILE_SIZE_CATEGORY = "customFileSizeCategory";
    
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        LOG.info("Starting custom metadata extraction");
        
        try {
            // Get ResourceResolver from WorkflowSession
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resolver == null) {
                throw new WorkflowException("Unable to obtain ResourceResolver");
            }
            
            // Get asset payload
            String assetPath = workItem.getWorkflowData().getPayload().toString();
            LOG.debug("Processing asset: {}", assetPath);
            
            // Get asset resource
            Resource assetResource = resolver.getResource(assetPath);
            if (assetResource == null) {
                LOG.warn("Asset not found: {}", assetPath);
                return;
            }
            
            // Adapt to Asset
            Asset asset = assetResource.adaptTo(Asset.class);
            if (asset == null) {
                LOG.warn("Resource is not a DAM asset: {}", assetPath);
                return;
            }
            
            // Extract and store custom metadata
            extractCustomMetadata(asset, assetResource, resolver);
            
            // Update workflow metadata
            MetaDataMap workflowMetadata = workItem.getWorkflow().getMetaDataMap();
            workflowMetadata.put("lastProcessedAsset", assetPath);
            workflowMetadata.put("processingComplete", true);
            
            LOG.info("Custom metadata extraction completed for: {}", assetPath);
            
        } catch (Exception e) {
            LOG.error("Failed to extract custom metadata", e);
            throw new WorkflowException("Metadata extraction failed", e);
        }
    }
    
    /**
     * Extracts custom metadata from the asset and stores in metadata node.
     */
    private void extractCustomMetadata(Asset asset, Resource assetResource, ResourceResolver resolver) {
        
        // Get metadata resource
        Resource metadataResource = assetResource.getChild("jcr:content/metadata");
        if (metadataResource == null) {
            LOG.warn("Metadata resource not found for: {}", assetResource.getPath());
            return;
        }
        
        ModifiableValueMap metadata = metadataResource.adaptTo(ModifiableValueMap.class);
        if (metadata == null) {
            LOG.warn("Cannot adapt metadata resource to ModifiableValueMap");
            return;
        }
        
        try {
            // Example 1: Store extraction timestamp
            metadata.put(PROP_EXTRACTED_DATE, new Date());
            
            // Example 2: Categorize by file size
            long fileSize = asset.getOriginal().getSize();
            String sizeCategory = categorizeFileSize(fileSize);
            metadata.put(PROP_FILE_SIZE_CATEGORY, sizeCategory);
            
            // Example 3: Store processing status
            metadata.put(PROP_PROCESSING_STATUS, "completed");
            
            // Example 4: Extract MIME type info
            String mimeType = asset.getMimeType();
            if (mimeType != null) {
                String[] parts = mimeType.split("/");
                if (parts.length > 0) {
                    metadata.put("customMediaType", parts[0]); // image, video, application, etc.
                }
            }
            
            // Commit changes
            resolver.commit();
            LOG.debug("Custom metadata saved for: {}", asset.getPath());
            
        } catch (Exception e) {
            LOG.error("Failed to save custom metadata", e);
        }
    }
    
    /**
     * Categorizes asset by file size.
     */
    private String categorizeFileSize(long bytes) {
        if (bytes < 1024 * 1024) { // < 1MB
            return "small";
        } else if (bytes < 10 * 1024 * 1024) { // < 10MB
            return "medium";
        } else if (bytes < 100 * 1024 * 1024) { // < 100MB
            return "large";
        } else {
            return "xlarge";
        }
    }
}
