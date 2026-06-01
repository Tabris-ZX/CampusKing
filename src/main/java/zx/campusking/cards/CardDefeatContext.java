package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

/**
 * 角色死亡 hook 的上下文。
 * 传给 {@link GameCard#handleDefeated(CardDefeatContext)}，用于让龙骑、鸟女这类角色
 * 自己处理“被击败后是否留下、变形或复活”的特殊规则。
 */
public record CardDefeatContext(
        MatchState match,
        PlayerState owner,
        CardInstance card,
        CardDefinition definition
) {

    /**
     * 读取指定形态的基础生命值。
     * 卡牌处理变形或复活时可以用它恢复到对应形态的生命上限。
     */
    public int healthForForm(int formIndex) {
        if (formIndex > 0 && definition.getSecondaryHealth() != null) {
            return definition.getSecondaryHealth();
        }
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }
}
