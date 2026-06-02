package zx.campusking.model;

/**
 * 状态效果类型。
 * 这里定义的是前后端共享的 Buff / Debuff 类型枚举。
 */
public enum StatusEffectType {
    /** 攻击力提升。 */
    ATTACK_UP,
    /** 最大生命提升。 */
    MAX_HP_UP,
    /** 回合开始回血。 */
    TURN_HEAL,
    /** 抵御下一次指定动作。 */
    PREVENT_NEXT_ACTION,
    /** 角色死亡后回复一定生命。 */
    REVIVE_ON_DEATH
}
