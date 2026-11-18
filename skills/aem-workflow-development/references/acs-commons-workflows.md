# ACS AEM Commons Workflow Tools

**Note:** The following sections describe features available in the popular open-source library ACS AEM Commons. These are powerful tools for advanced workflow scenarios, but they are not required to use this skill. This skill does not have a dependency on ACS AEM Commons.

## Bulk Workflow Manager (ACS Commons)

ACS AEM Commons provides a powerful tool for running workflows on a large number of resources in bulk. This is particularly useful for content migrations, bulk updates, or reprocessing large sets of assets.

**Key Features:**

- **Bulk Execution**: Run a workflow model against a list of payloads.
- **Query-based Payloads**: Use a JCR2 query to dynamically select the resources to process.
- **Throttling**: Control the rate at which workflows are created to avoid overwhelming the system.
- **Synthetic Workflow Integration**: Can be used with Synthetic Workflows for high-performance processing.

**When to Use It:**

- Activating a large number of pages.
- Applying a new metadata schema to thousands of assets.
- Running a custom workflow on all content within a specific path.

## Synthetic Workflow (ACS Commons)

Synthetic Workflow is a feature of ACS AEM Commons that allows for the execution of AEM Workflow Processes without the overhead of the full AEM Workflow Engine. It directly calls the `WorkflowProcess` implementation, bypassing the creation of Sling Jobs and workflow instances.

**Benefits:**

- **Performance**: Much faster than traditional workflows, especially for a large number of invocations.
- **Simplicity**: Avoids the complexity of creating and managing workflow models for simple, single-process tasks.
- **Reduced Load**: Less impact on the AEM instance's resources.

**Limitations:**

- **No External Steps**: Does not support steps that are not implemented as a `WorkflowProcess`.
- **No Participant Steps**: Cannot be used for workflows that require user interaction.
- **Single Process**: Only supports a single `WorkflowProcess` step.
- **No Transitions**: Does not support multiple transition paths.

For high-performance bulk processing without full workflow engine overhead (primarily for SDK/local development):

```java
// Using ACS Commons Synthetic Workflow
// NOTE: This is useful for local development and SDK
// In Cloud Service, asset processing uses Asset Microservices
@Reference
private SyntheticWorkflowRunner syntheticWorkflowRunner;

public void processBulkContent(List<String> contentPaths, String workflowModelPath) {
    SyntheticWorkflowModel model =
        syntheticWorkflowRunner.getSyntheticWorkflowModel(
            resourceResolver,
            workflowModelPath,
            true // Track progress
        );

    for (String contentPath : contentPaths) {
        syntheticWorkflowRunner.execute(
            resourceResolver,
            contentPath,
            model,
            false, // Auto-save changes
            false  // Throw exceptions
        );
    }
}
```
