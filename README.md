# AEM Workflow Development - Claude AI Skill

> AI coding assistant skill for developing custom workflows in Adobe Experience Manager (AEM) as a Cloud Service
> **Supports: Claude Code | GitHub Copilot | Gemini | Cursor | Windsurf**

[![npm version](https://img.shields.io/npm/v/aem-workflow-skill.svg)](https://www.npmjs.com/package/aem-workflow-skill)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![AEM Cloud Service](https://img.shields.io/badge/AEM-Cloud%20Service-blue.svg)](https://experienceleague.adobe.com/docs/experience-manager-cloud-service.html)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![SDK Version](https://img.shields.io/badge/AEM%20SDK-2025.11.x-green.svg)](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/developing/aem-as-a-cloud-service-sdk.html)

## Overview

This AI skill provides expert guidance for developing custom workflows in Adobe Experience Manager as a Cloud Service. It works with multiple AI coding assistants and is designed for developers using the latest AEM SDK (2025.x) following cloud-native best practices.

### What This Skill Covers

- ✅ **Granite Workflow APIs** - Modern workflow development patterns
- ✅ **Asset Microservices** - Post-processing workflow architecture
- ✅ **Java 21 Support** - Latest LTS version configuration
- ✅ **Workflow Participation** - User interaction and approval patterns
- ✅ **Programmatic Triggering** - Automated workflow execution
- ✅ **Administration** - Instance management and troubleshooting
- ✅ **Production Code** - Real, tested templates and examples

## Quick Start

### Installation

#### Option 1: npm/npx (Recommended)

```bash
# Install for Claude Code (default)
npx aem-workflow-skill

# Install for ALL supported platforms at once
npx aem-workflow-skill -p all

# Install for specific platform
npx aem-workflow-skill -p copilot    # GitHub Copilot
npx aem-workflow-skill -p gemini     # Google Gemini CLI
npx aem-workflow-skill -p cursor     # Cursor AI
npx aem-workflow-skill -p windsurf   # Windsurf/Cascade

# Install globally (where supported)
npx aem-workflow-skill -p gemini -g  # Global Gemini config
npx aem-workflow-skill --global      # Global Claude config

# List all supported platforms
npx aem-workflow-skill --list

# Uninstall
npx aem-workflow-skill -p all -u     # Remove from all platforms
```

#### Supported Platforms

| Platform | Install Location | Global |
|----------|-----------------|--------|
| Claude Code | `.claude/skills/aem-workflow.md` | Yes |
| GitHub Copilot | `.github/copilot-instructions.md` | No |
| Google Gemini | `GEMINI.md` | Yes |
| Cursor AI | `.cursor/rules/aem-workflow.mdc` | No |
| Windsurf | `.windsurf/rules/aem-workflow.md` | No |

#### Option 2: Claude Code Plugin

```bash
# In Claude Code, run:
/plugin marketplace add narendragandhi/aem-workflow-skill
```

#### Option 3: Manual Installation

1. **Download the Skill**
   ```bash
   # Clone this repository
   git clone https://github.com/narendragandhi/aem-workflow-skill.git
   cd aem-workflow-skill

   # Copy the skill file to your project (choose your platform)
   # Claude Code:
   mkdir -p .claude/skills && cp skills/aem-workflow/SKILL.md .claude/skills/aem-workflow.md

   # GitHub Copilot:
   mkdir -p .github && cp skills/aem-workflow/SKILL.md .github/copilot-instructions.md

   # Gemini:
   cp skills/aem-workflow/SKILL.md GEMINI.md
   ```

2. **Start Using**
   - Open your AI coding assistant in your project
   - Ask any AEM workflow development question
   - The skill automatically provides expert guidance

### Example Queries

```
"How do I create a custom workflow process step in AEM Cloud Service?"
"Show me how to implement a post-processing workflow for assets"
"What's the correct way to programmatically start a workflow?"
"How do I handle workflow participant assignment dynamically?"
"Why isn't my DAM Update Asset workflow working in Cloud Service?"
```

## What's Included

### 📚 Documentation

- **SKILL.md** - Complete workflow development guide
  - Core principles and best practices
  - Maven dependencies and Java 21 setup
  - Step-by-step workflow creation
  - Asset Microservices architecture
  - Participation and automation patterns
  - Administration and troubleshooting

- **API Reference** - Comprehensive interface documentation
  - WorkflowSession, WorkflowProcess, WorkItem
  - MetaDataMap, WorkflowModel, WorkflowNode
  - ParticipantStepChooser
  - Migration from deprecated CQ APIs

### 💻 Code Templates

Three production-ready Java templates:

1. **BasicWorkflowProcessTemplate.java**
   - Foundation for any workflow process
   - Proper error handling
   - Resource management patterns

2. **DamAssetWorkflowProcessTemplate.java**
   - Asset-specific processing
   - Rendition handling
   - Metadata manipulation

3. **WorkflowStarterTemplate.java**
   - Programmatic workflow triggering
   - Bulk processing patterns
   - Service implementation

## Key Features

### 🎯 Cloud Service Architecture

This skill reflects the **fundamental changes** in AEM as a Cloud Service:

#### Asset Microservices (Critical)
```diff
- ❌ OLD: DAM Update Asset workflow (/var/workflow/models/dam/update_asset)
+ ✅ NEW: Asset Microservices + Post-Processing Workflows
```

**What Changed:**
- Asset processing (renditions, metadata extraction) now handled by cloud-native microservices
- Custom processing requires post-processing workflows that run AFTER microservices
- Configured per-folder via Processing Profiles

#### Java 21 Support
```xml
<!-- .cloudmanager/java-version -->
21

<!-- pom.xml -->
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### 📖 Usage Examples

#### Creating a Custom Workflow Process

```java
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Custom Asset Processing"
    }
)
public class CustomWorkflowProcess implements WorkflowProcess {
    
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, 
                       MetaDataMap metaDataMap) throws WorkflowException {
        
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        
        // Your custom workflow logic here
    }
}
```

#### Programmatically Starting Workflows

```java
WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);
WorkflowModel model = workflowSession.getModel("/var/workflow/models/request_for_activation");
WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", "/content/my-site/page");

Map<String, Object> metadata = new HashMap<>();
metadata.put("initiatedBy", "automated-process");

Workflow workflow = workflowSession.startWorkflow(model, workflowData, metadata);
```

## Compatibility

| Component | Version | Notes |
|-----------|---------|-------|
| AEM | Cloud Service | Primary target |
| Java | 21 (LTS) | Recommended |
| Java | 17 | Supported |
| Java | 11 | Being phased out |
| SDK | 2025.11.x | Latest tested |
| Maven | 3.8.6+ | Minimum required |

## Architecture Considerations

### ⚠️ Important Changes in Cloud Service

1. **Workflows Run in Ephemeral Containers**
   - Design workflows to be stateless
   - Avoid long-running processes
   - Use external job processing for intensive tasks

2. **Forms Workflows Author-Only**
   - Forms-centric workflows ONLY run on Author
   - Publish can submit to Author workflows
   - Important for approval patterns

3. **Content Distribution (Not Replication)**
   - Content distributed via Sling Content Distribution
   - Not traditional replication
   - Affects workflow design patterns

## Development Workflow

### Setting Up Locally

```bash
# 1. Download latest AEM SDK
# https://experience.adobe.com/#/downloads

# 2. Configure Java 21
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# 3. Update pom.xml with correct dependencies
<dependency>
    <groupId>com.adobe.aem</groupId>
    <artifactId>aem-sdk-api</artifactId>
    <version>2025.11.23482.20251120T200914Z-251200</version>
    <scope>provided</scope>
</dependency>

# 4. Build and deploy
mvn clean install -PautoInstallPackage
```

### Testing Workflows

```bash
# Start local AEM instance
java -jar aem-sdk-quickstart-*.jar

# Access workflow console
http://localhost:4502/libs/cq/workflow/admin/console/content/models.html

# Monitor instances
http://localhost:4502/libs/cq/workflow/admin/console/content/instances.html
```

## Common Issues & Solutions

### "Process implementation not found"
- ✅ Verify OSGi bundle is active
- ✅ Check `@Component` annotation has `service = WorkflowProcess.class`
- ✅ Ensure `process.label` property is set

### "Cannot adapt WorkflowSession"
- ✅ Use `com.adobe.granite.workflow.WorkflowSession`
- ❌ NOT `com.day.cq.workflow.WorkflowSession` (deprecated)

### "DAM Update Asset workflow not working"
- ✅ Use post-processing workflows instead
- ✅ Configure via Processing Profiles
- ✅ Runs AFTER Asset Microservices

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

### Areas for Contribution
- Additional code examples
- Use case documentation
- Troubleshooting scenarios
- Performance optimization patterns

## Resources

### Official Documentation
- [AEM Cloud Service Documentation](https://experienceleague.adobe.com/docs/experience-manager-cloud-service.html)
- [Workflow Development Guide](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/sites/authoring/workflows/overview)
- [AEM SDK JavaDoc](https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/)
- [Granite Workflow API](https://javadoc.io/doc/com.adobe.aem/aem-sdk-api/latest/com/adobe/granite/workflow/package-summary.html)

### Community
- [Adobe Experience League Community](https://experienceleaguecommunities.adobe.com/t5/adobe-experience-manager/ct-p/adobe-experience-manager-community)
- [AEM Developers on Stack Overflow](https://stackoverflow.com/questions/tagged/aem)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Based on official Adobe Experience Manager documentation
- Built with guidance from the Adobe Experience League community
- Validated against AEM SDK 2025.11.x and latest JavaDoc
- Reflects real-world implementation patterns from production systems

## Changelog

### Version 1.2.0 (February 2026)
- **Multi-Platform Support**: Claude, Copilot, Gemini, Cursor, Windsurf
- Platform-specific content transformations
- Global installation support
- Quality score: 95/100

### Version 1.1.0 (February 2026)
- Content Fragment Workflows (UI Extensibility)
- App Builder Extensions
- Sling Jobs Integration
- Multi-Step Approval Workflow templates

### Version 1.0.0 (February 2026)
- Initial release with Granite Workflow APIs
- Asset Microservices architecture
- Java 21 support

---

**Maintained by**: Narendra Gandhi
**Last Updated**: February 2026
**SDK Version**: 2025.11.23482.20251120T200914Z-251200
**Quality Score**: 95/100
**Status**: Production Ready
