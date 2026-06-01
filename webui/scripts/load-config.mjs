import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const defaultConfigPath = path.resolve(__dirname, "..", "..", "config", "config.yaml");

const DEFAULT_CONFIG = {
  backendPort: 8080,
  baseUrl: "/",
  apiBaseUrl: "",
  wsBaseUrl: "",
  wsGamePath: "/ws/game",
  assetBaseUrl: "",
  assetLocalRoot: "webui/public",
  assetMaxResponseBytes: 1024 * 1024
};

const KEY_ALIASES = {
  "server.backendPort": "backendPort",
  "frontend.baseUrl": "baseUrl",
  "frontend.apiBaseUrl": "apiBaseUrl",
  "frontend.wsBaseUrl": "wsBaseUrl",
  "websocket.gamePath": "wsGamePath",
  "asset.baseUrl": "assetBaseUrl",
  "asset.localRoot": "assetLocalRoot",
  "asset.maxResponseBytes": "assetMaxResponseBytes"
};

function stripInlineComment(value) {
  let quote = "";
  for (let index = 0; index < value.length; index += 1) {
    const char = value[index];
    if ((char === "\"" || char === "'") && value[index - 1] !== "\\") {
      quote = quote === char ? "" : (quote || char);
      continue;
    }
    if (char === "#" && !quote) {
      return value.slice(0, index).trim();
    }
  }
  return value.trim();
}

function parseScalar(value) {
  const normalized = stripInlineComment(value);
  if (!normalized) {
    return "";
  }
  if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
    return normalized.slice(1, -1);
  }
  if (/^-?\d+$/.test(normalized)) {
    return Number.parseInt(normalized, 10);
  }
  if (normalized === "true") {
    return true;
  }
  if (normalized === "false") {
    return false;
  }
  return normalized;
}

export function loadWebuiConfig(configPath = process.env.CAMPUSKING_WEBUI_CONFIG || defaultConfigPath) {
  const resolvedPath = path.resolve(configPath);
  if (!existsSync(resolvedPath)) {
    return { ...DEFAULT_CONFIG };
  }

  const parsed = { ...DEFAULT_CONFIG };
  const content = readFileSync(resolvedPath, "utf8");
  let section = "";

  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    if (/^\s*-/.test(line)) {
      continue;
    }

    const separatorIndex = line.indexOf(":");
    if (separatorIndex <= 0) {
      continue;
    }

    const key = line.slice(0, separatorIndex).trim();
    if (!key || key.includes(" ")) {
      continue;
    }

    const value = parseScalar(line.slice(separatorIndex + 1));
    const isTopLevel = line.search(/\S/) === 0;
    if (isTopLevel && value === "") {
      section = key;
      continue;
    }

    const configKey = isTopLevel ? key : `${section}.${key}`;
    const targetKey = KEY_ALIASES[configKey] || configKey;
    if (targetKey in DEFAULT_CONFIG) {
      parsed[targetKey] = value;
    }
  }

  return parsed;
}

export function normalizeBaseUrl(baseUrl) {
  const normalized = String(baseUrl || "/").trim();
  if (!normalized || normalized === ".") {
    return "/";
  }
  if (/^https?:\/\//i.test(normalized)) {
    return normalized.replace(/\/?$/, "/");
  }
  return `/${normalized.replace(/^\/+|\/+$/g, "")}/`.replace(/^\/\/+$/, "/");
}

export { DEFAULT_CONFIG };
