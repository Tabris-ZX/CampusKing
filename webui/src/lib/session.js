import { STORAGE_KEY } from "./constants";

const DEFAULT_PLAYER_NAME_KEY = `${STORAGE_KEY}:default-player-name`;

function generateDefaultPlayerName() {
  const suffix = Math.floor(Math.random() * 10000).toString().padStart(4, "0");
  return `用户-${suffix}`;
}

function getStoredDefaultPlayerName() {
  try {
    const existing = sessionStorage.getItem(DEFAULT_PLAYER_NAME_KEY);
    if (existing && existing.trim()) {
      return existing.trim();
    }
    const generated = generateDefaultPlayerName();
    sessionStorage.setItem(DEFAULT_PLAYER_NAME_KEY, generated);
    return generated;
  } catch {
    return generateDefaultPlayerName();
  }
}

function normalizeSession(session) {
  if (!session || typeof session !== "object") {
    return null;
  }
  return {
    ...session,
    playerName: (session.playerName || "").trim() || getStoredDefaultPlayerName()
  };
}

export function loadSession() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = normalizeSession(JSON.parse(raw));
    if (session) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    }
    return session;
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function saveSession(session) {
  const normalized = normalizeSession(session);
  if (!normalized) {
    sessionStorage.removeItem(STORAGE_KEY);
    return;
  }
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
}

export function clearSession() {
  sessionStorage.removeItem(STORAGE_KEY);
}

export function generateClientToken() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    return window.crypto.randomUUID().replaceAll("-", "");
  }
  if (window.crypto && typeof window.crypto.getRandomValues === "function") {
    const buffer = new Uint8Array(16);
    window.crypto.getRandomValues(buffer);
    return Array.from(buffer, value => value.toString(16).padStart(2, "0")).join("");
  }
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`;
}

export function ensureSessionPlayerName(session) {
  return normalizeSession(session)?.playerName || getStoredDefaultPlayerName();
}
