# Contributing to AEM Workflow Development Skill

Thank you for your interest in contributing! This document provides guidelines for contributing to this Claude AI skill.

## How to Contribute

### Reporting Issues

If you find an error or have a suggestion:

1. **Check existing issues** - Someone may have already reported it
2. **Create a new issue** with:
   - Clear description of the problem or suggestion
   - AEM version and SDK version if applicable
   - Code examples that demonstrate the issue
   - Expected vs actual behavior

### Submitting Changes

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Test thoroughly**
   - Verify code examples compile
   - Check documentation accuracy
   - Test skill packaging
5. **Commit with clear messages**
   ```bash
   git commit -m "Add: Support for custom participant choosers"
   ```
6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Submit a pull request**

## Contribution Guidelines

### Code Examples

- All Java code must compile with Java 21
- Follow Adobe AEM coding standards
- Include proper error handling
- Add comments for complex logic
- Test against latest AEM SDK

### Documentation

- Use clear, concise language
- Include code examples for complex concepts
- Keep line length reasonable (80-120 characters)
- Use proper Markdown formatting
- Reference official Adobe documentation where applicable

### Skill Modifications

When modifying the skill itself:

1. **Update SKILL.md** - Main skill documentation
2. **Update API reference** - If changing API coverage
3. **Update templates** - Code templates in scripts/
4. **Test packaging** - Ensure skill packages correctly
5. **Update version** - Increment version in metadata

## Areas for Contribution

### High Priority

- [ ] Additional workflow patterns (e.g., multi-step approvals)
- [ ] More participant chooser examples
- [ ] Performance optimization patterns
- [ ] Integration examples (external systems)
- [ ] Migration guides from AEM 6.x workflows

### Documentation

- [ ] More troubleshooting scenarios
- [ ] Video tutorials or diagrams
- [ ] Best practices from production systems
- [ ] Common anti-patterns to avoid
- [ ] Cloud Manager integration guidance

### Code Templates

- [ ] Email notification templates
- [ ] Integration with external APIs
- [ ] Batch processing patterns
- [ ] Error recovery strategies
- [ ] Testing utilities and mocks

## Code Style

### Java

```java
// ✅ Good - Clear, documented, proper error handling
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Descriptive Label"
    }
)
public class MyWorkflowProcess implements WorkflowProcess {
    
    private static final Logger LOG = LoggerFactory.getLogger(MyWorkflowProcess.class);
    
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, 
                       MetaDataMap metaDataMap) throws WorkflowException {
        try {
            // Clear implementation
        } catch (Exception e) {
            LOG.error("Descriptive error message", e);
            throw new WorkflowException("User-friendly error", e);
        }
    }
}
```

### Documentation

```markdown
## Section Title

Clear explanation of concept.

### Subsection

**Important Point**: Highlighted information.

```java
// Code example with comments
WorkflowSession session = resolver.adaptTo(WorkflowSession.class);
```

**Best Practice**: Always do X because Y.
```

## Testing

Before submitting:

1. **Compile all Java templates**
   ```bash
   javac -cp aem-sdk-api.jar YourTemplate.java
   ```

2. **Validate Markdown**
   ```bash
   # Use markdownlint or similar
   markdownlint *.md
   ```

3. **Package the skill**
   ```bash
   python3 package_skill.py aem-workflow-development
   ```

4. **Test in Claude Desktop**
   - Install the skill
   - Test various workflow queries
   - Verify code examples work

## Review Process

Pull requests will be reviewed for:

- ✅ Accuracy against official Adobe documentation
- ✅ Code quality and best practices
- ✅ Clarity of documentation
- ✅ Completeness of examples
- ✅ Testing and validation

Reviews typically take 2-5 business days.

## Getting Help

- **Questions**: Open an issue with the "question" label
- **Discussions**: Use GitHub Discussions
- **Security Issues**: Email [security contact] directly

## Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Credited in significant contributions

Thank you for helping make this skill better! 🙏
