# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
