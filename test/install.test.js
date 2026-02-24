const {
  transformForCopilot,
  transformForGemini,
  transformForCursor,
  transformForWindsurf
} = require('../bin/install');

describe('Content Transformations', () => {
  const sampleContent = `---
name: test-skill
description: test description
---
# Actual Content
This is the skill content.`;

  test('transformForCopilot removes frontmatter and adds header', () => {
    const result = transformForCopilot(sampleContent);
    expect(result).toContain('# AEM Workflow Development Instructions');
    expect(result).toContain('# Actual Content');
    expect(result).not.toContain('name: test-skill');
  });

  test('transformForGemini removes frontmatter and adds header', () => {
    const result = transformForGemini(sampleContent);
    expect(result).toContain('# AEM Workflow Development Context');
    expect(result).toContain('# Actual Content');
    expect(result).not.toContain('name: test-skill');
  });

  test('transformForCursor adds .mdc frontmatter', () => {
    const result = transformForCursor(sampleContent);
    expect(result).toContain('description: Expert guidance');
    expect(result).toContain('globs: ["**/*.java"');
    expect(result).toContain('# Actual Content');
  });

  test('transformForWindsurf adds header', () => {
    const result = transformForWindsurf(sampleContent);
    expect(result).toContain('# AEM Workflow Development Rules');
    expect(result).toContain('Cascade rules');
    expect(result).toContain('# Actual Content');
  });
});
