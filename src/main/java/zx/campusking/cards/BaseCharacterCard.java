package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;
import zx.campusking.model.EffectType;

/**
 * 角色牌基类。
 * 负责填充角色通用字段，让具体角色类只关注自身数值和特殊战斗 hook。
 */
public abstract class BaseCharacterCard extends BaseGameCard {

    protected BaseCharacterCard(
            int order,
            String id,
            String name,
            String description,
            int attack,
            String attackText,
            int health,
            Integer secondaryAttack,
            Integer secondaryHealth
    ) {
        super(order, character(id, name, description, attack, attackText, health, secondaryAttack, secondaryHealth));
    }

    private static CardDefinition character(
            String id,
            String name,
            String description,
            int attack,
            String attackText,
            int health,
            Integer secondaryAttack,
            Integer secondaryHealth
    ) {
        CardDefinition definition = baseDefinition(id, name, CardType.CHARACTER, description);
        definition.setAttack(attack);
        definition.setAttackText(attackText);
        definition.setHealth(health);
        definition.setSecondaryAttack(secondaryAttack);
        definition.setSecondaryHealth(secondaryHealth);
        definition.setEffectType(EffectType.NONE);
        definition.setEffectValue(0);
        return definition;
    }
}
