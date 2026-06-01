package zx.campusking.model;

/**
 * 卡牌类型。
 * 当前分为角色牌和技能牌两类。
 */
public enum CardType {
    /** 角色牌，可以召唤到场上并参与战斗。 */
    CHARACTER,
    /** 技能牌，使用后立即或持续产生效果。 */
    SKILL
}
