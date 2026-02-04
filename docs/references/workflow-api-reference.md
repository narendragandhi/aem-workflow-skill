# AEM Workflow API Reference

Comprehensive reference for Granite Workflow APIs in AEM as a Cloud Service.

## Package Structure

### Main Packages
- `com.adobe.granite.workflow` - Core workflow API
- `com.adobe.granite.workflow.exec` - Workflow execution interfaces
- `com.adobe.granite.workflow.model` - Workflow model definitions
- `com.adobe.granite.workflow.metadata` - Metadata management

## Core Interfaces and Classes

### WorkflowSession

The main entry point for workflow operations. Provides access to both design-time and runtime workflow objects.

**Package:** `com.adobe.granite.workflow.WorkflowSession`

**Key Methods:**

```java
// Workflow Model Operations
WorkflowModel getModel(String modelId) throws WorkflowException
WorkflowModel createNewModel(String title) throws WorkflowException
WorkflowModel createNewModel(String title, String id) throws WorkflowException
void deployModel(WorkflowModel model) throws WorkflowException
void deleteModel(String id) throws WorkflowException

// Workflow Instance Operations
Workflow startWorkflow(WorkflowModel model, WorkflowData data) throws WorkflowException
Workflow startWorkflow(WorkflowModel model, WorkflowData data, Map<String, Object> metaData) throws WorkflowException
Workflow getWorkflow(String id) throws WorkflowException
Workflow[] getWorkflows(String[] states) throws WorkflowException
void terminateWorkflow(Workflow workflow) throws WorkflowException
void suspendWorkflow(Workflow workflow) throws WorkflowException
void resumeWorkflow(Workflow workflow) throws WorkflowException
void restartWorkflow(Workflow workflow) throws WorkflowException

// WorkItem Operations
ResultSet<WorkItem> getActiveWorkItems() throws WorkflowException
ResultSet<WorkItem> getActiveWorkItems(long start, long limit) throws WorkflowException
ResultSet<WorkItem> getActiveWorkItems(long start, long limit, WorkItemFilter filter) throws WorkflowException
WorkItem getWorkItem(String id) throws WorkflowException
void complete(WorkItem workItem, Route route) throws WorkflowException

// WorkflowData Operations
WorkflowData newWorkflowData(String payloadType, Object payload)
void updateWorkflowData(Workflow workflow, WorkflowData data) throws WorkflowException

// Inbox Operations
ResultSet<InboxItem> getActiveInboxItems(long start, long limit, InboxItemFilter filter) throws WorkflowException

// Adaptable
<AdapterType> AdapterType adaptTo(Class<AdapterType> type)
// Can adapt to: ResourceResolver, Session
```

**Common Workflow States:**
- `RUNNING` - Workflow is active
- `COMPLETED` - Workflow finished successfully  
- `ABORTED` - Workflow was terminated
- `SUSPENDED` - Workflow is paused
- `STALE` - Workflow is no longer valid

### WorkflowProcess

Interface to implement custom workflow process steps.

**Package:** `com.adobe.granite.workflow.exec.WorkflowProcess`

**Required Method:**

```java
void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
    throws WorkflowException
```

**Parameters:**
- `workItem` - Represents the resource moving through the workflow
- `workflowSession` - Session for workflow operations
- `metaDataMap` - Process step arguments and configuration

### WorkItem

Represents a step in workflow execution.

**Package:** `com.adobe.granite.workflow.exec.WorkItem`

**Key Methods:**

```java
// Get the workflow instance
Workflow getWorkflow()

// Get the workflow payload data
WorkflowData getWorkflowData()

// Get work item metadata
MetaDataMap getMetaDataMap()

// Get work item ID
String getId()

// Get work item state
String getState()

// Get the node representing this work item
WorkflowNode getNode()

// Get current step ID
String getCurrentAssignee()

// Get time info
Date getTimeStarted()
Date getTimeEnded()
```

### Workflow

Represents a workflow instance.

**Package:** `com.adobe.granite.workflow.exec.Workflow`

**Key Methods:**

```java
// Get workflow ID
String getId()

// Get workflow state (RUNNING, COMPLETED, ABORTED, SUSPENDED, STALE)
String getState()

// Get workflow metadata
MetaDataMap getMetaDataMap()

// Get workflow data (payload)
WorkflowData getWorkflowData()

// Get all work items
List<WorkItem> getWorkItems()

// Get active work items
List<WorkItem> getActiveWorkItems()

// Get workflow model
WorkflowModel getWorkflowModel()

// Get initiator
String getInitiator()

// Get time info
Date getTimeStarted()
Date getTimeEnded()
```

### WorkflowData

Represents the payload and type being processed by a workflow.

**Package:** `com.adobe.granite.workflow.exec.WorkflowData`

**Key Methods:**

```java
// Get payload (JCR path, UUID, or other identifier)
Object getPayload()

// Get payload type ("JCR_PATH", "JCR_UUID", etc.)
String getPayloadType()

// Get metadata
MetaDataMap getMetaDataMap()
```

**Common Payload Types:**
- `JCR_PATH` - JCR repository path
- `JCR_UUID` - JCR node UUID

### MetaDataMap

Map for storing and retrieving workflow metadata.

**Package:** `com.adobe.granite.workflow.metadata.MetaDataMap`

**Key Methods:**

```java
// Get value with type
<T> T get(String key, Class<T> type)

// Get value with default
<T> T get(String key, T defaultValue)

// Put value
void put(String key, Object value)

// Remove value
Object remove(String key)

// Check if key exists
boolean containsKey(String key)

// Get all keys
Set<String> keySet()

// Common keys
String PROCESS_ARGS = "PROCESS_ARGS"  // Arguments passed to process step
```

### WorkflowModel

Represents a workflow model definition.

**Package:** `com.adobe.granite.workflow.model.WorkflowModel`

**Key Methods:**

```java
// Get model ID (path)
String getId()

// Get model title
String getTitle()

// Get model version
String getVersion()

// Get model description
String getDescription()

// Get all nodes in the model
List<WorkflowNode> getNodes()

// Get start node
WorkflowNode getStartNode()

// Get end node
WorkflowNode getEndNode()

// Check if model is active
boolean isActive()
```

### WorkflowNode

Represents a node (step) in a workflow model.

**Package:** `com.adobe.granite.workflow.model.WorkflowNode`

**Key Methods:**

```java
// Get node ID
String getId()

// Get node title
String getTitle()

// Get node type
String getType()

// Get node description
String getDescription()

// Get metadata
MetaDataMap getMetaDataMap()

// Get transitions (routes) from this node
List<WorkflowTransition> getTransitions()
```

**Common Node Types:**
- `PROCESS` - Process step
- `PARTICIPANT` - Participant step (requires user action)
- `AND_SPLIT` - Parallel split
- `OR_SPLIT` - Exclusive choice split
- `AND_JOIN` - Parallel join
- `OR_JOIN` - Exclusive choice join
- `START` - Start node
- `END` - End node

### Route

Represents a route (transition) between workflow steps.

**Package:** `com.adobe.granite.workflow.exec.Route`

**Key Methods:**

```java
// Get route ID
String getId()

// Get route name/label
String getName()

// Check if this is the default route
boolean hasDefault()

// Get metadata
MetaDataMap getMetaDataMap()
```

## Common Exceptions

### WorkflowException

**Package:** `com.adobe.granite.workflow.WorkflowException`

Main exception thrown by workflow operations. Extends `Exception`.

```java
// Constructors
WorkflowException(String message)
WorkflowException(String message, Throwable cause)
WorkflowException(Throwable cause)
```

**When to throw:**
- Repository access failures
- Invalid workflow configuration
- Processing errors that should cause retry
- Missing required resources

## Participant Step Chooser

### ParticipantStepChooser

Interface for dynamically selecting workflow participants.

**Package:** `com.adobe.granite.workflow.exec.ParticipantStepChooser`

**Required Method:**

```java
String getParticipant(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
    throws WorkflowException
```

**Returns:**
- User ID (e.g., "admin")
- Group ID (e.g., "content-authors")
- Principal ID

**OSGi Registration:**

```java
@Component(
    service = ParticipantStepChooser.class,
    property = {
        "chooser.label=My Custom Chooser"
    }
)
public class MyChooser implements ParticipantStepChooser {
    // Implementation
}
```

## Service User Mapping

For programmatic workflow operations, configure service user mapping:

**In `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-workflow.config`:**

```
user.mapping=[
  "your-bundle-symbolic-name:workflow-service\=workflow-service-user"
]
```

**Create service user in repo:**
```
create service user workflow-service-user with path /home/users/system/workflow
set ACL for workflow-service-user
  allow jcr:read,jcr:write on /content
  allow jcr:read,jcr:write on /var/workflow
end
```

## Performance Considerations

### Workflow Purging

Configure workflow purge to remove old instances:

**OSGi Config:** `com.adobe.granite.workflow.purge.Scheduler`

```java
scheduledpurge.name="Daily Purge"
scheduledpurge.workflowStatus="COMPLETED"
scheduledpurge.modelIds=["/var/workflow/models/dam/update_asset"]
scheduledpurge.daysold=7
```

### Transient Workflows

For high-volume processing, use transient workflows (not persisted to JCR):

```java
// Mark workflow model as transient
workflowModel.getMetaDataMap().put("transient", true);
```

**Limitations:**
- No workflow history
- Cannot pause/resume
- No workflow inbox items

## Migration from CQ Workflow API

### Package Mapping

| Deprecated (CQ)                    | Modern (Granite)                     |
|------------------------------------|--------------------------------------|
| com.day.cq.workflow.WorkflowSession| com.adobe.granite.workflow.WorkflowSession|
| com.day.cq.workflow.WorkflowException| com.adobe.granite.workflow.WorkflowException|
| com.day.cq.workflow.exec.WorkItem  | com.adobe.granite.workflow.exec.WorkItem|
| com.day.cq.workflow.exec.WorkflowProcess| com.adobe.granite.workflow.exec.WorkflowProcess|
| com.day.cq.workflow.exec.Workflow  | com.adobe.granite.workflow.exec.Workflow|
| com.day.cq.workflow.exec.WorkflowData| com.adobe.granite.workflow.exec.WorkflowData|
| com.day.cq.workflow.metadata.MetaDataMap| com.adobe.granite.workflow.metadata.MetaDataMap|
| com.day.cq.workflow.model.WorkflowModel| com.adobe.granite.workflow.model.WorkflowModel|

### Code Migration Example

**Before (CQ API - Deprecated):**
```java
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.exec.WorkItem;
```

**After (Granite API - Correct):**
```java
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.exec.WorkItem;
```

## External Resources

- **Latest JavaDoc**: https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/
- **Granite Workflow Package**: https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/com/adobe/granite/workflow/package-summary.html
- **Workflow Exec Package**: https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/com/adobe/granite/workflow/exec/package-summary.html
- **Workflow Model Package**: https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/com/adobe/granite/workflow/model/package-summary.html
