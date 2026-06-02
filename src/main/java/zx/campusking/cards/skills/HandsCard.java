package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class HandsCard extends BaseSkillCard {

    public static final String ID = "hands";
    public static final int ORDER = 130;

    public HandsCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.discardEnemyHand();
    }
}
