package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.PlayerState;

/**
 * 角色战斗 hook 的只读上下文。
 * 目前用于 {@link GameCard#modifyAttack(CardCombatContext, int)}，让单张角色牌基于拥有者、
 * 场上实例和静态定义修正攻击力，而不需要直接依赖 BattleService。
 */
public record CardCombatContext(PlayerState owner, CardInstance card, CardDefinition definition) {

    /**
     * 返回当前形态的基础生命值。
     * 例如鸟女进入第二形态后会读取 secondaryHealth，普通角色则读取 health。
     */
    public int baseHealth() {
        if (card.getFormIndex() > 0 && definition.getSecondaryHealth() != null) {
            return definition.getSecondaryHealth();
        }
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }
}
