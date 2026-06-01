import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "node:path";

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
        target: "http://127.0.0.1:8080",
        changeOrigin: true
      },
      "/ws": {
        target: "ws://127.0.0.1:8080",
        ws: true,
        changeOrigin: true
      }
    },
    open: false
  }
});
