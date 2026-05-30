package zx.campusking.model;

/**
 * 状态效果类型。
 * 这里定义的是前后端共享的 Buff / Debuff 类型枚举。
 */
public enum StatusEffectType {
    ATTACK_UP,
    MAX_HP_UP,
    TURN_HEAL,
    SHIELD,
    BLOCK_DAMAGE,
    NEGATE_NEXT_SKILL,
    REVIVE_ON_DEATH
}
