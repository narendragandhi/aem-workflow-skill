const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { OUT, SKILLS, skillFiles } = require('../tools/package-skill');

// The release workflow uploads the committed package, so it must match the skill folder.
describe('Released .skill package', () => {
  const listed = () =>
    execFileSync('unzip', ['-Z1', OUT], { encoding: 'utf8' })
      .split('\n')
      .filter((name) => name && !name.endsWith('/'));

  test('contains exactly the files in the skill folder', () => {
    expect(listed().sort()).toEqual(skillFiles().sort());
  });

  test('every file matches its source (run `npm run package` after changing the skill)', () => {
    for (const rel of skillFiles()) {
      const packaged = execFileSync('unzip', ['-p', OUT, rel]);
      expect({ rel, same: packaged.equals(fs.readFileSync(path.join(SKILLS, rel))) }).toEqual({
        rel,
        same: true
      });
    }
  });
});
