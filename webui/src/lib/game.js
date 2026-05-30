import { EFFECT_CATEGORY_TEXT, EFFECT_TYPE_TEXT, SKILL_RANGE_TEXT, TYPE_TEXT } from "./constants";

export function apiBase() {
  return window.location.origin;
}

export async function api(path, options = {}) {
  const response = await fetch(`${apiBase()}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "请求失败");
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
  const base = (assetBaseUrl || apiBase()).replace(/\/$/, "");
  return `${base}/images/texture/${folder}/${card.id}.png`;
}
