# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2026-11-17

Templates and guidance were checked by running them in real workflows on an AEM author
(SDK 2026.7.27083). Several things the skill taught did not hold up there.

### Fixed

- **Workflow data was not persisted.** The skill and every template wrote to
  `workItem.getWorkflow().getMetaDataMap()`, which the engine does not save from a process
  step. All guidance and templates now use `workItem.getWorkflowData().getMetaDataMap()`.
- **Dialog Participant Step data.** The skill said dialog values land in workflow metadata.
  The Inbox posts `./`-prefixed fields to the payload and drops the rest; the comment goes to
  workflow history. The dialog section now says so and shows how to read and clean up
  payload fields.
- **Approval decisions.** Approvals are modelled as a participant step followed by an OR
  Split whose branches the reviewer picks as "Next Step" in the Inbox; the recorder starts
  each branch with `DECISION:approve` or `DECISION:reject`. The recorder now fails closed on a
  missing or misspelled decision (it used to default to approval), reads the reviewer and
  comment from history, persists its results, and records each work item once.
- **Claude Code install did not load as a skill.** `npx aem-workflow-skill` wrote
  `.claude/skills/aem-workflow.md`; Claude Code loads `.claude/skills/<name>/SKILL.md`. It now
  installs the `aem-workflow-development` folder with references and scripts, and removes the
  old file on upgrade.
- **`/plugin marketplace add` failed**: added `.claude-plugin/marketplace.json`.
- **Async job step did not wait.** Replaced with a `WorkflowExternalProcess` step and an
  idempotent `JobConsumer` (`scripts/long-running-job/`).
- `WorkflowSession.restartWorkflow` returns `void`; no-arg `getActiveWorkItems()` returns
  `WorkItem[]`.
- Multi-class template files could not compile when copied; each class now has its own file.

### Added

- **Retries, Idempotency and Failure Routing** section.
- **AI and LLM Steps in Workflows** section and `scripts/ai-proposal/`: the model proposes a
  validated value, the reviewer approves or rejects in the Inbox (optionally correcting it),
  deterministic code applies it.
- `verification/`: compiles every template and example against the AEM SDK API with
  `-Werror`, runs JUnit behaviour tests, and `run-on-aem.sh` runs the templates in real
  workflows on a local author and cleans up afterwards.
- CI on Node 20/22/24 (lint, format, tests) and a Java job; releases run the same gates and
  check that the tag matches `package.json`.
- `npm run package` builds the `.skill` file; a test fails if the committed package is stale.

### Changed

- **Skill folder renamed** to `skills/aem-workflow-development`, matching the skill's `name`
  and the `.skill` package; references and scripts moved inside it.
- **Smaller SKILL.md (1,855 → ~1,100 lines).** Security code, App Builder UI, ACS Commons and
  asset/multi-resource detail moved to `references/`.
- Single-file installs (Copilot, Gemini, Cursor, Windsurf) get the moved references appended.
- Node 18 or later is required for the installer.

### Known limitations

- The long-running job template has not completed on a real instance: the test author's
  repository was damaged and its Sling job scheduler did not fire external-process polling.
  The job consumer and its service-user setup did work there.

## [1.3.0] - 2026-02-24

### Added

- **Automated Testing**: Comprehensive Jest test suite (unit + integration tests).
- **Quality Assurance**: Integrated ESLint (v10) and Prettier for code consistency.
- **CI/CD**: Added GitHub Actions pipeline for automated testing on every push.
- **Integration Tests**: File system verification for all supported platforms.

### Changed

- **CLI Enhancement**: Refactored `bin/install.js` with ANSI color output and improved UI.
- **Unified Documentation**: Consolidated installation guides into a single source of truth in `README.md`.
- **Code Quality**: Refactored installer to be modular and testable.

### Removed

- Redundant `INSTALLATION.md` file.

## [1.2.0] - 2026-02-05

### Added

- **Multi-Platform Support**: Skill can now be installed for multiple AI coding assistants
  - Claude Code (default)
  - GitHub Copilot (`.github/copilot-instructions.md`)
  - Google Gemini CLI (`GEMINI.md`)
  - Cursor AI (`.cursor/rules/aem-workflow.mdc`)
  - Windsurf/Cascade (`.windsurf/rules/aem-workflow.md`)
- **Platform-specific transformations**: Content automatically adapted for each platform's format
- **Global installation support**: For Claude Code and Gemini CLI
- **New CLI options**: `--platform`, `--list` for platform management

### Changed

- Install script now supports `-p <platform>` flag
- Package description updated to reflect multi-platform support
- Quality score improved to 95/100

### Verified

- All 8 Java templates compile with Java 21 and AEM SDK 2025.11.x
- OSGi component descriptors correctly generated
- Content accuracy verified against Adobe documentation

---

## [1.1.0] - 2026-02-04

### Added

- **Content Fragment Workflows**: Complete guide for integrating workflows with CF Editor using UI Extensibility and App Builder
- **App Builder Extensions**: Documentation for custom workflow UI, inbox extensions, and external dashboards
- **Sling Jobs Integration**: Pattern for long-running processes with Job Consumer and async workflow dispatch
- **Multi-Step Approval Workflow**: Complete example with hierarchical participant chooser, escalation, and decision tracking
- **New Template**: `MultiStepApprovalWorkflow.java` with 4 reusable components (HierarchicalApprovalChooser, ApprovalDecisionRecorder, EscalationCheckProcess, ApprovalCompletionNotifier)

### Documentation

- Added 500+ lines of new content covering modern AEM Cloud Service patterns
- Expanded Advanced Topics section with production-ready examples
- Added OSGi configuration examples for Sling Jobs

---

## [1.0.1] - 2026-02-04

### Fixed

- **WorkflowStarterTemplate.java**: Fixed `@Component` service class reference (was `WorkflowStarter.class`, now correctly `WorkflowStarterTemplate.class`)
- **CustomMetadataExtractorWorkflow.java**: Fixed method name typo (`categorizeSizeSize()` → `categorizeFileSize()`)

### Added

- Documentation: Added note about "DAM Update Asset Workflow Completed" step requirement for post-processing workflows
- Quality report and roadmap documentation

---

## [1.0.0] - 2026-02-03

### Added

- Initial release of AEM Workflow Development skill
- Complete Granite Workflow API documentation
- Asset Microservices architecture guidance
- Java 21 configuration and best practices
- Three production-ready code templates:
  - BasicWorkflowProcessTemplate.java
  - DamAssetWorkflowProcessTemplate.java
  - WorkflowStarterTemplate.java
- Comprehensive API reference documentation
- Workflow participation patterns (Participant steps, dynamic assignment)
- Workflow launcher configuration guidance
- Workflow administration and monitoring patterns
- Multi-resource support documentation
- Testing patterns with mocks
- Troubleshooting guide for common issues

### Architecture

- Reflects AEM as a Cloud Service containerized architecture
- Documents Asset Microservices replacing DAM Update Asset workflow
- Covers Sling Content Distribution (not traditional replication)
- Includes Forms workflows Author-only patterns

### Documentation

- 550+ lines of main skill documentation
- Comprehensive API reference with all major interfaces
- Migration guide from deprecated CQ Workflow APIs
- Real-world code examples and patterns

### Validation

- Validated against AEM SDK 2025.11.23482.20251120T200914Z-251200
- Cross-referenced with official Adobe documentation
- Reviewed for Cloud Service architectural accuracy
- Tested for Java 21 compatibility

## [Unreleased]

### Planned

- Additional workflow patterns (multi-step approvals, escalations)
- More participant chooser examples
- Video tutorials and architectural diagrams
- Performance optimization patterns
- Integration examples with external systems
- Migration guides from AEM 6.x workflows
- Batch processing patterns
- More troubleshooting scenarios

---

## Version History

### Version Numbering

This project uses [Semantic Versioning](https://semver.org/):

- MAJOR version for incompatible API changes
- MINOR version for backwards-compatible functionality additions
- PATCH version for backwards-compatible bug fixes

### Release Notes Format

- **Added**: New features
- **Changed**: Changes to existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Security vulnerability fixes

---

For older versions, see [GitHub Releases](https://github.com/narendragandhi/aem-workflow-skill/releases)
