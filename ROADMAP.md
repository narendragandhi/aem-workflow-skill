# AEM Workflow Skill - Enhancement Roadmap

This document outlines planned enhancements for future releases of the AEM Workflow Development skill.

## Version 1.1.0 (Released February 2026)

### Completed Features

#### 1. Multi-Step Approval Workflow ✅

**Status**: Completed
**Files**: 4 Java templates in `examples/`

Delivered end-to-end approval workflow:

- Sequential approval stages (Draft → Review → Legal → Final Approval)
- Rejection handling with return-to-author flow
- Escalation after timeout
- Approval history tracking
- Dynamic stage assignment based on content type

Components: HierarchicalApprovalChooser, ApprovalDecisionRecorder, EscalationCheckProcess, ApprovalCompletionNotifier

#### 2. Content Fragment Workflows ✅

**Status**: Completed
Documentation for CF workflows via UI Extensibility and App Builder

#### 3. Sling Jobs Integration ✅

**Status**: Completed
Pattern for long-running processes with Job Consumer

#### 4. Hierarchical Participant Chooser ✅

**Status**: Completed
**Files**: `examples/HierarchicalApprovalChooser.java`

Advanced participant selection patterns with manager chain lookup and escalation

---

## Version 1.2.0 (Released February 2026)

### Completed Features

#### 5. Multi-Platform Support ✅

**Status**: Completed

Skill can now be installed for multiple AI coding assistants:

- Claude Code (default)
- GitHub Copilot
- Google Gemini CLI
- Cursor AI
- Windsurf/Cascade

CLI options: `--platform`, `--list`, `--global`, `--uninstall`

---

## Version 1.3.0 (Planned)

### High Priority Enhancements

#### 6. Email Notification Template

**Status**: Planned
**Effort**: Low
**Files**: `docs/scripts/EmailNotificationTemplate.java`

Production-ready email notification process step:

- HTML email templating with Sling Models
- Dynamic recipient resolution (author, reviewers, groups)
- Configurable email templates per workflow stage
- Attachment support for workflow context
- Internationalization support

#### 7. AEM 6.x to Cloud Service Migration Guide

**Status**: Planned
**Effort**: Medium
**Files**: `docs/migration-guide.md`, SKILL.md section

Comprehensive migration documentation:

- API mapping (CQ → Granite)
- Deprecated patterns and replacements
- DAM Update Asset → Asset Microservices migration
- Workflow launcher changes
- Testing migration checklist
- Common migration pitfalls

| AEM 6.x Pattern            | Cloud Service Replacement             |
| -------------------------- | ------------------------------------- |
| `com.day.cq.workflow.*`    | `com.adobe.granite.workflow.*`        |
| DAM Update Asset workflow  | Asset Microservices + Post-processing |
| `/etc/workflow/models/`    | `/var/workflow/models/`               |
| Replication agents         | Sling Content Distribution            |
| Custom rendition workflows | Asset Compute workers                 |

#### 8. External API Integration Template

**Status**: Planned
**Effort**: Medium
**Files**: `docs/scripts/ExternalAPIIntegrationTemplate.java`

Template for integrating workflows with external systems:

- REST API calls with retry logic
- Webhook callback handling
- OAuth2 authentication patterns
- Async response handling
- Circuit breaker pattern for resilience
- Error handling and fallback strategies

#### 9. Performance Optimization Guide

**Status**: Planned
**Effort**: Medium

Topics to cover:

- Batch processing patterns for bulk content
- Async workflow execution
- Avoiding workflow bottlenecks
- Workflow instance cleanup strategies
- Memory management in long-running processes
- Query optimization for work item retrieval

---

## Version 1.4.0 (Future)

### Medium Priority Enhancements

#### 10. Cloud Manager Integration Guide

**Status**: Planned
**Effort**: Low

Documentation covering:

- Environment-specific workflow configurations
- OSGi config management for workflows
- Pipeline deployment considerations
- Workflow testing in RDE (Rapid Development Environment)
- Production vs non-production workflow behavior

#### 11. Advanced Error Recovery Patterns

**Status**: Planned
**Effort**: Medium
**Files**: `docs/scripts/ErrorRecoveryTemplate.java`

Patterns for robust error handling:

- Retry strategies with exponential backoff
- Dead-letter queue pattern
- Compensation/rollback workflows
- Partial failure handling
- Error notification and alerting
- Manual intervention hooks

#### 12. AEM Mocks Testing Guide

**Status**: Planned
**Effort**: Medium

Complete testing setup documentation:

- `io.wcm.testing.aem-mock` integration
- Mocking WorkflowSession, WorkItem, MetaDataMap
- Integration testing patterns
- Test fixtures for common scenarios
- CI/CD testing configuration

#### 13. Workflow Monitoring & Reporting

**Status**: Planned
**Effort**: Medium

Custom monitoring solutions:

- Workflow metrics collection
- Custom dashboard creation
- SLA tracking and alerting
- Performance bottleneck identification
- Historical analysis patterns

---

## Version 1.5.0+ (Future)

### Nice to Have Enhancements

#### 14. Architecture Diagrams

Visual documentation:

- Asset Microservices flow diagram
- Workflow lifecycle state diagram
- Cloud Service architecture overview
- Post-processing workflow sequence diagram

#### 15. Anti-Patterns Guide

Common mistakes to avoid:

- Long-running synchronous processes
- Improper resource resolver handling
- Missing error handling
- Hardcoded paths and values
- Ignoring Cloud Service constraints

#### 16. Workflow Versioning Guide

Model management:

- Workflow model versioning strategies
- Backward compatibility approaches
- In-flight instance handling during upgrades
- A/B testing workflows

#### 17. Internationalization in Workflows

i18n patterns:

- Localized email notifications
- Multi-language participant instructions
- Locale-aware date/time handling
- Translation workflow integration

---

## Completed Templates Summary

### v1.0.0 Templates (Initial Release)

```
docs/scripts/
├── BasicWorkflowProcessTemplate.java      # Foundation workflow process
├── DamAssetWorkflowProcessTemplate.java   # Asset processing
└── WorkflowStarterTemplate.java           # Programmatic workflow start
```

### v1.1.0 Templates (Approval Workflows)

```
examples/
├── HierarchicalApprovalChooser.java       # Dynamic participant selection
├── ApprovalDecisionRecorder.java          # Decision tracking
├── EscalationCheckProcess.java            # Timeout escalation
├── ApprovalCompletionNotifier.java        # Completion notifications
└── CustomMetadataExtractorWorkflow.java   # Metadata extraction
```

### v1.2.0 (Multi-Platform Support)

No new templates - focused on installer and platform support

---

## Contributing

We welcome contributions! Priority areas:

1. Real-world workflow examples from production systems
2. Additional troubleshooting scenarios
3. Performance optimization tips
4. Integration patterns with other Adobe products

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## Feedback

Have suggestions for the roadmap? Open an issue on GitHub:
https://github.com/narendragandhi/aem-workflow-skill/issues
