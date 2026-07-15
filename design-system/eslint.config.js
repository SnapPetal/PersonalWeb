export default [
  {
    ignores: ["target/**", "node_modules/**"],
  },
  {
    files: ["**/*.js"],
    rules: {
      "no-undef": "error",
      "no-unused-vars": "error",
    },
  },
];
