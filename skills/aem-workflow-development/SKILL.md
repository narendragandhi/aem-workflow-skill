---
name: aem-workflow-development
description: Comprehensive guide for developing custom workflows in Adobe Experience Manager (AEM) as a Cloud Service. Use this skill when creating, implementing, or debugging workflow processes, workflow models, custom process steps, participant and approval steps, retry-safe (idempotent) steps, long-running Sling Job steps, or LLM/AI steps with human review in AEM Cloud Service. Based on the AEM SDK API (2026.x) and Granite Workflow APIs.
---

# AEM Workflow Development

Guide for developing custom workflows in AEM as a Cloud Service using the latest Granite Workflow APIs and best practices.

## Core Principles

### Use Granite Workflow APIs (Not Legacy CQ APIs)

**CRITICAL**: Always use `com.adobe.granite.workflow` packages, NOT the deprecated `com.day.cq.workflow` packages.

**Correct Imports:**

```java
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
```

**Incorrect (Deprecated) Imports:**

```java
// DO NOT USE - These are deprecated
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
```

### AEM Cloud Service Considerations

- AEM as a Cloud Service uses containerized architecture with transient instances
- Content is distributed using Sling Content Distribution (not traditional replication)
- Workflow instances run in ephemeral containers
- Always design workflows to be stateless and resilient
- Avoid long-running workflow processes (use external job processing if needed)

**Asset Processing in Cloud Service:**

- Traditional DAM Update Asset workflow has been replaced by **Asset Microservices**
- Asset processing (renditions, metadata extraction) is now handled by cloud-native microservices
- Custom asset processing should use **post-processing workflows** configured per folder
- Post-processing workflows run AFTER Asset Microservices complete their processing
- A post-processing workflow MUST end with the **"DAM Update Asset Workflow Completed"**
  process step, or the asset stays in "processing"
- Processing profiles, a post-processing step, asset handling code and multi-resource
  (workflow package) payloads: [references/asset-and-multi-resource-workflows.md](references/asset-and-multi-resource-workflows.md)

**AEM Forms Workflows:**

- Forms-centric workflows can ONLY run on Author instances
- Adaptive Forms on Publish can submit data to workflows on Author
- This pattern enables approval workflows for form submissions
- Configure form submission action to trigger workflow on Author instance

## Maven Dependencies

### Current SDK Version

Example AEM SDK API version: `2026.10.29251.20261029T165345Z-261000` (the newest this skill was
compiled against). Use the version your environment runs: compiling against a newer API can
import package versions an older author does not export, and the bundle will not resolve.

### POM Configuration

```xml
<dependency>
    <groupId>com.adobe.aem</groupId>
    <artifactId>aem-sdk-api</artifactId>
    <version>2026.10.29251.20261029T165345Z-261000</version>
    <scope>provided</scope>
</dependency>
```

### Java Version Requirements

- **AEM Cloud Service (2025.x+)**: Java 21 (LTS)
- **Minimum Maven Version**: 3.8.6
- **Required Plugins**:
  - `aemanalyser-maven-plugin`: 1.6.6+
  - `maven-bundle-plugin`: 5.1.5+

### Java 21 Configuration

Create `.cloudmanager/java-version` file in project root:

```
21
```

Update `pom.xml`:

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

## Creating Custom Workflow Processes

### Step 1: Implement WorkflowProcess Interface

```java
package com.example.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Custom Asset Processing"
    }
)
public class CustomWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(CustomWorkflowProcess.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        try {
            // Get ResourceResolver from WorkflowSession
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);

            // Get the workflow payload
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Processing workflow for payload: {}", payloadPath);

            // Get the resource
            Resource resource = resolver.getResource(payloadPath);
            if (resource == null) {
                LOG.warn("Resource not found at path: {}", payloadPath);
                return;
            }

            // Read process arguments from MetaDataMap
            String customArg = metaDataMap.get("PROCESS_ARGS", String.class);
            LOG.info("Process arguments: {}", customArg);

            // Access workflow metadata
            MetaDataMap workflowMetadata = workItem.getWorkflowData().getMetaDataMap();

            // Store data for next workflow step
            workflowMetadata.put("processedBy", "CustomWorkflowProcess");
            workflowMetadata.put("processedAt", System.currentTimeMillis());

            // Perform custom processing logic here
            performCustomProcessing(resource, resolver);

            LOG.info("Workflow processing completed successfully");

        } catch (Exception e) {
            LOG.error("Error in workflow process", e);
            throw new WorkflowException("Failed to process workflow", e);
        }
    }

    private void performCustomProcessing(Resource resource, ResourceResolver resolver) {
        // Custom business logic implementation
    }
}
```

### Step 2: OSGi Component Configuration

**Key Annotations:**

- `@Component(service = WorkflowProcess.class)` - Registers as OSGi service
- `property = {"process.label=..."}` - Sets display name in AEM Workflow console

**Additional Properties (Optional):**

```java
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Custom Process",
        "service.description=Performs custom asset processing",
        "service.vendor=Your Company"
    }
)
```

## Working with WorkflowSession

### Obtaining WorkflowSession

**From ResourceResolver:**

```java
WorkflowSession workflowSession = resourceResolver.adaptTo(WorkflowSession.class);
```

**From JCR Session:**

```java
WorkflowSession workflowSession = jcrSession.adaptTo(WorkflowSession.class);
```

### Key WorkflowSession Methods

```java
// Get workflow model
// NOTE: In AEM Cloud Service, use activation/deactivation workflows for pages
// Asset processing workflows have been replaced by Asset Microservices
WorkflowModel model = workflowSession.getModel("/var/workflow/models/request_for_activation");

// Create workflow data (payload)
WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", "/content/my-site/page");

// Start workflow programmatically
Map<String, Object> metadata = new HashMap<>();
metadata.put("customKey", "customValue");
Workflow workflow = workflowSession.startWorkflow(model, workflowData, metadata);

// Get active work items (no-arg form returns an array; paged forms return ResultSet)
WorkItem[] activeItems = workflowSession.getActiveWorkItems();
ResultSet<WorkItem> firstPage = workflowSession.getActiveWorkItems(0, 50);

// Complete work item
workflowSession.complete(workItem, route);

// Terminate workflow
workflowSession.terminateWorkflow(workflow);

// Get workflow by ID
Workflow wf = workflowSession.getWorkflow(workflowId);
```

## Working with WorkItem and Payload

### Accessing Payload

```java
// Get payload path
String payloadPath = workItem.getWorkflowData().getPayload().toString();

// Get payload type
String payloadType = workItem.getWorkflowData().getPayloadType();
// Common types: "JCR_PATH", "JCR_UUID"

// Get resource from payload
ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
Resource resource = resolver.getResource(payloadPath);
```

### Working with Workflow Metadata

**CRITICAL: use `workItem.getWorkflowData().getMetaDataMap()`.** That map is saved by the
engine after each step and is what later steps read. Values written to
`workItem.getWorkflow().getMetaDataMap()` from a process step are **not** persisted: the
step appears to work, then the data is gone (verified on AEM SDK 2026.7).

```java
// Access the persisted workflow metadata
MetaDataMap workflowMetadata = workItem.getWorkflowData().getMetaDataMap();

// Read metadata
String value = workflowMetadata.get("key", String.class);
Integer intValue = workflowMetadata.get("count", 0);

// Write metadata (available to subsequent steps)
workflowMetadata.put("status", "processed");
workflowMetadata.put("timestamp", new Date());

// Access process step arguments
String processArgs = metaDataMap.get("PROCESS_ARGS", String.class);
```

### Persisting Data Between Steps

Inside a process step, writing to the WorkflowData metadata map is enough: the engine saves
it when the step completes.

```java
// Step A: store data for later steps
workItem.getWorkflowData().getMetaDataMap().put("reviewTarget", "marketing");

// Step B (later in the same instance): read it back
String target = workItem.getWorkflowData().getMetaDataMap().get("reviewTarget", String.class);
```

Values must be simple (String, Long, Boolean, Date, arrays of those). Keep them small: they
are stored in the instance and shown in the Inbox and instance history.

## Programmatically Starting Workflows

```java
@Reference
private ResourceResolverFactory resolverFactory;

public void startWorkflow() throws WorkflowException {
    Map<String, Object> authInfo = new HashMap<>();
    authInfo.put(ResourceResolverFactory.SUBSERVICE, "workflow-service");

    try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
        WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);

        // Get workflow model (path changed in AEM 6.4+)
        // AEM 6.4+: /var/workflow/models/...
        // Pre-6.4: /etc/workflow/models/.../jcr:content/model

        // NOTE: DAM Update Asset workflow replaced by Asset Microservices in Cloud Service
        // Use post-processing workflows for custom asset operations
        String modelPath = "/var/workflow/models/request_for_activation";
        WorkflowModel model = workflowSession.getModel(modelPath);

        // Create workflow payload
        String pagePath = "/content/my-site/page";
        WorkflowData wfData = workflowSession.newWorkflowData("JCR_PATH", pagePath);

        // Optional: Add workflow metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("initiatedBy", "automated-process");
        metadata.put("priority", "high");

        // Start the workflow
        Workflow workflow = workflowSession.startWorkflow(model, wfData, metadata);

        LOG.info("Started workflow: {} for payload: {}",
                 workflow.getId(), pagePath);
    }
}
```

## Error Handling and Best Practices

### Exception Handling

```java
@Override
public void execute(WorkItem workItem, WorkflowSession workflowSession,
                   MetaDataMap metaDataMap) throws WorkflowException {
    try {
        // Workflow logic
        performWorkflowLogic(workItem, workflowSession);

    } catch (RepositoryException e) {
        LOG.error("JCR Repository error in workflow", e);
        throw new WorkflowException("Repository access failed", e);

    } catch (IllegalArgumentException e) {
        LOG.error("Invalid workflow arguments", e);
        throw new WorkflowException("Invalid process configuration", e);

    } catch (Exception e) {
        LOG.error("Unexpected error in workflow process", e);
        // Throwing WorkflowException will cause workflow to retry
        throw new WorkflowException("Workflow processing failed", e);
    }
}
```

### Retries, Idempotency and Failure Routing

Throwing `WorkflowException` makes the engine **run the same step again**, and an admin can
retry it again from the Failures console. So every process step can execute more than once
for the same work item. Design for it:

- **Make steps idempotent.** Check before acting (is the property already set? is the history
  entry already recorded?), use set-to-value writes instead of increments or appends, and
  guard one-time records with a marker such as the work item ID:

  ```java
  MetaDataMap wf = workItem.getWorkflowData().getMetaDataMap();
  if (workItem.getId().equals(wf.get("lastRecordedWorkItem", String.class))) {
      return; // this step already did its work; a retry must not repeat it
  }
  // ... do the work ...
  wf.put("lastRecordedWorkItem", workItem.getId());
  ```

- **Give external side effects an idempotency key.** Emails, API calls and job submissions
  cannot be rolled back. Derive a key from the workflow ID plus the model node ID (stable
  across retries of the same step), pass it to APIs that accept one, and look up existing
  jobs by it before queueing a new one. Add a loop counter if the step can run more than once
  legitimately (revise-and-resubmit loops).
- **Only throw for transient errors.** Timeouts, 5xx responses and lock contention may succeed
  on retry. Invalid input, missing content and rejected business rules will not: write a
  short reason to workflow metadata (`failureReason`) and let an OR Split route the instance
  to a person or a rejection branch. Throwing a permanent error just burns retries and ends in
  the Failures console with nobody assigned.
- **Keep error context short.** Store a one-line reason, not a stack trace; it is shown to the
  next person in the Inbox and stays in the instance history.

### Resource Management

```java
// Always close ResourceResolvers when manually created
ResourceResolver resolver = null;
try {
    resolver = resolverFactory.getServiceResourceResolver(authInfo);
    // Use resolver
} finally {
    if (resolver != null && resolver.isLive()) {
        resolver.close();
    }
}

// When obtaining from WorkflowSession, no need to close
// (it's managed by the workflow engine)
ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
```

### Workflow Model Locations

**AEM 6.4+ / Cloud Service:**

```
/var/workflow/models/request_for_activation
/var/workflow/models/request_for_deactivation
/var/workflow/models/[custom-workflow-models]
```

**Pre-AEM 6.4 (legacy):**

```
/etc/workflow/models/dam/update_asset/jcr:content/model
```

`/var/workflow/models/dam/update_asset` may exist in the SDK but is not used for asset
processing in Cloud Service production (see [AEM Cloud Service Considerations](#aem-cloud-service-considerations)).

### Common Workflow Models in AEM Cloud Service

- `/var/workflow/models/request_for_activation` - Request for Activation (page publishing)
- `/var/workflow/models/request_for_deactivation` - Request for Deactivation
- Post-processing workflows for custom asset operations (configured per folder)
- Custom workflow models for non-asset content processing

## Security Best Practices

- Validate payload paths against an allowlist of roots; reject `..`.
- Sanitize anything that becomes a group name, model path or query.
- Use a dedicated service user per workflow subsystem (`subservice`), never admin.
- Never store secrets in workflow metadata; it is visible in the Inbox and history.
- Audit-log approvals, rejections and escalations with user ID and payload.
- Validate `PROCESS_ARGS` keys against an allowlist.

Code for each rule: [references/workflow-security.md](references/workflow-security.md).

## Testing Workflows

### Unit Testing with Mocks

```java
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomWorkflowProcessTest {

    @Test
    void testExecute() throws WorkflowException {
        // Arrange
        WorkItem workItem = mock(WorkItem.class);
        WorkflowSession workflowSession = mock(WorkflowSession.class);
        MetaDataMap metaDataMap = mock(MetaDataMap.class);
        WorkflowData workflowData = mock(WorkflowData.class);
        Workflow workflow = mock(Workflow.class);

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflowData.getPayload()).thenReturn("/content/dam/test.jpg");

        // Act
        CustomWorkflowProcess process = new CustomWorkflowProcess();
        process.execute(workItem, workflowSession, metaDataMap);

        // Assert - verify expected behavior
    }
}
```

## Workflow Participation Patterns

### Participant Steps

Workflows often require user interaction. Implement participant steps for approval, review, or decision-making:

**Participant Step Types:**

- **Participant Step**: Assigns work item to specific user/group
- **Dynamic Participant Step**: Determines assignee programmatically via script or service
- **Dialog Participant Step**: Presents dialog for user input before proceeding

**Example: Implementing Dynamic Participant Chooser**

```java
@Component(
    service = ParticipantStepChooser.class,
    property = {
        "chooser.label=Custom Participant Chooser"
    }
)
public class CustomParticipantChooser implements ParticipantStepChooser {

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession,
                                 MetaDataMap metaDataMap) throws WorkflowException {

        // Determine assignee based on workflow context
        String payloadPath = workItem.getWorkflowData().getPayload().toString();

        // Example: Assign based on content path
        if (payloadPath.startsWith("/content/we-retail")) {
            return "content-approvers"; // Group ID
        } else if (payloadPath.startsWith("/content/wknd")) {
            return "admin"; // User ID
        }

        return "administrators"; // Default group
    }
}
```

### ECMA Scripts in Workflows

ECMA (JavaScript) scripts can be used in various workflow steps to add dynamic logic. These scripts are stored in the JCR and have access to several workflow-related Java objects.

**Available Variables:**

- `graniteWorkItem`: The current work item.
- `workflowData`: The workflow data.
- `workflowSession`: The workflow session.
- `args`: An array of arguments passed to the step.
- `log`: A logger object.

**Use Cases:**

- **Process Step:** Perform operations on the payload or metadata.
- **Participant Step:** Dynamically select the participant.
- **OR Split:** Define routing logic.

**Example: Process Step Script**

```javascript
// Access the workflow payload and set a property
var workflowData = graniteWorkItem.getWorkflowData();
if (workflowData.getPayloadType() == 'JCR_PATH') {
  var path = workflowData.getPayload().toString();
  var node = workflowSession.getSession().getItem(path);
  node.setProperty('approved', true);
  node.save();
}
```

### Decision Steps (OR Split)

An OR Split runs exactly one of its branches. There are two ways a branch gets chosen:

**1. The reviewer chooses (approve/reject).** Put the OR Split directly after a participant
step. When the reviewer clicks **Complete** in the Inbox, each branch appears as a choice in
the **Next Step** list (`workflowSession.getRoutes(workItem, true)` returns one route per
branch), and the engine follows the branch they pick. This is how a person's decision enters
a workflow; start each branch with a step that records it (see
[Multi-Step Approval](#multi-step-approval-workflow-pattern)).

```
[Participant: Review] → [OR Split] ─┬─ Approve: [Record decision DECISION:approve] → …
                                    └─ Reject:  [Record decision DECISION:reject]  → …
```

**2. A rule chooses (automatic routing).** Give each branch a condition, usually an ECMA
script, that reads data an earlier step stored:

```javascript
function check() {
  var priority = workflowData.getMetaDataMap().get('priority', 'String');
  return priority == 'high';
}
```

Mark one branch as the default so an instance never has nowhere to go.

### Custom Dialogs for Participant Steps

A **Dialog Participant Step** shows extra fields when the reviewer completes the item. Know
where those fields go before relying on them (behavior of the AEM Inbox, checked against
SDK 2026.7):

- Fields whose `name` starts with `./` are POSTed by the Inbox **to the payload** before the
  item is completed. `./jcr:content/reviewNotes` becomes a property on the page's
  `jcr:content`.
- Other fields are sent along with the completion request and **not stored** anywhere.
- The Complete dialog's own **Comment** is stored in the workflow history; read it with
  `workflowSession.getHistory(workflow)` → `HistoryItem.getComment()`.
- They are **not** copied into workflow metadata.

So use routes (above) for the decision itself, the built-in comment for the reason, and dialog
fields only for extra data that belongs with the payload, removing it again once used if it
must not be published.

**Creating a dialog:**

1.  Create the dialog under `/apps`, e.g. `/apps/my-project/workflow/dialogs/review/cq:dialog`,
    with a `content` child (the Inbox renders that child):

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
              xmlns:jcr="http://www.jcp.org/jcr/1.0"
              xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
              jcr:primaryType="nt:unstructured"
              jcr:title="Review"
              sling:resourceType="cq/gui/components/authoring/dialog">
        <content
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/container">
            <items jcr:primaryType="nt:unstructured">
                <editedDescription
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                    fieldLabel="Corrected description (optional)"
                    name="./jcr:content/aiEditedDescription"/>
            </items>
        </content>
    </jcr:root>
    ```

2.  In the model, use a **Dialog Participant Step** and set **Dialog Path** to the `cq:dialog`
    node.

**Reading the dialog data in a later step:**

```java
String path = workItem.getWorkflowData().getPayload().toString();
ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
Resource content = resolver.getResource(path + "/jcr:content");
ModifiableValueMap props = content.adaptTo(ModifiableValueMap.class);

String edited = props.get("aiEditedDescription", "");
// ... use it ...
props.remove("aiEditedDescription"); // review data should not be published with the page
resolver.commit();
```


### Workflow Inbox Integration

Users interact with workflows through the AEM Inbox:

**User Actions Available:**

- **Complete**: Approve and advance to next step
- **Delegate**: Reassign work item to another user
- **Step Back**: Return to previous workflow step
- **View Details**: See workflow metadata and history

**Accessing Workflow Items Programmatically:**

```java
// Get user's active work items
WorkflowSession wfSession = resourceResolver.adaptTo(WorkflowSession.class);
WorkItem[] workItems = wfSession.getActiveWorkItems();

// Filter work items
for (WorkItem item : workItems) {
    String payload = item.getWorkflowData().getPayload().toString();
    String assignee = item.getCurrentAssignee();

    LOG.info("Work item: {} assigned to: {}", payload, assignee);
}
```

### Workflow Launcher Configuration

Automatically trigger workflows based on repository events:

**Launcher Properties:**

- **Event Type**: Created, Modified, Removed
- **Node Type**: dam:Asset, cq:Page, etc.
- **Path**: Repository path where launcher applies (supports wildcards)
- **Exclude List**: Paths to exclude from triggering
- **Run Modes**: Author, Publish, or both
- **Workflow Model**: Which workflow to start

**Example Configuration (via JCR):**

```xml
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:WorkflowLauncher"
    description="Auto-process new assets in specific folder"
    eventType="1"
    glob="/content/dam/auto-process/**"
    nodetype="dam:Asset"
    runModes="author"
    workflow="/var/workflow/models/custom-asset-workflow"/>
```

**Event Types:**

- `1` = Created
- `2` = Modified
- `4` = Removed

### Workflow Stages

Organize workflows into logical stages for better tracking:

```java
// Configure stages in workflow model metadata
// Stages appear in timeline and reporting

// In custom process, update current stage
MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();
wfMetadata.put("currentStage", "Review");
wfMetadata.put("stageStartTime", System.currentTimeMillis());
```

**Best Practices for Stages:**

- Keep stage names consistent across workflows
- Limit to 5-7 stages for clarity
- Update stage at beginning of each major phase
- Use stages for reporting and dashboards

## Advanced Topics

### ACS AEM Commons Tools

For bulk runs (Bulk Workflow Manager) and engine-free execution of single process steps
(Synthetic Workflow), see [references/acs-commons-workflows.md](references/acs-commons-workflows.md).
This skill does not depend on ACS AEM Commons.

### Workflow Variables (External Storage)

For sensitive data or large payloads, configure external storage:

```java
// At workflow model level, flag can be set:
// Property: userMetadataPersistenceEnabled = true
// This stores variables externally, not in JCR

// Access in code remains the same
MetaDataMap metadata = workItem.getWorkflowData().getMetaDataMap();
```

### Workflow REST API

```bash
# Get workflow models
curl -u admin:admin http://localhost:4502/libs/cq/workflow/content/console/content/models.json

# Start workflow via REST (example: activation workflow)
curl -u admin:admin -X POST \
  http://localhost:4502/etc/workflow/instances \
  -d model=/var/workflow/models/request_for_activation \
  -d payloadType=JCR_PATH \
  -d payload=/content/my-site/page

# Get workflow instance details
curl -u admin:admin \
  http://localhost:4502/etc/workflow/instances/2025-02-01/activation_12345.json

# Complete work item
curl -u admin:admin -X POST \
  http://localhost:4502/etc/workflow/instances/{workflowId}/workItems/{workItemId} \
  -d route=Approve
```

## Common Patterns

### Email Notification in Workflow

```java
@Reference
private MessageGatewayService messageGatewayService;

@Reference
private MessageGateway<Email> emailService;

private void sendEmail(String recipient, String subject, String body) {
    Email email = new HtmlEmail();
    email.setSubject(subject);
    email.setMsg(body);

    try {
        emailService.send(email);
    } catch (Exception e) {
        LOG.error("Failed to send email", e);
    }
}
```

## Troubleshooting

### Workflow Administration

**Workflow Console Locations:**

- **Models**: `/libs/cq/workflow/admin/console/content/models.html`
- **Instances**: `/libs/cq/workflow/admin/console/content/instances.html`
- **Launchers**: `/libs/cq/workflow/admin/console/content/launchers.html`
- **Archive**: `/libs/cq/workflow/admin/console/content/archive.html`
- **Failures**: `/libs/cq/workflow/admin/console/content/failures.html`

**Workflow Instance States:**

- `RUNNING` - Workflow is actively executing
- `COMPLETED` - Workflow finished successfully
- `ABORTED` - Workflow was terminated
- `SUSPENDED` - Workflow is paused (can be resumed)
- `STALE` - Workflow progression failed (background job issue)

**Managing Workflow Instances:**

```java
// Suspend a running workflow
workflowSession.suspendWorkflow(workflow);

// Resume a suspended workflow
workflowSession.resumeWorkflow(workflow);

// Terminate a workflow
workflowSession.terminateWorkflow(workflow);

// Restart a workflow (returns void)
workflowSession.restartWorkflow(workflow);
```

**Workflow Purging Configuration:**

Configure automatic cleanup of old workflow instances via OSGi:

```
PID: com.adobe.granite.workflow.purge.Scheduler

scheduledpurge.name = "Daily Completed Workflow Purge"
scheduledpurge.workflowStatus = "COMPLETED"
scheduledpurge.modelIds = ["/var/workflow/models/custom-workflow"]
scheduledpurge.daysold = 30
scheduledpurge.webjobs = false
```

**Properties:**

- `scheduledpurge.name` - Name for this purge configuration
- `scheduledpurge.workflowStatus` - Which status to purge (COMPLETED, ABORTED)
- `scheduledpurge.modelIds` - Specific models, or empty for all
- `scheduledpurge.daysold` - Days since completion before purging
- `scheduledpurge.webjobs` - Include web.jobs workflows

### Workflow Failure Handling

When workflows fail, use the Failures console:

**Available Actions:**

- **Failure Details** - View error message, step, and stack trace
- **Open History** - See complete workflow execution history
- **Retry Step** - Re-execute failed step after fixing cause
- **Terminate** - End workflow if error is unrecoverable
- **Terminate and Retry** - End and start new instance with same payload

**Logging Workflow Issues:**

```java
@Override
public void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap args)
        throws WorkflowException {

    try {
        // Workflow logic

    } catch (Exception e) {
        // Log detailed context for troubleshooting
        LOG.error("Workflow failed for payload: {}, model: {}, step: {}",
            workItem.getWorkflowData().getPayload(),
            workItem.getWorkflow().getWorkflowModel().getId(),
            workItem.getNode().getTitle(),
            e);

        // Add failure context to workflow metadata
        workItem.getWorkflowData().getMetaDataMap().put("failureReason", e.getMessage());
        workItem.getWorkflowData().getMetaDataMap().put("failureTimestamp", new Date());

        throw new WorkflowException("Processing failed", e);
    }
}
```

### Common Issues

**Issue: "Process implementation not found"**

- Verify OSGi bundle is active
- Check `@Component` annotation has correct `service = WorkflowProcess.class`
- Ensure `process.label` property is set

**Issue: Cannot adapt WorkflowSession**

- Ensure using `com.adobe.granite.workflow.WorkflowSession`
- Not `com.day.cq.workflow.WorkflowSession` (deprecated)

**Issue: Workflow hangs or doesn't progress**

- Check for exceptions in logs
- Verify workflow has proper routes configured
- Ensure participant steps have assigned users/groups

**Issue: Java version mismatch**

- Ensure project uses Java 21 for AEM Cloud Service 2025.x
- Update `.cloudmanager/java-version` to `21`
- Update Maven compiler settings

**Issue: Workflow payload is null or incorrect**

- Verify payload type matches expected format (JCR_PATH vs JCR_UUID)
- Check if resource still exists at payload path
- Ensure launcher configuration matches node type

## Workflow UI with App Builder

To start workflows from the Content Fragment Editor, add Inbox actions, or build an external
workflow dashboard, use App Builder UI extensions rather than overlays. Setup, extension
registration and backend actions: [references/app-builder-workflow-ui.md](references/app-builder-workflow-ui.md).

## Long-Running Work: Make the Workflow Wait

Heavy work (transcoding, large files, slow external APIs) belongs in a **Sling Job**: jobs
survive restarts, retry with backoff, and spread across the cluster. The trap is the step that
queues the job:

- A `WorkflowProcess` that calls `jobManager.addJob(...)` returns immediately. The workflow
  advances **before the job runs**, so later steps act on unfinished work.
- Implement **`WorkflowExternalProcess`** instead. The engine calls `execute()` once, polls
  `hasFinished(handle)` on a schedule, then calls `handleResult(handle)` before advancing.

Abridged (helper names are placeholders; the full template follows):

```java
@Component(service = WorkflowExternalProcess.class,
           property = {"process.label=Run Long-Running Job and Wait"})
public class LongRunningJobStep implements WorkflowExternalProcess {

    @Reference private JobManager jobManager;

    public Serializable execute(WorkItem item, WorkflowSession session, MetaDataMap args)
            throws WorkflowException {
        String key = LongRunningJobs.keyFor(item);           // workflow id + node id, hashed
        if (resultExists(session, key)) return key;          // retry after the job finished
        if (jobManager.getJob(TOPIC, Map.of("idempotencyKey", key)) == null) {
            jobManager.addJob(TOPIC, Map.of("idempotencyKey", key, "payload", payloadOf(item)));
        }                                                    // else: retry while still running
        return key;
    }

    public boolean hasFinished(Serializable key, WorkItem item, WorkflowSession s, MetaDataMap a) {
        return resultExists(s, key.toString());              // the consumer writes the result
    }

    public void handleResult(Serializable key, WorkItem item, WorkflowSession s, MetaDataMap a) {
        // Copy status (and a short reason) into workflow metadata as "jobOutcome".
        // A permanent failure is routed by an OR Split, not thrown: throwing only retries.
    }
}
```

Full template, including an idempotent `JobConsumer` that records each result once and stops
retrying errors that cannot succeed: [scripts/long-running-job/](scripts/long-running-job/README.md).

In the model, an external process step sets `EXTERNAL_PROCESS` to the implementation's class
name plus `period`, `timeout` and optionally `advanceOnTimeout` (the same keys Adobe's own
models use). The engine polls through Sling scheduled jobs, so a broken job scheduler leaves
the instance waiting with `cq:waitingToAdvance`.

**Polling limits** (OSGi `com.adobe.granite.workflow.core.job.ExternalProcessJobHandler`, as
shipped in the SDK): poll period 300 s, timeout 3600 s by default, maximum timeout 10800 s; period
and timeout can be set per step. On timeout the step fails into the Failures console, or, with
advance-on-timeout, the workflow moves on **without** `handleResult`, so route on
`jobOutcome == "succeeded"` and treat a missing value as failure. Work that can exceed three
hours should not hold a workflow step open; finish the workflow and let the job start a
follow-up workflow.

**Job queue configuration:**

```
PID: org.apache.sling.event.jobs.QueueConfiguration~longrunning

queue.name = "Long Running Workflow Jobs"
queue.topics = ["com/example/workflow/longrunning"]
queue.type = "ORDERED"
queue.maxparallel = 3
queue.retries = 5
queue.retrydelay = 60000
queue.priority = "NORM"
```

## Multi-Step Approval Workflow Pattern

Each approval level is a participant step followed by an OR Split; the reviewer picks the
branch in the Inbox, and each branch starts by recording the decision:

```
[Participant: Dept review] → [OR Split] ─┬─ Approve: [Recorder DECISION:approve] → next level / [Publish]
                                         └─ Reject:  [Recorder DECISION:reject]  → [Notify author]
```

Rules the template follows (exercised end to end on a local AEM author through the Inbox
completion endpoint):

- **The route is the decision.** The recorder's `DECISION:approve|reject` argument is fixed
  per branch, but which branch runs is the reviewer's choice at run time. Do not try to carry
  the decision in a dialog field: the Inbox does not put dialog fields into workflow metadata
  (see [Custom Dialogs](#custom-dialogs-for-participant-steps)).
- **Fail closed.** A recorder with a missing or misspelled `DECISION` records a rejection with
  a reason. A default must never publish.
- **The reviewer and comment come from history.** `getCurrentAssignee()` on the recorder's
  own work item is the process step, not the person. Read the last completed participant
  step from `workflowSession.getHistory(workflow)`: `getUserId()` and `getComment()` (the
  Complete dialog's comment).
- **Persist through `getWorkflowData().getMetaDataMap()`** (`lastDecision`, `lastApprover`,
  `approvalHistory`, `rejectionReason`), so later steps and the notifier can read them.
- **Record once.** Guard on the work item ID so a retried step does not duplicate history.

Components (participant chooser by level and department, decision recorder, escalation check,
completion notifier): [scripts/approval/](scripts/approval/README.md).


## AI and LLM Steps in Workflows

AEM Workflow already supplies what an LLM step needs to be safe: durable state (workflow
metadata), human review (participant steps and the Inbox), explicit routing (OR Splits),
pause/resume, and retries. Use them; do not let a model call write content directly.

```
[AI: Propose] → [Participant: Review] → [OR Split] ─┬─ Approve: [Recorder DECISION:approve] → [Apply]
                                                    └─ Reject:  [Recorder DECISION:reject]
```

1. **The model only proposes.** Parse its reply into a typed value (e.g. JSON with one field),
   validate it, and store it in workflow metadata. No JCR writes in this step.
2. **Own the prompt.** Keep it in your code with a version string (`page-description/v1`), and
   store the prompt version and model ID with every proposal.
3. **Own the context.** Send only the fields the task needs (title, current value), not the
   page tree or the full workflow history.
4. **Bound the retries.** On invalid output, retry a few times with a one-line description of
   what was wrong. When attempts run out, mark the proposal invalid and let the reviewer write
   the value; do not fail the workflow.
5. **Separate transient from permanent errors.** Throw `WorkflowException` for timeouts and
   5xx (the engine retries the step); record and route everything a retry cannot fix.
6. **Make the model step idempotent.** If a valid proposal is already in metadata, return. A
   retried step must not pay for, or silently change, a proposal a reviewer may be reading.
7. **A person approves; deterministic code applies.** The reviewer picks Approve or Reject in
   the Inbox. The apply step runs only on the Approve branch, still refuses to act unless
   `lastDecision` is `approve`, re-validates the value, and is a no-op if it is already set.
   An optional reviewer correction comes from a Dialog Participant field that the Inbox
   writes to the payload (`./jcr:content/aiEditedDescription`); the apply step uses it and
   then removes it so it is never published.
8. **Wrap the model provider in one OSGi service** (`ContentModelClient`), so prompts, providers
   and test fakes swap without touching workflow steps.

Full template (propose step, output contract, apply step): [scripts/ai-proposal/](scripts/ai-proposal/README.md).
This flow, including a reviewer edit and a rejection, runs end to end in the repository's
on-AEM verification.

## Reference Documentation

Load these when the task needs them:

| File | Use it for |
| --- | --- |
| [references/workflow-api-reference.md](references/workflow-api-reference.md) | Full Granite Workflow API listing |
| [references/workflow-security.md](references/workflow-security.md) | Code for each security rule |
| [references/app-builder-workflow-ui.md](references/app-builder-workflow-ui.md) | CF Editor, Inbox and dashboard extensions |
| [references/acs-commons-workflows.md](references/acs-commons-workflows.md) | Bulk Workflow Manager, Synthetic Workflow |
| [references/asset-and-multi-resource-workflows.md](references/asset-and-multi-resource-workflows.md) | Asset post-processing, workflow packages |
| [scripts/BasicWorkflowProcessTemplate.java](scripts/BasicWorkflowProcessTemplate.java) | Starting point for a process step |
| [scripts/DamAssetWorkflowProcessTemplate.java](scripts/DamAssetWorkflowProcessTemplate.java) | Asset post-processing step |
| [scripts/WorkflowStarterTemplate.java](scripts/WorkflowStarterTemplate.java) | Starting workflows from code |
| [scripts/approval/](scripts/approval/README.md) | Multi-level approval with escalation |
| [scripts/long-running-job/](scripts/long-running-job/README.md) | Sling Job that the workflow waits for |
| [scripts/ai-proposal/](scripts/ai-proposal/README.md) | LLM proposal with human review |

## Additional Resources

- AEM SDK JavaDoc: https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/
- AEM Cloud Service Documentation: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service
- Granite Workflow Package: `com.adobe.granite.workflow`
- Workflow Execution Package: `com.adobe.granite.workflow.exec`
- Workflow Model Package: `com.adobe.granite.workflow.model`
