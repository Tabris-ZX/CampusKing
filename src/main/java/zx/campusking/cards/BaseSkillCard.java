package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

/**
 * 技能牌基类。
 * 负责填充技能通用字段，让具体技能类只实现可用性和结算效果。
 */
public abstract class BaseSkillCard extends BaseGameCard {

    protected BaseSkillCard(
            int order,
            String id,
            String name,
            String description,
            EffectType effectType,
            EffectCategory effectCategory,
            int effectValue,
            int effectDuration,
            SkillRange skillRange
    ) {
        super(order, skill(id, name, description, effectType, effectCategory, effectValue, effectDuration, skillRange));
    }

    private static CardDefinition skill(
            String id,
            String name,
            String description,
            EffectType effectType,
            EffectCategory effectCategory,
            int effectValue,
            int effectDuration,
            SkillRange skillRange
    ) {
        CardDefinition definition = baseDefinition(id, name, CardType.SKILL, description);
        definition.setAttack(0);
        definition.setHealth(0);
        definition.setEffectType(effectType);
        definition.setEffectCategory(effectCategory);
        definition.setEffectValue(effectValue);
        definition.setEffectDuration(effectDuration);
        definition.setSkillRange(skillRange);
        return definition;
    }
}
