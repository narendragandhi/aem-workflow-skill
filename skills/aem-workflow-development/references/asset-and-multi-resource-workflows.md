# Asset and Multi-Resource Workflows

## Asset Processing in Cloud Service

**Asset Microservices vs Traditional Workflows:**

In AEM as a Cloud Service, asset processing has fundamentally changed:

**Traditional (AEM 6.x and earlier):**

- Assets processed via DAM Update Asset workflow
- Synchronous processing in AEM instance
- Limited scalability

**Cloud Service (Current):**

- Asset processing handled by cloud-native Asset Microservices
- Automatic rendition generation, metadata extraction
- Horizontally scalable and highly performant
- Runs outside AEM instances

**For Custom Asset Processing:**

Use **post-processing workflows** that run AFTER Asset Microservices complete:

1. **Configure Processing Profile with Post-Processing Workflow:**
   - Navigate to Tools → Assets → Processing Profiles
   - Create or edit a processing profile
   - Add your custom workflow in the "Post-processing Workflow" section
   - Apply the profile to specific DAM folders

   **IMPORTANT**: Post-processing workflows MUST end with the **"DAM Update Asset Workflow Completed"** process step. This step signals to Asset Microservices that custom processing is complete. Without this step, the asset may appear stuck in "processing" state.

2. **Create Custom Post-Processing Workflow Step:**

```java
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Custom Post-Processing Step"
    }
)
public class CustomAssetPostProcessor implements WorkflowProcess {

    @Override
    public void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        // Asset has already been processed by Asset Microservices
        // Renditions, metadata extraction are complete

        String assetPath = workItem.getWorkflowData().getPayload().toString();
        ResourceResolver resolver = wfSession.adaptTo(ResourceResolver.class);

        Resource assetResource = resolver.getResource(assetPath);
        Asset asset = assetResource.adaptTo(Asset.class);

        // Perform custom operations AFTER microservices processing
        // Examples: custom metadata, external system integration, tagging

        LOG.info("Post-processing asset: {}", assetPath);
    }
}
```

3. **Asset Reprocessing:**
   - Use "Reprocess Assets" feature in Assets UI
   - Sends assets through entire microservices pipeline again
   - Useful after updating processing profiles

**For Bulk Asset Operations:**

- Use Asset Compute SDK for custom processing workers
- Configure as part of processing profiles
- Runs in Adobe I/O Runtime

## Asset Processing Pattern

```java
private void processAsset(Resource assetResource) {
    Asset asset = assetResource.adaptTo(Asset.class);
    if (asset == null) {
        return;
    }

    // Get original rendition
    Rendition original = asset.getOriginal();

    // Process renditions
    for (Rendition rendition : asset.getRenditions()) {
        // Custom rendition processing
    }

    // Update metadata
    ModifiableValueMap metadata = assetResource.adaptTo(ModifiableValueMap.class);
    metadata.put("processedDate", new Date());
}
```

## Multi-Resource Support

Handle workflows that process multiple resources:

**Enable Multi-Resource Support:**

```java
// Workflow model metadata
workflowModel.getMetaDataMap().put("multiResourceSupport", true);
```

**Processing Multiple Resources:**

```java
@Override
public void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap args)
        throws WorkflowException {

    String payloadPath = workItem.getWorkflowData().getPayload().toString();
    ResourceResolver resolver = wfSession.adaptTo(ResourceResolver.class);

    // Check if this is a workflow package (multiple resources)
    Resource resource = resolver.getResource(payloadPath);

    if (resource != null && resource.isResourceType("cq/workflow/components/collection")) {
        // This is a workflow package - process all contained resources
        Resource collectionContent = resource.getChild("list");
        if (collectionContent != null) {
            for (Resource child : collectionContent.getChildren()) {
                String path = child.getValueMap().get("path", String.class);
                if (path != null) {
                    processResource(resolver.getResource(path));
                }
            }
        }
    } else {
        // Single resource
        processResource(resource);
    }
}

private void processResource(Resource resource) {
    // Process individual resource
    LOG.info("Processing resource: {}", resource.getPath());
}
```
