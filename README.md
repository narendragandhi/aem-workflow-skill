# AEM Workflow Development - AI Assistant Skill

> AI coding assistant skill for developing custom workflows in Adobe Experience Manager (AEM) as a Cloud Service.
> **Supports: Claude Code | GitHub Copilot | Gemini | Cursor | Windsurf**

[![npm version](https://img.shields.io/npm/v/aem-workflow-skill.svg)](https://www.npmjs.com/package/aem-workflow-skill)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![AEM Cloud Service](https://img.shields.io/badge/AEM-Cloud%20Service-blue.svg)](https://experienceleague.adobe.com/docs/experience-manager-cloud-service.html)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

## Overview

This AI skill provides expert guidance for developing custom workflows in Adobe Experience Manager as a Cloud Service. It works with multiple AI coding assistants and is designed for developers using the latest AEM SDK (2025.x) following cloud-native best practices.

## Installation

### 1. For AI CLI Agents (Recommended)

The easiest way to install the skill for CLI-based assistants (Claude Code, Gemini CLI, etc.) is using `npx`.

```bash
# Install for Claude Code (default)
npx aem-workflow-skill

# Install for Google Gemini CLI (Global)
npx aem-workflow-skill -p gemini --global

# Install for ALL supported platforms at once
npx aem-workflow-skill -p all
```

**Supported CLI Platforms:**

- **Claude Code**: `.claude/skills/aem-workflow.md`
- **GitHub Copilot**: `.github/copilot-instructions.md`
- **Google Gemini**: `GEMINI.md`
- **Cursor AI**: `.cursor/rules/aem-workflow.mdc`
- **Windsurf**: `.windsurf/rules/aem-workflow.md`

### 2. For Claude Desktop (GUI)

If you are using the Claude Desktop application, you can install the skill using the pre-packaged `.skill` file.

1. **Download**: Go to [Releases](https://github.com/narendragandhi/aem-workflow-skill/releases) and download `aem-workflow-development.skill`.
2. **Install**:
   - Open Claude Desktop → Settings → Skills.
   - Click "Add Skill" and select the downloaded file.
3. **Verify**: The skill should appear as "Active" in your skills list.

### 3. As a Claude Code Plugin

```bash
/plugin marketplace add narendragandhi/aem-workflow-skill
```

---

## Example Queries

Try these in your AI assistant after installation:

- _"How do I create a custom workflow process step in AEM Cloud Service?"_
- _"Show me how to implement a post-processing workflow for assets"_
- _"What's the correct way to programmatically start a workflow?"_
- _"Show me the correct Granite Workflow API imports"_

## What's Included

### 📚 Documentation

- **Expert Guide**: Comprehensive workflow development principles.
- **API Reference**: Detailed documentation for `WorkflowSession`, `WorkItem`, etc.
- **Migration Guide**: Moving from deprecated CQ APIs to modern Granite APIs.

### 💻 Code Templates

- **Basic Process Step**: Standard implementation pattern.
- **Asset Processor**: Pattern for DAM post-processing.
- **Workflow Starter**: Programmatic execution template.

## Key Features

### 🎯 Cloud Service Architecture

Reflects modern AEM as a Cloud Service patterns:

- **Asset Microservices**: Focus on post-processing workflows.
- **Java 21**: Latest LTS configuration and best practices.
- **Container-Native**: Designing for ephemeral, stateless execution.

---

## License

MIT © Narendra Gandhi
