// ESLint v9 configuration file
// https://eslint.org/docs/latest/use/configure/configuration-files

export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/build/**",
      "**/target/**",
      "**/*.config.js",
      "**/*.config.ts",
      "**/vite-env.d.ts"
    ]
  },
  {
    files: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"],
    rules: {
      // Basic rules - можно расширить по необходимости
      "no-console": "warn",
      "no-unused-vars": ["warn", {
        "argsIgnorePattern": "^_",
        "varsIgnorePattern": "^_"
      }],
      "prefer-const": "warn"
    }
  }
];
