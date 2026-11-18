const fs = require('fs');
const path = require('path');
const { INLINED_REFERENCES, bundleForSingleFile, getSkillContent } = require('../bin/install');

const SKILL_DIR = path.join(__dirname, '..', 'skills', 'aem-workflow-development');
const skill = fs.readFileSync(path.join(SKILL_DIR, 'SKILL.md'), 'utf8');

// Relative links like [text](references/x.md) or [text](scripts/X.java)
const relativeLinks = (markdown) =>
  [...markdown.matchAll(/\]\(((?:references|scripts)\/[^)\s#]+)\)/g)].map((m) => m[1]);

describe('Skill layout (progressive disclosure)', () => {
  test('every relative link in SKILL.md resolves inside the skill folder', () => {
    const links = relativeLinks(skill);
    expect(links.length).toBeGreaterThan(0);
    for (const rel of links) {
      expect(fs.existsSync(path.join(SKILL_DIR, rel))).toBe(true);
    }
  });

  test('every reference and script is linked from SKILL.md', () => {
    const linked = new Set(relativeLinks(skill));
    for (const dir of ['references', 'scripts']) {
      for (const entry of fs.readdirSync(path.join(SKILL_DIR, dir), { withFileTypes: true })) {
        // Template folders are linked through their README
        const rel = entry.isDirectory() ? `${dir}/${entry.name}/README.md` : `${dir}/${entry.name}`;
        expect(linked).toContain(rel);
      }
    }
  });

  test('frontmatter name matches the skill folder', () => {
    expect(skill).toMatch(new RegExp(`^name: ${path.basename(SKILL_DIR)}$`, 'm'));
  });

  test('SKILL.md stays within its context budget', () => {
    // Detail belongs in references/ and scripts/, loaded only when needed.
    expect(skill.split('\n').length).toBeLessThan(1200);
  });
});

describe('Single-file bundle for installers', () => {
  const sample = [
    '# Main',
    'See [security](references/workflow-security.md) and [code](scripts/Foo.java).',
    'API: [api](references/workflow-api-reference.md)'
  ].join('\n');
  const refs = Object.fromEntries(
    INLINED_REFERENCES.map((rel) => [rel, `# Title of ${path.basename(rel)}`])
  );
  refs['references/workflow-security.md'] =
    '# Security\n\n## Rule\n\n```bash\n# not a heading\n```';
  const bundled = bundleForSingleFile(sample, (rel) => refs[rel]);

  test('inlines each listed reference with its headings nested one level', () => {
    expect(bundled).toContain('<a id="reference-workflow-security"></a>');
    expect(bundled).toContain('\n## Security\n');
    expect(bundled).toContain('\n### Rule\n');
    for (const rel of INLINED_REFERENCES.filter((r) => !r.endsWith('workflow-security.md'))) {
      expect(bundled).toContain(`\n## Title of ${path.basename(rel)}\n`);
    }
  });

  test('leaves comment lines inside code fences untouched', () => {
    expect(bundled).toContain('\n# not a heading\n');
  });

  test('points links to inlined references at their appended section', () => {
    expect(bundled).toContain('[security](#reference-workflow-security)');
  });

  test('points other relative links at the repository', () => {
    expect(bundled).toMatch(/\[code\]\(https:\/\/github\.com\/.+\/scripts\/Foo\.java\)/);
    expect(bundled).toMatch(/\[api\]\(https:\/\/github\.com\/.+\/workflow-api-reference\.md\)/);
  });

  test('installed content has no dead relative links and includes inlined references', () => {
    const content = getSkillContent();
    expect(relativeLinks(content)).toEqual([]);
    for (const rel of INLINED_REFERENCES) {
      expect(content).toContain(`<a id="reference-${path.basename(rel, '.md')}"></a>`);
    }
  });
});
