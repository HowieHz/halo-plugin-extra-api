import { defineConfig } from "vite";

export default defineConfig({
  build: {
    outDir: "build/dist",
    lib: {
      entry: "src/main.js",
      fileName: "shiki",
      name: "shiki",
      formats: ["umd"],
    },
  },
});
