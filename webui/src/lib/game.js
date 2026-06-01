import { EFFECT_CATEGORY_TEXT, EFFECT_TYPE_TEXT, SKILL_RANGE_TEXT, TYPE_TEXT } from "./constants";

export function apiBase() {
  return window.location.origin;
}

export function buildInviteLink(roomCode, baseUrl = window.location.origin) {
  const normalizedRoomCode = (roomCode || "").trim().toUpperCase();
  if (!normalizedRoomCode) {
    return "";
  }
  const normalizedBaseUrl = (baseUrl || window.location.origin).replace(/\/$/, "");
  return `${normalizedBaseUrl}?roomID=${encodeURIComponent(normalizedRoomCode)}`;
}

function playerCountOf(match) {
  if (Number.isFinite(Number(match?.playerCount))) {
    return Number(match.playerCount);
  }
  return Array.isArray(match?.players) ? match.players.length : 0;
}

export function getInviteBlockedReason(match) {
  if (!match) {
    return "对局数据未同步，暂时无法复制邀请链接。";
  }
  if (match.phase === "FINISHED") {
    return "对局已结束，无法邀请其他玩家。";
  }
  if (match.mode === "PVE") {
    return "人机局已满，无法邀请其他玩家。";
  }
  if (playerCountOf(match) >= 2) {
    return "房间已满，无法邀请其他玩家。";
  }
  return "";
}

export function isMissingRoomError(error) {
  return error?.status === 404 && (error.message || "").includes("房间不存在");
}

export async function api(path, options = {}) {
  const response = await fetch(`${apiBase()}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  const data = await response.json();
  if (!response.ok) {
    const error = new Error(data.error || "请求失败");
    error.status = response.status;
    throw error;
  }
  return data;
}

export function describeType(type) {
  return TYPE_TEXT[type] || type || TYPE_TEXT.UNKNOWN;
}

export function describeSkillRange(range) {
  return SKILL_RANGE_TEXT[range] || "未定义";
}

export function describeEffectType(effectType) {
  return EFFECT_TYPE_TEXT[effectType] || effectType || "无";
}

export function describeEffectCategory(effectCategory) {
  return EFFECT_CATEGORY_TEXT[effectCategory] || effectCategory || "未定义";
}

export function describeEffect(effect) {
  const suffix = effect.remainingTurns != null ? ` · ${effect.remainingTurns} 回合` : "";
  const stackSuffix = effect.stacks > 1 ? ` x${effect.stacks}` : "";
  switch (effect.type) {
    case "ATTACK_UP":
      return `Buff：攻击 +${effect.value}${suffix}`;
    case "MAX_HP_UP":
      return `Buff：最大生命 +${effect.value}${suffix}`;
    case "TURN_HEAL":
      return `Buff：回合回血 +${effect.value}${suffix}`;
    case "SHIELD":
      return `Buff：护盾${stackSuffix}${suffix}`;
    case "BLOCK_DAMAGE":
      return `Buff：免伤${stackSuffix}${suffix}`;
    case "NEGATE_NEXT_SKILL":
      return `Debuff：对方技能无效${stackSuffix}${suffix}`;
    case "REVIVE_ON_DEATH":
      return `Buff：死亡后回复 1/${effect.value}${suffix}`;
    default:
      return `${effect.category === "debuff" ? "Debuff" : "Buff"}：${effect.type}${suffix}`;
  }
}

export function describeAttack(card, currentFormIndex = 0) {
  if (!card) {
    return 0;
  }
  if (card.attackText) {
    return currentFormIndex > 0 && card.secondaryAttack != null
      ? `${card.secondaryAttack}`
      : card.attackText;
  }
  if (currentFormIndex > 0 && card.secondaryAttack != null) {
    return card.secondaryAttack;
  }
  return card.attack || 0;
}

export function describeHealth(card, currentFormIndex = 0) {
  if (!card) {
    return 0;
  }
  if (card.secondaryHealth != null && currentFormIndex > 0) {
    return card.secondaryHealth;
  }
  return card.health || 0;
}

export function cardImage(cardOrId, cardsMap = {}, assetBaseUrl = "") {
  const card = typeof cardOrId === "string" ? cardsMap[cardOrId] : cardOrId;
  if (!card?.id) {
    return "";
  }
  const folder = card.type === "SKILL" ? "skills" : "characters";
  const base = apiBase().replace(/\/$/, "");
  return `${base}/api/assets/card-images/${folder}/${card.id}`;
}

export function swapCardImageToFallback(event, cardOrId, cardsMap = {}, assetBaseUrl = "") {
  const target = event?.target;
  if (!target) {
    return false;
  }

  const imageUrl = cardImage(cardOrId, cardsMap, assetBaseUrl);
  if (!imageUrl) {
    return false;
  }

  if (target.dataset.fallbackApplied === "true") {
    return false;
  }

  target.dataset.fallbackApplied = "true";
  target.src = `${imageUrl}${imageUrl.includes("?") ? "&" : "?"}retry=1`;
  return true;
}
