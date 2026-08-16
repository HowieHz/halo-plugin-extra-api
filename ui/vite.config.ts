import { viteConfig } from "@halo-dev/ui-plugin-bundler-kit/vite";
import path from "node:path";
import Icons from "unplugin-icons/vite";

export default viteConfig({
  format: "esm",
  vite: {
    resolve: {
      alias: {
        "@": path.resolve(import.meta.dirname, "src"),
      },
    },
    plugins: [Icons({ compiler: "vue3" })],
  },
});
