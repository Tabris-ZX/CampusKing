import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const defaultConfigPath = path.resolve(__dirname, "..", "..", "config", "config.yaml");

const DEFAULT_CONFIG = {
  viteApiTarget: "http://127.0.0.1:8080",
  viteWsTarget: "ws://127.0.0.1:8080",
  assetBaseUrl: "",
  assetLocalRoot: "webui/public",
  assetMaxResponseBytes: 1024 * 1024
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
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
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

    parsed[key] = parseScalar(line.slice(separatorIndex + 1));
  }

  return parsed;
}

export { DEFAULT_CONFIG };
