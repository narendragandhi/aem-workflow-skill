const jest = require('eslint-plugin-jest');
const js = require('@eslint/js');
const prettier = require('eslint-config-prettier');

module.exports = [
  js.configs.recommended,
  prettier,
  {
    plugins: {
      jest: jest
    },
    languageOptions: {
      globals: {
        ...jest.environments.globals.globals,
        process: 'readonly',
        __dirname: 'readonly',
        module: 'readonly',
        require: 'readonly',
        console: 'readonly'
      },
      ecmaVersion: 'latest',
      sourceType: 'commonjs'
    },
    rules: {
      'no-console': 'off',
      'no-unused-vars': 'warn'
    }
  }
];
