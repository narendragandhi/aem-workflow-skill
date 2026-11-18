# Multi-step approval

Hierarchical approval with escalation. Each approval level in the model is:

```
[Participant: Review] → [OR Split] ─┬─ Approve: [Approval Decision Recorder  DECISION:approve] → next level / [Publish]
                                    └─ Reject:  [Approval Decision Recorder  DECISION:reject]  → [Notify Author & Revise]
```

| Class | Role |
| --- | --- |
| `HierarchicalApprovalChooser` | Picks the approver group by level and department |
| `ApprovalDecisionRecorder` | First step on each branch: records the decision, reviewer and comment |
| `EscalationCheckProcess` | Flags approvals that exceed a time threshold |
| `ApprovalCompletionNotifier` | Summarizes the outcome in workflow metadata |

The reviewer chooses Approve or Reject as **Next Step** when completing the item in the
Inbox. The recorder's `DECISION` argument is fixed per branch, so which recorder runs is the
reviewer's choice. The reviewer and the Complete dialog's comment are read from workflow
history. A missing or misspelled `DECISION` fails closed (recorded as a rejection).

Everything is stored in `workItem.getWorkflowData().getMetaDataMap()` (`lastDecision`,
`lastApprover`, `approvalHistory`, `rejectionReason`, `workflowRoute`); that is the map the
engine persists between steps.
