# Quick Start: Hosting on GitHub

**5-Minute Setup Guide**

## What You're Getting

- ✅ Complete GitHub repository structure
- ✅ Professional README with badges
- ✅ Installation and contribution guides
- ✅ Example code and documentation
- ✅ GitHub Actions for releases
- ✅ Pre-packaged .skill file ready to distribute

## Step 1: Extract Files (30 seconds)

```bash
# Extract the archive
tar -xzf aem-workflow-skill-github-repo.tar.gz
cd aem-workflow-skill-repo
```

## Step 2: Create GitHub Repository (1 minute)

**Option A: GitHub CLI (Fastest)**

```bash
gh repo create aem-workflow-skill --public --source=. --remote=origin --push
```

**Option B: Web Interface**

1. Go to https://github.com/new
2. Name: `aem-workflow-skill`
3. Public repository
4. Don't initialize (we have files)
5. Create repository

## Step 3: Push to GitHub (1 minute)

```bash
# Initialize and push
git init
git add .
git commit -m "Initial commit: AEM Workflow Development skill"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/aem-workflow-skill.git
git push -u origin main
```

## Step 4: Create First Release (2 minutes)

```bash
# Create and push tag
git tag -a v1.0.0 -m "Initial release"
git push origin v1.0.0
```

Then on GitHub:

1. Go to Releases → Draft new release
2. Choose tag: v1.0.0
3. Title: "AEM Workflow Development Skill v1.0.0"
4. Upload: `releases/aem-workflow-development.skill`
5. Publish

## Step 5: Configure (1 minute)

1. **Add Topics**: Settings → Topics
   - `adobe-experience-manager`, `aem`, `claude-ai`, `workflow`

2. **Enable Features**: Settings → General
   - ✅ Issues
   - ✅ Discussions (optional)

3. **Update README**: Replace `YOUR-USERNAME` with your GitHub username

## Done! 🎉

Your skill is now:

- ✅ Hosted on GitHub
- ✅ Ready for download
- ✅ Properly documented
- ✅ Easy to contribute to

## Share With Your Team

Send them:

```
📦 AEM Workflow Development Skill for Claude AI

Download: https://github.com/YOUR-USERNAME/aem-workflow-skill/releases

Installation:
1. Download aem-workflow-development.skill
2. Claude Desktop → Settings → Skills → Add Skill
3. Select downloaded file

Start using: Ask Claude any AEM workflow question!
```

## Next Steps

**Immediate:**

- [ ] Update README.md with your GitHub username
- [ ] Test skill download from releases
- [ ] Share with team

**Soon:**

- [ ] Enable GitHub Discussions for Q&A
- [ ] Add issue templates
- [ ] Set up branch protection
- [ ] Create CONTRIBUTORS.md

**Later:**

- [ ] Add more examples
- [ ] Create video tutorial
- [ ] Write blog post
- [ ] Contribute back improvements

## File Structure

```
aem-workflow-skill-repo/
├── README.md                    # Main documentation
├── LICENSE                      # MIT License
├── CHANGELOG.md                 # Version history
├── CONTRIBUTING.md              # Contribution guidelines
├── INSTALLATION.md              # Detailed install guide
├── GITHUB_SETUP.md             # This guide (detailed)
├── .gitignore                   # Git ignore rules
├── .github/
│   └── workflows/
│       └── release.yml          # Auto-release on tag
├── releases/
│   └── aem-workflow-development.skill  # Downloadable skill
├── docs/
│   ├── SKILL.md                # Main skill documentation
│   ├── references/
│   │   └── workflow-api-reference.md  # API docs
│   └── scripts/                # Code templates
│       ├── BasicWorkflowProcessTemplate.java
│       ├── DamAssetWorkflowProcessTemplate.java
│       └── WorkflowStarterTemplate.java
└── examples/
    └── CustomMetadataExtractorWorkflow.java  # Full example
```

## Troubleshooting

**Can't push to GitHub?**

```bash
# Make sure you're authenticated
gh auth login
# or
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

**Release not creating?**

- Check .github/workflows/release.yml exists
- Ensure tag starts with 'v' (v1.0.0)
- Check Actions tab for errors

**Skill file missing?**

- Should be in releases/aem-workflow-development.skill
- If missing, download from original outputs folder

## Getting Help

- GitHub Issues: Report problems
- GitHub Discussions: Ask questions
- Original source: Check aem-workflow-development/ folder

---

**Estimated Total Time: 5 minutes** ⏱️
