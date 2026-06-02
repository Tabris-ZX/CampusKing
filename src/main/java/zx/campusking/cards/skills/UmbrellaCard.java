package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.PreventableAction;

public final class UmbrellaCard extends BaseSkillCard {

    public static final String ID = "umbrella";
    public static final int ORDER = 110;

    public UmbrellaCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public boolean bypassesNegate() {
        return true;
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyPrevention(PreventableAction.SKILL_CARD);
    }
}
