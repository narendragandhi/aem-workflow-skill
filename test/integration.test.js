const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

describe('Installation Integration', () => {
  let tmpDir;
  const installScript = path.join(__dirname, '..', 'bin', 'install.js');

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'aem-workflow-test-'));
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  test('full installation for all platforms in project directory', () => {
    // Run the install script targeting the tmp directory
    // We use process.cwd() in the script, so we need to run it from the tmp directory
    // or mock the target directory.
    // For this integration test, we'll run the script via node and pass args

    execSync(`node ${installScript} -p all`, {
      cwd: tmpDir,
      env: { ...process.env, PWD: tmpDir }
    });

    // Verify files
    expect(fs.existsSync(path.join(tmpDir, '.claude/skills/aem-workflow.md'))).toBe(true);
    expect(fs.existsSync(path.join(tmpDir, '.github/copilot-instructions.md'))).toBe(true);
    expect(fs.existsSync(path.join(tmpDir, 'GEMINI.md'))).toBe(true);
    expect(fs.existsSync(path.join(tmpDir, '.cursor/rules/aem-workflow.mdc'))).toBe(true);
    expect(fs.existsSync(path.join(tmpDir, '.windsurf/rules/aem-workflow.md'))).toBe(true);
  });

  test('specific platform installation', () => {
    execSync(`node ${installScript} -p gemini`, {
      cwd: tmpDir
    });

    expect(fs.existsSync(path.join(tmpDir, 'GEMINI.md'))).toBe(true);
    expect(fs.existsSync(path.join(tmpDir, '.github/copilot-instructions.md'))).toBe(false);
  });

  test('uninstallation removes files', () => {
    // Install first
    execSync(`node ${installScript} -p gemini`, { cwd: tmpDir });
    expect(fs.existsSync(path.join(tmpDir, 'GEMINI.md'))).toBe(true);

    // Uninstall
    execSync(`node ${installScript} -p gemini -u`, { cwd: tmpDir });
    expect(fs.existsSync(path.join(tmpDir, 'GEMINI.md'))).toBe(false);
  });
});
