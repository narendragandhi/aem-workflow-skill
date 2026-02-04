#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const os = require('os');

const SKILL_NAME = 'aem-workflow';

function printBanner() {
  console.log(`
╔═══════════════════════════════════════════════════════════╗
║         AEM Workflow Development - Claude AI Skill        ║
║                                                           ║
║  Expert guidance for Adobe Experience Manager workflows   ║
╚═══════════════════════════════════════════════════════════╝
`);
}

function getSkillContent() {
  const skillPath = path.join(__dirname, '..', 'skills', 'aem-workflow', 'SKILL.md');
  if (fs.existsSync(skillPath)) {
    return fs.readFileSync(skillPath, 'utf8');
  }
  // Fallback to docs location
  const docsPath = path.join(__dirname, '..', 'docs', 'SKILL.md');
  if (fs.existsSync(docsPath)) {
    return fs.readFileSync(docsPath, 'utf8');
  }
  throw new Error('SKILL.md not found');
}

function installSkill(targetDir, scope) {
  const skillsDir = path.join(targetDir, '.claude', 'skills');
  const skillFile = path.join(skillsDir, `${SKILL_NAME}.md`);

  // Create directories if they don't exist
  if (!fs.existsSync(skillsDir)) {
    fs.mkdirSync(skillsDir, { recursive: true });
    console.log(`Created directory: ${skillsDir}`);
  }

  // Copy skill file
  const skillContent = getSkillContent();
  fs.writeFileSync(skillFile, skillContent);
  console.log(`Installed skill to: ${skillFile}`);

  return skillFile;
}

function main() {
  printBanner();

  const args = process.argv.slice(2);
  const isGlobal = args.includes('--global') || args.includes('-g');
  const isUninstall = args.includes('--uninstall') || args.includes('-u');
  const showHelp = args.includes('--help') || args.includes('-h');

  if (showHelp) {
    console.log(`
Usage: npx aem-workflow-skill [options]

Options:
  --global, -g     Install to home directory (~/.claude/skills/)
  --uninstall, -u  Remove the installed skill
  --help, -h       Show this help message

Examples:
  npx aem-workflow-skill              # Install to current project
  npx aem-workflow-skill --global     # Install globally
  npx aem-workflow-skill --uninstall  # Remove from current project

Alternative Installation:
  Claude Code Plugin: /plugin marketplace add narendragandhi/aem-workflow-skill
`);
    process.exit(0);
  }

  const targetDir = isGlobal ? os.homedir() : process.cwd();
  const scope = isGlobal ? 'global' : 'project';

  if (isUninstall) {
    const skillFile = path.join(targetDir, '.claude', 'skills', `${SKILL_NAME}.md`);
    if (fs.existsSync(skillFile)) {
      fs.unlinkSync(skillFile);
      console.log(`Uninstalled skill from: ${skillFile}`);
    } else {
      console.log(`Skill not found at: ${skillFile}`);
    }
    process.exit(0);
  }

  try {
    const installedPath = installSkill(targetDir, scope);

    console.log(`
Installation complete!

The AEM Workflow Development skill is now available in Claude Code.
Scope: ${scope} (${targetDir})

Usage:
  Just ask Claude about AEM workflow development, for example:
  - "How do I create a custom workflow process step in AEM Cloud Service?"
  - "Show me how to implement a post-processing workflow for assets"
  - "What's the correct way to programmatically start a workflow?"

The skill will automatically activate for AEM workflow-related queries.

Documentation: https://github.com/narendragandhi/aem-workflow-skill
`);
  } catch (error) {
    console.error(`Error installing skill: ${error.message}`);
    process.exit(1);
  }
}

main();
