# App Builder and UI Extensibility for Workflows

## Content Fragment Workflows with UI Extensibility

AEM as a Cloud Service introduces **UI Extensibility** for integrating workflows with the Content Fragment Editor. This modern approach uses App Builder extensions instead of traditional AEM overlays.

## Overview

Content Fragment workflows allow authors to:

- Trigger approval workflows from the CF Editor
- Submit content for review without leaving the editor
- Track workflow status inline

## App Builder Extension Setup

**1. Create Extension Project:**

```bash
aio app init my-cf-workflow-extension --template @adobe/aem-cf-editor-ui-ext-tpl
```

**2. Register Action Bar Button:**

```javascript
// src/aem-cf-editor-1/web-src/src/components/ExtensionRegistration.js
import { register } from '@adobe/uix-guest';

function ExtensionRegistration() {
  const init = async () => {
    const guestConnection = await register({
      id: 'cf-workflow-extension',
      methods: {
        actionBar: {
          getButtons() {
            return [
              {
                id: 'start-approval-workflow',
                label: 'Submit for Approval',
                icon: 'Workflow',
                onClick: async () => {
                  const contentFragment =
                    await guestConnection.host.contentFragment.getContentFragment();
                  // Trigger workflow via AEM API
                  await startWorkflow(contentFragment.path);
                }
              }
            ];
          }
        }
      }
    });
  };

  init().catch(console.error);
  return <></>;
}
```

**3. Backend Action to Start Workflow:**

```javascript
// src/aem-cf-editor-1/actions/start-workflow/index.js
const { Core } = require('@adobe/aio-sdk');
const { getAEMAccessToken } = require('../utils');

async function main(params) {
  const { contentFragmentPath, workflowModel } = params;

  const accessToken = await getAEMAccessToken(params);
  const aemHost = params.aemHost;

  // Start workflow via AEM REST API
  const response = await fetch(`${aemHost}/etc/workflow/instances`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      model: workflowModel || '/var/workflow/models/request_for_activation',
      payloadType: 'JCR_PATH',
      payload: contentFragmentPath
    })
  });

  return {
    statusCode: response.ok ? 200 : response.status,
    body: await response.json()
  };
}

exports.main = main;
```

## Extension Configuration

**app.config.yaml:**

```yaml
extensions:
  aem/cf-editor/1:
    $include: src/aem-cf-editor-1/ext.config.yaml
    operations:
      view:
        - type: actionBar
          impl: ExtensionRegistration
```

## Deployment

```bash
## Deploy to Stage
aio app deploy

## Deploy to Production
aio app deploy --target production
```

## App Builder Extensions for Workflow UI

Beyond Content Fragments, App Builder enables custom workflow UI across AEM:

## Custom Inbox Extensions

Create custom views and actions in the AEM Inbox:

```javascript
// Custom inbox action
const inboxExtension = {
  id: 'custom-bulk-approve',
  label: 'Bulk Approve',
  icon: 'CheckmarkCircle',
  onClick: async (selectedItems) => {
    for (const item of selectedItems) {
      await completeWorkItem(item.workItemId, 'approve');
    }
  }
};
```

## External Workflow Dashboard

Build standalone dashboards that integrate with AEM workflows:

```javascript
// Fetch workflow instances
async function getWorkflowInstances(status = 'RUNNING') {
  const response = await fetch(
    `${aemHost}/libs/cq/workflow/content/console/content/instances.json?status=${status}`,
    { headers: { Authorization: `Bearer ${accessToken}` } }
  );
  return response.json();
}

// Display in React component
function WorkflowDashboard() {
  const [instances, setInstances] = useState([]);

  useEffect(() => {
    getWorkflowInstances().then(setInstances);
  }, []);

  return (
    <Table>
      {instances.map((wf) => (
        <Row key={wf.id}>
          <Cell>{wf.payload}</Cell>
          <Cell>{wf.state}</Cell>
          <Cell>{wf.currentAssignee}</Cell>
        </Row>
      ))}
    </Table>
  );
}
```
