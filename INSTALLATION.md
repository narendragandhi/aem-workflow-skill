# Installation Guide

Complete guide for installing and using the AEM Workflow Development Claude AI skill.

## Prerequisites

### Required
- **Claude Desktop** or access to **Claude.ai** with Skills feature enabled
- Basic understanding of AEM and workflow concepts

### For Development (Optional)
- Java 21 JDK
- Maven 3.8.6+
- AEM as a Cloud Service SDK
- IDE (IntelliJ IDEA, Eclipse, VS Code)

## Installation Methods

### Method 1: Download from Releases (Recommended)

1. **Go to Releases**
   - Visit: https://github.com/YOUR-USERNAME/aem-workflow-skill/releases
   - Download the latest `aem-workflow-development.skill` file

2. **Install in Claude Desktop**
   - Open Claude Desktop
   - Click your profile → Settings
   - Navigate to "Skills" section
   - Click "Add Skill"
   - Select the downloaded `.skill` file
   - Click "Open" or "Add"

3. **Verify Installation**
   - Skill should appear in your Skills list
   - Status should show "Active"
   - Test with: "How do I create a custom workflow in AEM?"

### Method 2: Build from Source

1. **Clone Repository**
   ```bash
   git clone https://github.com/YOUR-USERNAME/aem-workflow-skill.git
   cd aem-workflow-skill
   ```

2. **Package the Skill**
   ```bash
   # If you have the packaging script
   python3 scripts/package_skill.py skill/
   
   # Otherwise, the .skill file is already in releases/
   ```

3. **Install as Above**
   - Follow steps 2-3 from Method 1

## First Use

### Testing the Skill

Try these queries in Claude:

**Basic Queries:**
```
"How do I create a custom workflow process in AEM Cloud Service?"
"Show me the correct imports for Granite Workflow API"
"What's the difference between Granite and CQ Workflow APIs?"
```

**Advanced Queries:**
```
"How do I implement a post-processing workflow for assets?"
"Show me how to programmatically start a workflow"
"How do I create a dynamic participant chooser?"
```

**Troubleshooting:**
```
"Why is my workflow process not found?"
"How do I debug workflow failures?"
"What happened to the DAM Update Asset workflow?"
```

### Expected Behavior

When the skill is active and triggered:
- Claude will reference specific API methods and classes
- Provide code examples using correct packages (`com.adobe.granite.workflow.*`)
- Mention Asset Microservices architecture
- Include Java 21 configuration when relevant
- Reference official Adobe documentation

## Configuration

### Claude Desktop Settings

**Skills Menu:**
- Skills → aem-workflow-development
- Toggle: Enabled ✓
- Description: Visible
- Version: 1.0.0

**Trigger Behavior:**
- Automatically activates on workflow-related queries
- No manual activation needed
- Can be manually referenced: "@aem-workflow-development"

## Troubleshooting Installation

### Skill Not Appearing

**Problem**: Skill doesn't show in Skills list

**Solutions:**
1. Restart Claude Desktop
2. Check file extension is `.skill` (not `.zip`)
3. Verify file isn't corrupted (re-download)
4. Check Claude Desktop version (update if needed)

### Skill Not Triggering

**Problem**: Skill installed but not activating

**Solutions:**
1. Check skill is "Enabled" in settings
2. Use explicit trigger: "@aem-workflow-development your question"
3. Ask more specific workflow questions
4. Restart Claude Desktop

### Skill Errors

**Problem**: Skill shows error or won't load

**Solutions:**
1. Delete and reinstall the skill
2. Check for skill file corruption
3. Try older/newer version
4. Check Claude Desktop logs
5. Report issue on GitHub

## Updating

### Automatic Updates
- Claude Desktop may auto-update skills
- Check Settings → Skills for update notifications

### Manual Updates

1. **Check for New Version**
   ```bash
   git pull origin main
   # or download latest release
   ```

2. **Remove Old Version**
   - Settings → Skills → aem-workflow-development
   - Click "Remove" or "Delete"

3. **Install New Version**
   - Follow installation steps above
   - Use new `.skill` file

## Verification Checklist

After installation, verify:

- [ ] Skill appears in Skills list
- [ ] Status shows "Active" or "Enabled"
- [ ] Basic query works: "How do I create AEM workflow?"
- [ ] Code examples use `com.adobe.granite.workflow.*`
- [ ] References Asset Microservices
- [ ] Mentions Java 21 when relevant

## Usage Tips

### Getting Best Results

1. **Be Specific**
   - ✅ "How do I implement WorkflowProcess with error handling?"
   - ❌ "Tell me about workflows"

2. **Mention Context**
   - Include: "in AEM Cloud Service"
   - Include: "using Java 21"
   - Include: "with Granite Workflow API"

3. **Reference Components**
   - "WorkflowSession", "WorkItem", "MetaDataMap"
   - Helps Claude understand technical depth needed

4. **Ask for Examples**
   - "Show me code for..."
   - "Give me an example of..."
   - "What's the template for..."

### Common Workflows

**Development:**
1. Ask for code template
2. Get explanation of components
3. Request error handling patterns
4. Ask about testing approaches

**Troubleshooting:**
1. Describe the problem
2. Share error messages
3. Ask for diagnostic steps
4. Request solutions

**Learning:**
1. Ask conceptual questions
2. Request architecture overview
3. Compare old vs new approaches
4. Understand best practices

## Support

### Getting Help

1. **Documentation**: Read skill's SKILL.md
2. **Issues**: Check GitHub Issues
3. **Discussions**: Use GitHub Discussions
4. **Community**: Adobe Experience League forums

### Reporting Problems

When reporting installation issues:
- Claude Desktop version
- Operating system
- Exact error message
- Steps to reproduce
- Screenshots if applicable

## Uninstallation

To remove the skill:

1. Open Claude Desktop Settings
2. Navigate to Skills
3. Find "aem-workflow-development"
4. Click Remove/Delete
5. Confirm removal

Skill data is removed immediately.

## Next Steps

After installation:

1. **Explore the Skill**
   - Try different types of queries
   - Test code generation
   - Ask for explanations

2. **Read Documentation**
   - Check README.md for overview
   - Review API reference
   - Study code templates

3. **Start Developing**
   - Create your first workflow
   - Test in local SDK
   - Deploy to Cloud Service

4. **Provide Feedback**
   - Star the repo if helpful
   - Report issues
   - Suggest improvements

---

**Need Help?** Open an issue: https://github.com/YOUR-USERNAME/aem-workflow-skill/issues
