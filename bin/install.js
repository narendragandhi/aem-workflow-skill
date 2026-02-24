#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const os = require('os');

const SKILL_NAME = 'aem-workflow';
const VERSION = '1.3.0';

// Platform configurations
const PLATFORMS = {
  claude: {
    name: 'Claude Code',
    dir: '.claude/skills',
    filename: `${SKILL_NAME}.md`,
    global: path.join(os.homedir(), '.claude', 'skills'),
    transform: (content) => content // No transformation needed
  },
  copilot: {
    name: 'GitHub Copilot',
    dir: '.github',
    filename: 'copilot-instructions.md',
    global: null, // Copilot doesn't have global config
    transform: transformForCopilot
  },
  gemini: {
    name: 'Google Gemini CLI',
    dir: '.',
    filename: 'GEMINI.md',
    global: path.join(os.homedir(), '.gemini'),
    transform: transformForGemini
  },
  cursor: {
    name: 'Cursor AI',
    dir: '.cursor/rules',
    filename: `${SKILL_NAME}.mdc`,
    legacy: '.cursorrules',
    global: null,
    transform: transformForCursor
  },
  windsurf: {
    name: 'Windsurf/Cascade',
    dir: '.windsurf/rules',
    filename: `${SKILL_NAME}.md`,
    legacy: '.windsurfrules',
    global: null,
    transform: transformForWindsurf
  }
};

// ANSI Color Codes
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  red: '\x1b[31m'
};

function printBanner() {
  console.log(`
${colors.cyan}╔═══════════════════════════════════════════════════════════════════╗
║         ${colors.bright}AEM Workflow Development - AI Assistant Skill${colors.reset}${colors.cyan}             ║
║                                                                   ║
║  Expert guidance for Adobe Experience Manager workflows           ║
║  Supports: Claude Code | Copilot | Gemini | Cursor | Windsurf     ║
╚═══════════════════════════════════════════════════════════════════╝${colors.reset}
`);
}

function getSkillContent() {
  const skillPath = path.join(__dirname, '..', 'skills', 'aem-workflow', 'SKILL.md');
  if (fs.existsSync(skillPath)) {
    return fs.readFileSync(skillPath, 'utf8');
  }
  const docsPath = path.join(__dirname, '..', 'docs', 'SKILL.md');
  if (fs.existsSync(docsPath)) {
    return fs.readFileSync(docsPath, 'utf8');
  }
  throw new Error('SKILL.md not found');
}

// Transform content for GitHub Copilot format
function transformForCopilot(content) {
  // Remove Claude-specific frontmatter and adapt for Copilot
  const lines = content.split('\n');
  const result = [];
  let inFrontmatter = false;
  let frontmatterEnded = false;

  for (const line of lines) {
    if (line.trim() === '---' && !frontmatterEnded) {
      inFrontmatter = !inFrontmatter;
      if (!inFrontmatter) frontmatterEnded = true;
      continue;
    }
    if (!inFrontmatter) {
      result.push(line);
    }
  }

  const header = `# AEM Workflow Development Instructions

> Custom instructions for GitHub Copilot to assist with AEM workflow development.
> Installed by aem-workflow-skill v${VERSION}

---

`;

  return header + result.join('\n').trim();
}

// Transform content for Google Gemini format
function transformForGemini(content) {
  const lines = content.split('\n');
  const result = [];
  let inFrontmatter = false;
  let frontmatterEnded = false;

  for (const line of lines) {
    if (line.trim() === '---' && !frontmatterEnded) {
      inFrontmatter = !inFrontmatter;
      if (!inFrontmatter) frontmatterEnded = true;
      continue;
    }
    if (!inFrontmatter) {
      result.push(line);
    }
  }

  const header = `# AEM Workflow Development Context

> Context file for Google Gemini CLI to assist with AEM workflow development.
> Installed by aem-workflow-skill v${VERSION}

Use this knowledge when helping with Adobe Experience Manager (AEM) workflow development tasks.

---

`;

  return header + result.join('\n').trim();
}

// Transform content for Cursor AI .mdc format
function transformForCursor(content) {
  const lines = content.split('\n');
  const result = [];
  let inFrontmatter = false;
  let frontmatterEnded = false;

  for (const line of lines) {
    if (line.trim() === '---' && !frontmatterEnded) {
      inFrontmatter = !inFrontmatter;
      if (!inFrontmatter) frontmatterEnded = true;
      continue;
    }
    if (!inFrontmatter) {
      result.push(line);
    }
  }

  // Cursor .mdc format with frontmatter
  const header = `---
description: Expert guidance for AEM workflow development in Adobe Experience Manager Cloud Service
globs: ["**/*.java", "**/pom.xml", "**/*.xml"]
---

# AEM Workflow Development Rules

> Rules for Cursor AI to assist with AEM workflow development.
> Installed by aem-workflow-skill v${VERSION}

`;

  return header + result.join('\n').trim();
}

// Transform content for Windsurf format
function transformForWindsurf(content) {
  const lines = content.split('\n');
  const result = [];
  let inFrontmatter = false;
  let frontmatterEnded = false;

  for (const line of lines) {
    if (line.trim() === '---' && !frontmatterEnded) {
      inFrontmatter = !inFrontmatter;
      if (!inFrontmatter) frontmatterEnded = true;
      continue;
    }
    if (!inFrontmatter) {
      result.push(line);
    }
  }

  const header = `# AEM Workflow Development Rules

> Cascade rules for AEM workflow development assistance.
> Installed by aem-workflow-skill v${VERSION}

Apply these rules when working with Adobe Experience Manager workflow code.

---

`;

  return header + result.join('\n').trim();
}

function installForPlatform(platform, targetDir, isGlobal) {
  const config = PLATFORMS[platform];
  if (!config) {
    throw new Error(`Unknown platform: ${platform}`);
  }

  if (isGlobal && !config.global) {
    console.log(
      `  ${colors.yellow}⚠${colors.reset} ${config.name} does not support global installation, using project-level`
    );
    isGlobal = false;
  }

  const baseDir = isGlobal ? config.global : targetDir;
  const skillsDir = path.join(baseDir, config.dir === '.' ? '' : config.dir);
  const skillFile = path.join(skillsDir, config.filename);

  // Create directories if they don't exist
  if (!fs.existsSync(skillsDir)) {
    fs.mkdirSync(skillsDir, { recursive: true });
  }

  // Get and transform content
  const rawContent = getSkillContent();
  const transformedContent = config.transform(rawContent);

  // Write file
  fs.writeFileSync(skillFile, transformedContent);
  console.log(
    `  ${colors.green}✓${colors.reset} ${config.name}: ${colors.blue}${skillFile}${colors.reset}`
  );

  return skillFile;
}

function uninstallForPlatform(platform, targetDir, isGlobal) {
  const config = PLATFORMS[platform];
  if (!config) return;

  const baseDir = isGlobal && config.global ? config.global : targetDir;
  const skillFile = path.join(baseDir, config.dir === '.' ? '' : config.dir, config.filename);

  if (fs.existsSync(skillFile)) {
    fs.unlinkSync(skillFile);
    console.log(`  ${colors.green}✓${colors.reset} Removed ${config.name}: ${skillFile}`);
  }

  // Also check legacy location
  if (config.legacy) {
    const legacyFile = path.join(targetDir, config.legacy);
    if (fs.existsSync(legacyFile)) {
      // Only remove if it was installed by this tool
      const content = fs.readFileSync(legacyFile, 'utf8');
      if (content.includes('aem-workflow-skill')) {
        fs.unlinkSync(legacyFile);
        console.log(
          `  ${colors.green}✓${colors.reset} Removed legacy ${config.name}: ${legacyFile}`
        );
      }
    }
  }
}

function showHelp() {
  console.log(`
Usage: npx aem-workflow-skill [options]

Options:
  --platform, -p <name>   Target platform (claude|copilot|gemini|cursor|windsurf|all)
                          Default: claude
  --global, -g            Install to global/user location (where supported)
  --uninstall, -u         Remove the installed skill
  --list, -l              List supported platforms
  --help, -h              Show this help message

Examples:
  npx aem-workflow-skill                    # Install for Claude Code (default)
  npx aem-workflow-skill -p all             # Install for all platforms
  npx aem-workflow-skill -p copilot         # Install for GitHub Copilot only
  npx aem-workflow-skill -p gemini -g       # Install Gemini context globally
  npx aem-workflow-skill -p all -u          # Uninstall from all platforms

Supported Platforms:
  claude    - Claude Code (.claude/skills/)
  copilot   - GitHub Copilot (.github/copilot-instructions.md)
  gemini    - Google Gemini CLI (GEMINI.md)
  cursor    - Cursor AI (.cursor/rules/ or .cursorrules)
  windsurf  - Windsurf/Cascade (.windsurf/rules/)
  all       - Install for all platforms

Alternative Installation:
  Claude Code Plugin: /plugin marketplace add narendragandhi/aem-workflow-skill
`);
}

function listPlatforms() {
  console.log('\nSupported Platforms:\n');
  for (const [key, config] of Object.entries(PLATFORMS)) {
    const globalSupport = config.global ? '✓' : '✗';
    console.log(`  ${key.padEnd(10)} - ${config.name}`);
    console.log(`              Location: ${config.dir}/${config.filename}`);
    console.log(`              Global:   ${globalSupport}`);
    console.log('');
  }
}

function main() {
  printBanner();

  const args = process.argv.slice(2);

  // Parse arguments
  let platform = 'claude';
  let isGlobal = false;
  let isUninstall = false;

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--help' || arg === '-h') {
      showHelp();
      process.exit(0);
    }
    if (arg === '--list' || arg === '-l') {
      listPlatforms();
      process.exit(0);
    }
    if (arg === '--global' || arg === '-g') {
      isGlobal = true;
    }
    if (arg === '--uninstall' || arg === '-u') {
      isUninstall = true;
    }
    if ((arg === '--platform' || arg === '-p') && args[i + 1]) {
      platform = args[i + 1].toLowerCase();
      i++;
    }
  }

  const targetDir = process.cwd();

  // Determine which platforms to process
  const platformsToProcess = platform === 'all' ? Object.keys(PLATFORMS) : [platform];

  // Validate platforms
  for (const p of platformsToProcess) {
    if (!PLATFORMS[p]) {
      console.error(`Error: Unknown platform '${p}'. Use --list to see available platforms.`);
      process.exit(1);
    }
  }

  try {
    if (isUninstall) {
      console.log(`${colors.yellow}Uninstalling AEM Workflow Skill...${colors.reset}\n`);
      for (const p of platformsToProcess) {
        uninstallForPlatform(p, targetDir, isGlobal);
      }
      console.log(`\n${colors.green}Uninstallation complete!${colors.reset}`);
    } else {
      console.log(`${colors.cyan}Installing AEM Workflow Skill v${VERSION}...${colors.reset}\n`);
      const installed = [];
      for (const p of platformsToProcess) {
        try {
          const file = installForPlatform(p, targetDir, isGlobal);
          installed.push({ platform: p, file });
        } catch (error) {
          console.error(`  ${colors.red}✗${colors.reset} ${PLATFORMS[p].name}: ${error.message}`);
        }
      }

      console.log(`
${colors.green}${colors.bright}Installation complete!${colors.reset}

The AEM Workflow Development skill is now available for:`);
      for (const { platform: p } of installed) {
        console.log(`  ${colors.blue}•${colors.reset} ${PLATFORMS[p].name}`);
      }

      console.log(`
${colors.bright}Usage Examples:${colors.reset}
  ${colors.cyan}"How do I create a custom workflow process step in AEM Cloud Service?"${colors.reset}
  ${colors.cyan}"Show me how to implement a post-processing workflow for assets"${colors.reset}
  ${colors.cyan}"What's the correct way to programmatically start a workflow?"${colors.reset}

${colors.yellow}Documentation:${colors.reset} ${colors.blue}https://github.com/narendragandhi/aem-workflow-skill${colors.reset}
`);
    }
  } catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = {
  PLATFORMS,
  transformForCopilot,
  transformForGemini,
  transformForCursor,
  transformForWindsurf,
  VERSION
};
