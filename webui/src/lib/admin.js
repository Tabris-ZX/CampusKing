const ANNOUNCEMENT_STORAGE_KEY = "campusking-admin-announcement";
const CARD_TUNING_STORAGE_KEY = "campusking-admin-card-tuning";
const ADMIN_PASSWORD_STORAGE_KEY = "campusking-admin-password";
const ADMIN_AUTH_STORAGE_KEY = "campusking-admin-authenticated";
const DEFAULT_ADMIN_PASSWORD = "123";

const DEFAULT_ANNOUNCEMENT = `# 我常常追忆过去...
`;

export function loadAnnouncementMarkdown() {
  try {
    return localStorage.getItem(ANNOUNCEMENT_STORAGE_KEY) || DEFAULT_ANNOUNCEMENT;
  } catch {
    return DEFAULT_ANNOUNCEMENT;
  }
}

export function saveAnnouncementMarkdown(markdown) {
  localStorage.setItem(ANNOUNCEMENT_STORAGE_KEY, markdown || DEFAULT_ANNOUNCEMENT);
}

export function loadCardTuning() {
  try {
    const raw = localStorage.getItem(CARD_TUNING_STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    localStorage.removeItem(CARD_TUNING_STORAGE_KEY);
    return {};
  }
}

export function saveCardTuning(tuning) {
  localStorage.setItem(CARD_TUNING_STORAGE_KEY, JSON.stringify(tuning || {}));
}

export function getAdminPassword() {
  try {
    return localStorage.getItem(ADMIN_PASSWORD_STORAGE_KEY) || DEFAULT_ADMIN_PASSWORD;
  } catch {
    return DEFAULT_ADMIN_PASSWORD;
  }
}

export function verifyAdminPassword(password) {
  return (password || "") === getAdminPassword();
}

export function isAdminAuthenticated() {
  try {
    return sessionStorage.getItem(ADMIN_AUTH_STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

export function markAdminAuthenticated() {
  sessionStorage.setItem(ADMIN_AUTH_STORAGE_KEY, "1");
}

export function clearAdminAuthentication() {
  sessionStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
}

export function applyCardTuning(cards, tuning = {}) {
  return (cards || []).map(card => {
    const patch = tuning[card.id];
    if (!patch) {
      return card;
    }
    return {
      ...card,
      attack: patch.attack ?? card.attack,
      health: patch.health ?? card.health,
      secondaryAttack: patch.secondaryAttack ?? card.secondaryAttack,
      secondaryHealth: patch.secondaryHealth ?? card.secondaryHealth,
      description: patch.description ?? card.description
    };
  });
}

export function renderMarkdown(markdown) {
  const source = (markdown || "").replace(/\r\n/g, "\n");
  const escaped = escapeHtml(source);
  const lines = escaped.split("\n");
  const html = [];
  let inList = false;

  function closeList() {
    if (inList) {
      html.push("</ul>");
      inList = false;
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      closeList();
      continue;
    }

    if (line.startsWith("### ")) {
      closeList();
      html.push(`<h3>${inlineMarkdown(line.slice(4))}</h3>`);
      continue;
    }
    if (line.startsWith("## ")) {
      closeList();
      html.push(`<h2>${inlineMarkdown(line.slice(3))}</h2>`);
      continue;
    }
    if (line.startsWith("# ")) {
      closeList();
      html.push(`<h1>${inlineMarkdown(line.slice(2))}</h1>`);
      continue;
    }
    if (line.startsWith("- ")) {
      if (!inList) {
        html.push("<ul>");
        inList = true;
      }
      html.push(`<li>${inlineMarkdown(line.slice(2))}</li>`);
      continue;
    }

    closeList();
    html.push(`<p>${inlineMarkdown(line)}</p>`);
  }

  closeList();
  return html.join("");
}

function inlineMarkdown(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.+?)\*/g, "<em>$1</em>")
    .replace(/`(.+?)`/g, "<code>$1</code>");
}

function escapeHtml(text) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}
