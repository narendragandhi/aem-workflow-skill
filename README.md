# AEM Workflow Development - AI Assistant Skill

> AI coding assistant skill for developing custom workflows in Adobe Experience Manager (AEM) as a Cloud Service.
> **Supports: Claude Code | GitHub Copilot | Gemini | Cursor | Windsurf**

[![npm version](https://img.shields.io/npm/v/aem-workflow-skill.svg)](https://www.npmjs.com/package/aem-workflow-skill)
[![CI](https://github.com/narendragandhi/aem-workflow-skill/actions/workflows/ci.yml/badge.svg)](https://github.com/narendragandhi/aem-workflow-skill/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![AEM Cloud Service](https://img.shields.io/badge/AEM-Cloud%20Service-blue.svg)](https://experienceleague.adobe.com/docs/experience-manager-cloud-service.html)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

## Overview

Guidance and Java templates for AEM as a Cloud Service workflows, built on the Granite
Workflow API: process steps, participant and approval steps, retry-safe steps, long-running
work, and LLM steps with human review. The skill keeps its core guidance short and loads API
detail, security code and App Builder material only when a task needs it.

## Installation

### Claude Code plugin

```
/plugin marketplace add narendragandhi/aem-workflow-skill
/plugin install aem-workflow-skill@aem-workflow-skill
```

### npx (Claude Code, Copilot, Gemini, Cursor, Windsurf)

```bash
# Claude Code, project level (default)
npx aem-workflow-skill

# Google Gemini CLI, user level
npx aem-workflow-skill -p gemini --global

# All supported tools
npx aem-workflow-skill -p all

# Remove
npx aem-workflow-skill -p all -u
```

| Tool | Installed to |
| --- | --- |
| Claude Code | `.claude/skills/aem-workflow-development/` (SKILL.md, references/, scripts/) |
| GitHub Copilot | `.github/copilot-instructions.md` |
| Google Gemini | `GEMINI.md` |
| Cursor | `.cursor/rules/aem-workflow.mdc` |
| Windsurf | `.windsurf/rules/aem-workflow.md` |

Single-file tools get the core guidance with the reference material appended; template links
point to this repository.

Upgrading from 1.3.0: the old Claude Code file `.claude/skills/aem-workflow.md` was not
loaded as a skill. Reinstalling replaces it with the folder above and removes the old file.

### Claude Desktop

Download `aem-workflow-development.skill` from
[Releases](https://github.com/narendragandhi/aem-workflow-skill/releases), then
Settings → Skills → Add Skill.

## Example queries

- _"Create a process step that tags assets after Asset Microservices finish."_
- _"Build a two-level approval where the reviewer can reject with a reason."_
- _"My step calls an external API; make it safe to retry."_
- _"Add an LLM step that drafts page descriptions for an editor to approve."_
- _"Why is the value I put in workflow metadata gone in the next step?"_

## What's included

**Guidance (`skills/aem-workflow-development/SKILL.md`)**

- Granite Workflow API usage (not the deprecated `com.day.cq.workflow` API) and Cloud Service constraints
- Persisting data between steps with `workItem.getWorkflowData().getMetaDataMap()`
- Approvals: the reviewer picks an OR Split branch in the Inbox; what Dialog Participant
  Step fields actually do
- Retries and idempotency, transient vs permanent errors
- Long-running work with `WorkflowExternalProcess` and Sling Jobs
- LLM steps: the model proposes, a person approves, deterministic code applies

**Templates (`skills/aem-workflow-development/scripts/`)**

| Template | Purpose |
| --- | --- |
| `BasicWorkflowProcessTemplate.java` | Starting point for a process step |
| `DamAssetWorkflowProcessTemplate.java` | Asset post-processing step |
| `WorkflowStarterTemplate.java` | Starting workflows from code |
| `approval/` | Multi-level approval: chooser, decision recorder, escalation, notifier |
| `long-running-job/` | External process step that waits for an idempotent Sling Job |
| `ai-proposal/` | LLM proposal, review and apply |

**References (`skills/aem-workflow-development/references/`)**: Workflow API listing,
security code, App Builder UI extensions, ACS AEM Commons tools, asset and multi-resource
workflows.

## Verification

| Check | How | Runs in CI |
| --- | --- | --- |
| Installer, docs layout, `.skill` package freshness | `npm test` | Yes |
| Every template and example compiles against the AEM SDK API (`-Werror`) | `npm run verify:java` | Yes |
| JUnit behaviour tests for the approval and AI templates | `npm run verify:java` | Yes |
| Templates running in real workflows on a local author | `verification/run-on-aem.sh` | No (needs AEM) |

The on-AEM run installs a temporary bundle, builds test models through the Workflow API,
completes review steps through the Inbox's own completion endpoint, checks the results, and
removes everything it created:

```bash
AEM_SDK_API=<version your author runs> verification/run-on-aem.sh
```

Last run (AEM SDK 2026.7.27083, local author): approval (approve and reject branches) and the
AI proposal flow (reviewer edit, rejection) passed 11 of 11 checks. The long-running job
scenario is opt-in (`RUN_JOB_SCENARIO=1`): it needs a working Sling job scheduler, and it has
not passed on a real instance yet because the test instance's repository was damaged.

## Development

```bash
npm ci
npm test               # installer, layout and package tests
npm run lint
npm run verify:java    # Java templates (needs Java 21 and Maven)
npm run package        # rebuild releases/aem-workflow-development.skill after changing the skill
```

## License

MIT © Narendra Gandhi
