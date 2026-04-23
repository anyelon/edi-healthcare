import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    css: true,
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
      "next/font/google": resolve(__dirname, "src/__mocks__/next-font-google.ts"),
    },
  },
});
