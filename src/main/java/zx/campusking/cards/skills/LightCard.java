package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.PreventableAction;

public final class LightCard extends BaseSkillCard {

    public static final String ID = "light";
    public static final int ORDER = 70;

    public LightCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyPrevention(PreventableAction.CHARACTER_ATTACK);
    }
}
