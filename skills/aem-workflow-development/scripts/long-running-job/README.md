# Long-running job the workflow waits for

A plain `WorkflowProcess` that only queues a Sling Job returns immediately, so the workflow
advances before the job has run. `LongRunningJobStep` implements `WorkflowExternalProcess`:
`execute()` starts the work and returns a handle, the engine polls `hasFinished(handle)`, then
calls `handleResult(handle)` once before advancing.

| Class | Role |
| --- | --- |
| `LongRunningJobs` | Topic, result location and the idempotency key |
| `LongRunningJobStep` | External process step: queues the job once, waits for its result |
| `LongRunningJobConsumer` | Does the work, records the result once, stops retrying permanent errors |

Both halves are idempotent, keyed on (workflow instance, model node): a retried `execute()`
reuses the queued job or stored result, and a retried job sees its own stored result.

Setup:

- A service user mapped to subservice `workflow-job-results` that can write under
  `/var/example/workflow-job-results`; the workflow service user must be able to read it.
- Route on `jobOutcome == "succeeded"` after the step and treat a missing value as failure
  (on timeout with advance-on-timeout, `handleResult` is not called).
- Purge result nodes on the same schedule as completed workflow instances.
