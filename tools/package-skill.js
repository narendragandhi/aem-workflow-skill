#!/usr/bin/env node
// Builds releases/aem-workflow-development.skill (a zip of the skill folder) from
// skills/aem-workflow-development. Requires the `zip` command (macOS and Ubuntu have it).

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const SKILL_ID = 'aem-workflow-development';
const ROOT = path.join(__dirname, '..');
const SKILLS = path.join(ROOT, 'skills');
const OUT = path.join(ROOT, 'releases', `${SKILL_ID}.skill`);

// Files that belong in the package: everything in the skill folder except OS clutter.
function skillFiles(dir = path.join(SKILLS, SKILL_ID), prefix = SKILL_ID) {
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .filter((entry) => !['.DS_Store', 'Thumbs.db'].includes(entry.name))
    .sort((a, b) => a.name.localeCompare(b.name))
    .flatMap((entry) => {
      const rel = `${prefix}/${entry.name}`;
      return entry.isDirectory() ? skillFiles(path.join(dir, entry.name), rel) : [rel];
    });
}

function build() {
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.rmSync(OUT, { force: true });
  // -X: no extra file attributes, so the archive depends only on names and contents
  execFileSync('zip', ['-X', '-q', OUT, ...skillFiles()], { cwd: SKILLS });
  return OUT;
}

if (require.main === module) {
  const out = build();
  console.log(`Wrote ${path.relative(ROOT, out)} (${skillFiles().length} files)`);
}

module.exports = { SKILL_ID, OUT, SKILLS, skillFiles, build };
