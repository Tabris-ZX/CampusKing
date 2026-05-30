package zx.campusking.model;

/**
 * 技能效果类型。
 * 用于驱动后端技能结算逻辑。
 */
public enum EffectType {
    NONE,
    HEAL_BOTH,
    DAMAGE_ALL_ENEMIES,
    GLOBAL_BUFF,
    SHIELD,
    COUNTER_EFFECT,
    DRAW_AND_MODIFY_UNIT,
    REVIVE_ALLY,
    DISCARD_AND_DRAW,
    DISCARD_ENEMY_HAND
}
