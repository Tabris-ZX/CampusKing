package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class BunchCard extends BaseSkillCard {

    public static final String ID = "bunch";
    public static final int ORDER = 100;

    public BunchCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.damageBoard(context.enemy(), context.value());
    }
}
