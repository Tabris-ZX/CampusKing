import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "node:path";
import { loadWebuiConfig } from "./scripts/load-config.mjs";

const webuiConfig = loadWebuiConfig();

export default defineConfig({
  plugins: [vue()],
  base: "/",
  build: {
    outDir: resolve(__dirname, "dist"),
    emptyOutDir: true
  },
  server: {
    port: 5173,
    host: "0.0.0.0",
    proxy: {
      "/api": {
        target: webuiConfig.viteApiTarget,
        changeOrigin: true
      },
      "/ws": {
        target: webuiConfig.viteWsTarget,
        ws: true,
        changeOrigin: true
      }
    },
    open: false
  }
});
