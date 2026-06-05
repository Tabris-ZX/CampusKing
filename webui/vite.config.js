import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "node:path";
import { loadWebuiConfig, normalizeBaseUrl } from "./scripts/load-config.mjs";

const webuiConfig = loadWebuiConfig();
const baseUrl = normalizeBaseUrl(webuiConfig.baseUrl);
const backendPort = Number.parseInt(webuiConfig.backendPort, 10) || 8080;
const frontendPort = Number.parseInt(webuiConfig.frontendPort, 10) || 5173;
const apiProxyTarget = `http://127.0.0.1:${backendPort}`;
const wsProxyTarget = `ws://127.0.0.1:${backendPort}`;
const apiProxyPaths = [
  "/assets",
  "/game",
  "/cpk/assets",
  "/cpk/game"
];

export default defineConfig({
  plugins: [vue()],
  base: baseUrl,
  build: {
    outDir: resolve(__dirname, "dist"),
    emptyOutDir: true
  },
  define: {
    __APP_CONFIG__: JSON.stringify({
      baseUrl,
      apiBaseUrl: String(webuiConfig.apiBaseUrl || "").trim(),
      wsBaseUrl: String(webuiConfig.wsBaseUrl || "").trim(),
      wsGamePath: String(webuiConfig.wsGamePath || "/ws/game").trim(),
      assetBaseUrl: String(webuiConfig.assetBaseUrl || "").trim(),
      githubUrl: String(webuiConfig.githubUrl || "").trim()
    })
  },
  server: {
    port: frontendPort,
    host: "0.0.0.0",
    allowedHosts: ["cpk.tabriszx.site", "api.tabriszx.site"],
    cors: {
      origin: ["https://cpk.tabriszx.site"],
      methods: ["GET", "POST", "OPTIONS"],
      allowedHeaders: ["Content-Type"]
    },
    fs: {
      allow: [
        resolve(__dirname, "..")
      ]
    },
    proxy: {
      ...Object.fromEntries(apiProxyPaths.map(proxyPath => [
        proxyPath,
        {
          target: apiProxyTarget,
          changeOrigin: true
        }
      ])),
      "/ws": {
        target: wsProxyTarget,
        ws: true,
        changeOrigin: true
      }
    },
    open: false
  }
});
