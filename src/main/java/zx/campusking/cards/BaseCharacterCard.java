package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;

/**
 * 角色牌基类。
 * 只校验角色牌类型，卡牌数值由注册表提供。
 */
public abstract class BaseCharacterCard extends BaseGameCard {

    protected BaseCharacterCard(int order, CardDefinition definition) {
        super(order, requireCharacter(definition));
    }

    private static CardDefinition requireCharacter(CardDefinition definition) {
        if (definition == null || definition.getType() != CardType.CHARACTER) {
            throw new IllegalArgumentException("角色卡必须使用 CHARACTER 类型定义");
        }
        return definition;
    }
}
