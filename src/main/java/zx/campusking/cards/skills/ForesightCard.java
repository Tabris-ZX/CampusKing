package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class ForesightCard extends BaseSkillCard {

    public static final String ID = "foresight";
    public static final int ORDER = 65;

    public ForesightCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.gainActionPoint();
        context.damageSelf(5);
    }
}
