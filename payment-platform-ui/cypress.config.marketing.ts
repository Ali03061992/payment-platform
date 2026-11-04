import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    baseUrl: "http://localhost:4200",
    env: {
      apiUrl: "http://localhost:8081",
      mailpitUrl: "http://localhost:8025",
    },
    viewportWidth: 1280,
    viewportHeight: 720,
    defaultCommandTimeout: 20000,
    requestTimeout: 20000,
    responseTimeout: 30000,
    video: true,
    videoCompression: 30,
    screenshotOnRunFailure: false,
    specPattern: "cypress/e2e/99-presentation.cy.ts",
    supportFile: "cypress/support/e2e.ts",
    retries: {
      runMode: 0,
      openMode: 0,
    },
  },
});
