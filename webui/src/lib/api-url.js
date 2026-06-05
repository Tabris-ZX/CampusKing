import { apiRoot } from "./runtime-config";

export function apiUrl(path) {
  const normalizedPath = `/${String(path || "").replace(/^\/+/, "")}`;
  return `${apiRoot().replace(/\/+$/, "")}${normalizedPath}`;
}
