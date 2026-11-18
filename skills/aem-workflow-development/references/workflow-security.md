# Security Best Practices

## Input Validation and Sanitization

Always validate and sanitize user inputs in workflow processes:

```java
private String sanitizeGroupName(String input) {
    if (input == null) {
        return "default";
    }
    return input.replaceAll("[^a-zA-Z0-9-_]", "");
}

private String sanitizePath(String path) {
    if (path == null) {
        return null;
    }
    return path.replaceAll("[^a-zA-Z0-9/._-]", "");
}

private String extractDepartment(String path) {
    if (path == null) {
        return "default";
    }
    String[] segments = path.split("/");
    if (segments.length > 3) {
        return sanitizeGroupName(segments[3]);
    }
    return "default";
}
```

**Key validation rules:**

- Reject paths containing `..` (parent directory traversal)
- Sanitize user-provided group names to prevent injection
- Validate workflow payload paths are within expected locations
- Use allowlist validation when possible

## Service User Configuration

Use dedicated service users with minimal required permissions:

```java
@Reference
private ResourceResolverFactory resolverFactory;

private ResourceResolver getServiceResolver() throws LoginException {
    Map<String, Object> authInfo = Collections.singletonMap(
        ResourceResolverFactory.SUBSERVICE,
        "workflow-service"
    );
    return resolverFactory.getServiceResourceResolver(authInfo);
}
```

**Service user best practices:**

- Create dedicated service users for each workflow subsystem
- Use `subservice` authentication (not username/password)
- Grant only read/write permissions for specific paths
- Never use administrative accounts in workflow code

## Permission Model for Approval Workflows

Design permission scopes carefully:

```java
/**
 * Approval workflow permission structure:
 *
 * /content/projects/[project]
 *   ├── authors (contributors)
 *   │   └── can create, edit own content
 *   ├── reviewers (reviewers)
 *   │   └── can approve/reject content
 *   ├── managers (managers)
 *   │   └── can escalate, override decisions
 *   └── governance (content governance)
 *       └── final approval authority
 */
```

**Permission hierarchy:**

- Authors: Create and edit content, submit for review
- Reviewers: Approve/reject at department level
- Managers: Escalate issues, override decisions
- Governance: Final approval authority

## Preventing Workflow Injection

Secure workflow model references:

```java
private static final String WORKFLOW_MODEL_PATH_PREFIX = "/var/workflow/models/";

private WorkflowModel getValidatedModel(WorkflowSession session, String modelPath)
        throws WorkflowException {
    // Validate path prefix to prevent injection
    if (modelPath == null || !modelPath.startsWith(WORKFLOW_MODEL_PATH_PREFIX)) {
        throw new WorkflowException("Invalid workflow model path");
    }

    // Prevent path traversal attacks
    String normalizedPath = modelPath.replaceAll("\\.\\.", "");
    return session.getModel(normalizedPath);
}
```

## Secure Workflow Metadata Handling

Protect sensitive workflow metadata:

```java
private static final String[] SENSITIVE_KEYS = {
    "password",
    "apiKey",
    "secret",
    "credential"
};

private boolean isSensitiveKey(String key) {
    if (key == null) return false;
    key = key.toLowerCase();
    for (String sensitive : SENSITIVE_KEYS) {
        if (key.contains(sensitive)) {
            return true;
        }
    }
    return false;
}

private void storeMetadataSafely(MetaDataMap metadata, String key, String value) {
    if (isSensitiveKey(key)) {
        LOG.warn("Refusing to store potentially sensitive metadata: {}", key);
        return;
    }
    metadata.put(key, value);
}
```

## Audit Logging

Log workflow actions for security auditing:

```java
private void logWorkflowAction(String action, String user, String payload) {
    Map<String, Object> auditData = new HashMap<>();
    auditData.put("action", action);
    auditData.put("user", user);
    auditData.put("payload", sanitizePath(payload));
    auditData.put("timestamp", System.currentTimeMillis());

    LOG.info("WORKFLOW_AUDIT: {}", auditData);
}
```

**Audit log recommendations:**

- Log all approval/rejection decisions with user ID
- Log escalation events and reasons
- Log workflow initiation and completion
- Include payload path (sanitized) in all log entries

## CSRF Protection for Workflow APIs

When workflow APIs are exposed externally:

```java
private static final Set<String> VALID_TOKENS = ConcurrentHashMap.newKeySet();

public boolean validateCsrfToken(String token, String userId) {
    if (token == null || userId == null) {
        return false;
    }
    return VALID_TOKENS.contains(token) && !isExpired(token);
}
```

## Secure Process Arguments

Validate workflow process arguments:

```java
// Process arguments configure a step and are fixed at model design time.
// DECISION is per OR Split branch: which branch runs is the reviewer's choice.
private static final Set<String> ALLOWED_PROCESS_ARGS = Set.of(
    "DECISION",
    "EDIT_PROPERTY",
    "THRESHOLD_HOURS"
);

private void validateProcessArgs(String args) throws WorkflowException {
    if (args == null || args.isEmpty()) {
        return;
    }
    for (String part : args.split(",")) {
        String key = part.split(":")[0];
        if (!ALLOWED_PROCESS_ARGS.contains(key)) {
            throw new WorkflowException("Invalid process argument: " + key);
        }
    }
}
```
