package zx.campusking.model;

/**
 * 技能效果类型。
 * 现在主要用于前端展示效果标签；具体结算逻辑由卡牌类自己的 hook 实现。
 */
public enum EffectType {
    /** 无展示效果。 */
    NONE,
    /** 对敌方全体角色造成伤害。 */
    DAMAGE_ALL_ENEMIES,
    /** 全局持续增益。 */
    GLOBAL_BUFF,
    /** 抵御指定动作。 */
    PREVENT_ACTION,
    /** 抽牌并修改场上角色生命。 */
    DRAW_AND_MODIFY_UNIT,
    /** 死亡后复活类效果。 */
    REVIVE_ALLY,
    /** 弃牌后抽等量牌。 */
    DISCARD_AND_DRAW,
    /** 弃置敌方手牌。 */
    DISCARD_ENEMY_HAND
}
