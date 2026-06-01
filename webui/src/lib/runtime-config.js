const injectedConfig = typeof __APP_CONFIG__ === "object" && __APP_CONFIG__ ? __APP_CONFIG__ : {};

function normalizeAbsoluteUrl(value) {
  const normalized = String(value || "").trim();
  return normalized ? normalized.replace(/\/+$/, "") : "";
}

function normalizeBasePath(value) {
  const normalized = String(value || "/").trim();
  if (!normalized || normalized === ".") {
    return "/";
  }
  if (/^https?:\/\//i.test(normalized)) {
    return `${normalized.replace(/\/+$/, "")}/`;
  }
  return `/${normalized.replace(/^\/+|\/+$/g, "")}/`.replace(/^\/\/+$/, "/");
}

function normalizePath(value, fallback) {
  const normalized = String(value || fallback).trim();
  return `/${normalized.replace(/^\/+/, "")}`;
}

const baseUrl = normalizeBasePath(injectedConfig.baseUrl);
const configuredApiBase = normalizeAbsoluteUrl(injectedConfig.apiBaseUrl);
const configuredWsBase = normalizeAbsoluteUrl(injectedConfig.wsBaseUrl);
const configuredWsGamePath = normalizePath(injectedConfig.wsGamePath, "/ws/game");
const configuredAssetBase = normalizeAbsoluteUrl(injectedConfig.assetBaseUrl);
const configuredGithubUrl = normalizeAbsoluteUrl(injectedConfig.githubUrl);

export function publicBaseUrl() {
  if (/^https?:\/\//i.test(baseUrl)) {
    return baseUrl.replace(/\/+$/, "");
  }
  return `${window.location.origin}${baseUrl === "/" ? "" : baseUrl.replace(/\/$/, "")}`;
}

export function apiRoot() {
  if (configuredApiBase && /^https?:\/\//i.test(configuredApiBase)) {
    return configuredApiBase;
  }
  return window.location.origin;
}

export function wsRoot() {
  if (configuredWsBase) {
    return configuredWsBase;
  }
  return window.location.origin.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
}

export function wsGamePath() {
  return configuredWsGamePath;
}

export function assetRoot() {
  return configuredAssetBase;
}

export function assetUrl(path) {
  const normalizedPath = String(path || "").replace(/^\/+/, "");
  const base = configuredAssetBase || publicBaseUrl();
  return normalizedPath ? `${base.replace(/\/+$/, "")}/${normalizedPath}` : base;
}

export function githubUrl() {
  return configuredGithubUrl;
}

export async function copyText(text) {
  if (navigator.clipboard?.writeText && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  textarea.style.pointerEvents = "none";
  document.body.appendChild(textarea);
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);

  try {
    const success = document.execCommand("copy");
    if (!success) {
      throw new Error("copy failed");
    }
  } finally {
    document.body.removeChild(textarea);
  }
}
