# LLM proposal with human review

```
[AI: Propose Page Description] → [Participant: Review] → [OR Split] ─┬─ Approve: [Approval Decision Recorder DECISION:approve] → [Apply Approved Description]
                                                                     └─ Reject:  [Approval Decision Recorder DECISION:reject]
```

| Type | Role |
| --- | --- |
| `ContentModelClient` | Your OSGi service wrapping the model provider (implement it) |
| `DescriptionRules` | The output contract, applied to model output and reviewer edits |
| `InvalidProposalException` | A reply or edit that breaks the contract |
| `AiProposeDescriptionProcess` | Calls the model, validates, stores the proposal; never writes content |
| `ApplyApprovedDescriptionProcess` | Applies the value only after a recorded approval, validating it again |

The recorder is `../approval/ApprovalDecisionRecorder.java`.

The proposal step records `aiProposal`, `aiPromptVersion`, `aiModel` and `aiProposalStatus`
(`ready` or `invalid`) in `workItem.getWorkflowData().getMetaDataMap()`.

Optional reviewer correction: make the review a Dialog Participant Step with a field named
`./jcr:content/aiEditedDescription`. The Inbox writes that field to the page; the apply step
uses it instead of the proposal and removes it (override the name with `EDIT_PROPERTY:name`).
