const ADMIN_PASSWORD_STORAGE_KEY = "campusking-admin-password";
const ADMIN_AUTH_STORAGE_KEY = "campusking-admin-authenticated";
const DEFAULT_ADMIN_PASSWORD = "123";

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

export function renderMarkdown(markdown) {
  const source = (markdown || "").replace(/\r\n/g, "\n");
  const lines = source.split("\n");
  const html = [];
  let listType = "";
  let inCodeBlock = false;

  function closeList() {
    if (listType) {
      html.push(`</${listType}>`);
      listType = "";
    }
  }

  function openList(nextType) {
    if (listType === nextType) {
      return;
    }
    closeList();
    html.push(`<${nextType}>`);
    listType = nextType;
  }

  for (const rawLine of lines) {
    const escapedLine = escapeHtml(rawLine);
    const line = escapedLine.trim();
    if (inCodeBlock) {
      if (line.startsWith("```")) {
        html.push("</code></pre>");
        inCodeBlock = false;
        continue;
      }
      html.push(`${escapedLine}\n`);
      continue;
    }

    if (!line) {
      closeList();
      continue;
    }

    if (line.startsWith("```")) {
      closeList();
      html.push("<pre><code>");
      inCodeBlock = true;
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
      openList("ul");
      html.push(`<li>${inlineMarkdown(line.slice(2))}</li>`);
      continue;
    }
    if (/^\d+\.\s+/.test(line)) {
      openList("ol");
      html.push(`<li>${inlineMarkdown(line.replace(/^\d+\.\s+/, ""))}</li>`);
      continue;
    }

    closeList();
    html.push(`<p>${inlineMarkdown(line)}</p>`);
  }

  closeList();
  if (inCodeBlock) {
    html.push("</code></pre>");
  }
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
