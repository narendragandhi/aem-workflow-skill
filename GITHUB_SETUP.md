# GitHub Repository Setup Guide

Complete guide to setting up this repository on GitHub.

## Initial Setup

### 1. Create GitHub Repository

```bash
# Option A: Via GitHub CLI
gh repo create aem-workflow-skill --public --description "Claude AI skill for AEM workflow development"

# Option B: Via GitHub web interface
# 1. Go to https://github.com/new
# 2. Repository name: aem-workflow-skill
# 3. Description: Claude AI skill for AEM workflow development
# 4. Public
# 5. Initialize with: None (we have existing code)
# 6. Create repository
```

### 2. Push Local Repository

```bash
# Navigate to repository
cd aem-workflow-skill-repo

# Initialize git (if not already done)
git init

# Add all files
git add .

# Initial commit
git commit -m "Initial commit: AEM Workflow Development skill v1.0.0"

# Add remote
git remote add origin https://github.com/YOUR-USERNAME/aem-workflow-skill.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## Repository Configuration

### 3. Configure Repository Settings

**Settings → General:**

- ✅ Issues enabled
- ✅ Discussions enabled (recommended)
- ✅ Projects enabled (optional)
- ✅ Wiki disabled (use docs/ instead)

**Settings → Branches:**

- Default branch: `main`
- Branch protection rules for `main`:
  - ✅ Require pull request reviews before merging
  - ✅ Require status checks to pass
  - ✅ Require branches to be up to date

**Settings → Pages (Optional):**

- Source: Deploy from branch
- Branch: `main`
- Folder: `/docs`

### 4. Add Topics

Settings → General → Topics:

- `adobe-experience-manager`
- `aem`
- `claude-ai`
- `ai-skill`
- `workflow`
- `aem-cloud-service`
- `java`

### 5. Create Labels

Issues → Labels → New label:

**Type Labels:**

- `bug` - Something isn't working (red)
- `enhancement` - New feature request (blue)
- `documentation` - Documentation improvements (yellow)
- `question` - Questions or help needed (purple)

**Priority Labels:**

- `priority-high` - High priority (red)
- `priority-medium` - Medium priority (orange)
- `priority-low` - Low priority (green)

**Status Labels:**

- `good-first-issue` - Good for newcomers (green)
- `help-wanted` - Extra attention needed (green)
- `wontfix` - Won't be worked on (white)

## Creating First Release

### 6. Create Release Tag

```bash
# Tag version 1.0.0
git tag -a v1.0.0 -m "Release v1.0.0 - Initial release"

# Push tag to GitHub
git push origin v1.0.0
```

This will trigger the GitHub Actions workflow to create a release automatically.

### 7. Manual Release (Alternative)

If automatic release doesn't work:

1. Go to: https://github.com/YOUR-USERNAME/aem-workflow-skill/releases
2. Click "Draft a new release"
3. Tag version: `v1.0.0`
4. Release title: `AEM Workflow Development Skill v1.0.0`
5. Description: Copy from CHANGELOG.md
6. Upload: `releases/aem-workflow-development.skill`
7. Publish release

## Documentation

### 8. Update README Badges

Edit README.md and update:

```markdown
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/YOUR-USERNAME/aem-workflow-skill)](https://github.com/YOUR-USERNAME/aem-workflow-skill/releases)
[![GitHub stars](https://img.shields.io/github/stars/YOUR-USERNAME/aem-workflow-skill)](https://github.com/YOUR-USERNAME/aem-workflow-skill/stargazers)
```

### 9. Setup GitHub Pages (Optional)

If you want to host documentation:

```bash
# Create gh-pages branch
git checkout -b gh-pages
git push origin gh-pages

# Back to main
git checkout main
```

Configure in Settings → Pages → Source → Branch: gh-pages

## Post-Setup Checklist

- [ ] Repository created and code pushed
- [ ] README.md displays correctly
- [ ] LICENSE visible
- [ ] Topics added
- [ ] Labels created
- [ ] Branch protection enabled
- [ ] First release published
- [ ] .skill file downloadable
- [ ] Issues enabled
- [ ] Discussions enabled (optional)

## Optional Enhancements

### GitHub Actions for CI

Create `.github/workflows/validate.yml`:

```yaml
name: Validate Skill

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Check skill structure
        run: |
          test -f docs/SKILL.md || exit 1
          test -f releases/aem-workflow-development.skill || exit 1

      - name: Validate Markdown
        uses: actionlint/markdownlint-cli2-action@v1
        with:
          globs: '**/*.md'
```

### Issue Templates

Create `.github/ISSUE_TEMPLATE/bug_report.md`:

```markdown
---
name: Bug report
about: Create a report to help us improve
title: '[BUG] '
labels: 'bug'
---

**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior.

**Expected behavior**
What you expected to happen.

**Environment:**

- AEM Version: [e.g., Cloud Service 2025.11]
- Java Version: [e.g., 21]
- Skill Version: [e.g., 1.0.0]

**Additional context**
Any other context about the problem.
```

### Pull Request Template

Create `.github/pull_request_template.md`:

```markdown
## Description

Brief description of changes.

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Code refactoring

## Testing

- [ ] Tested locally
- [ ] Code compiles
- [ ] Documentation updated

## Checklist

- [ ] Code follows style guidelines
- [ ] Comments added for complex logic
- [ ] CHANGELOG.md updated
- [ ] No breaking changes (or documented)
```

## Maintenance

### Regular Updates

```bash
# Update skill with new features
# 1. Make changes
# 2. Update CHANGELOG.md
# 3. Commit changes
git add .
git commit -m "feat: Add new workflow pattern example"
git push

# 4. Create new release
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

### Responding to Issues

1. Label issues appropriately
2. Respond within 48 hours
3. Close resolved issues with explanation
4. Link to commits/PRs that fix issues

### Managing Pull Requests

1. Review code thoroughly
2. Request changes if needed
3. Test before merging
4. Update CHANGELOG.md
5. Thank contributors

## Security

### Security Policy

Create `SECURITY.md`:

```markdown
# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

Please report security vulnerabilities to:

- Email: [your-email]
- Do NOT open public issues for security problems
```

## Community

### Encouraging Contributions

1. Be welcoming and helpful
2. Acknowledge contributions
3. Maintain CONTRIBUTORS.md
4. Regular communication
5. Recognition in release notes

---

**Need Help?** Open an issue or discussion!
