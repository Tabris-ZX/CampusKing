import { EFFECT_CATEGORY_TEXT, EFFECT_TYPE_TEXT, RARITY_TEXT, SKILL_RANGE_TEXT, TYPE_TEXT } from "./constants";
import { apiRoot } from "./runtime-config";

export function buildInviteLink(roomCode, baseUrl = apiRoot()) {
  const normalizedRoomCode = (roomCode || "").trim().toUpperCase();
  if (!normalizedRoomCode) {
    return "";
  }
  const normalizedBaseUrl = (baseUrl || apiRoot()).replace(/\/$/, "");
  if (normalizedBaseUrl.includes("/#/")) {
    return `${normalizedBaseUrl}${normalizedBaseUrl.endsWith("/") ? "" : "/"}?roomID=${encodeURIComponent(normalizedRoomCode)}`;
  }
  return `${normalizedBaseUrl}/#/?roomID=${encodeURIComponent(normalizedRoomCode)}`;
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
  const response = await fetch(`${apiRoot()}${path}`, {
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

export function describeRarity(rarity) {
  return RARITY_TEXT[rarity] || rarity || "普通";
}

export function describeEffect(effect) {
  const suffix = effect.remainingTurns != null ? ` · ${effect.remainingTurns} 回合` : "";
  const stackSuffix = effect.stacks > 1 ? ` x${effect.stacks}` : "";
  switch (effect.type) {
    case "ATTACK_UP":
      return `状态: 攻击 +${effect.value}${stackSuffix}${suffix}`;
    case "MAX_HP_UP":
      return `状态: 最大生命 +${effect.value}${stackSuffix}${suffix}`;
    case "TURN_HEAL":
      return `状态: 回合开始回复 +${effect.value}${stackSuffix}${suffix}`;
    case "PREVENT_NEXT_ACTION":
      return `状态: ${describePreventableAction(effect.value)}${stackSuffix}${suffix}`;
    case "REVIVE_ON_DEATH":
      return `状态: 死亡后回复 1/${effect.value}${stackSuffix}${suffix}`;
    default:
      return `状态: ${effect.type}${suffix}`;
  }
}

function describePreventableAction(value) {
  if (Number(value) === 0) {
    return "抵御下一次角色攻击";
  }
  if (Number(value) === 1) {
    return "抵御下一张技能牌";
  }
  return "抵御下一次动作";
}

export function describeAttack(card, currentFormIndex = 0) {
  if (!card) {
    return 0;
  }
  const secondaryAttack = card.exclusive?.secondaryAttack;
  if (currentFormIndex > 0 && secondaryAttack != null) {
    return secondaryAttack;
  }
  return card.attack || 0;
}

export function actionCostOf(card) {
  const cost = Number(card?.actionCost);
  return Number.isFinite(cost) ? Math.max(0, cost) : 1;
}

export function cardImage(cardOrId, cardsMap = {}, assetBaseUrl = "") {
  const card = typeof cardOrId === "string" ? cardsMap[cardOrId] : cardOrId;
  if (!card?.id) {
    return "";
  }
  const folder = card.type === "SKILL" ? "skills" : "characters";
  const normalizedAssetBaseUrl = (assetBaseUrl || "").trim().replace(/\/$/, "");
  if (normalizedAssetBaseUrl) {
    return `${normalizedAssetBaseUrl}/images/texture/${folder}/${card.id}.png`;
  }
  const base = apiRoot().replace(/\/$/, "");
  return `${base}/api/assets/card-images/${folder}/${card.id}`;
}

export function cardTexture(textureId) {
  if (!textureId) {
    return "";
  }
  const base = apiRoot().replace(/\/$/, "");
  return `${base}/api/assets/card-textures/cards/${textureId}`;
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
