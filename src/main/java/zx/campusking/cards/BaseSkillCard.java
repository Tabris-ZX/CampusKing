package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;

/**
 * 技能牌基类。
 * 只校验技能牌类型，卡牌参数由注册表提供。
 */
public abstract class BaseSkillCard extends BaseGameCard {

    protected BaseSkillCard(int order, CardDefinition definition) {
        super(order, requireSkill(definition));
    }

    private static CardDefinition requireSkill(CardDefinition definition) {
        if (definition == null || definition.getType() != CardType.SKILL) {
            throw new IllegalArgumentException("技能卡必须使用 SKILL 类型定义");
        }
        return definition;
    }
}
