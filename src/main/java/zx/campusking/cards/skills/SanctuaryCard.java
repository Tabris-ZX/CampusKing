package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class SanctuaryCard extends BaseSkillCard {

    public static final String ID = "sanctuary";
    public static final int ORDER = 60;

    public SanctuaryCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyGlobalBuff(context.player());
        context.applyGlobalBuff(context.enemy());
    }
}
