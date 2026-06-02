package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardDefinition;

import java.util.List;

public final class ChipsCard extends BaseSkillCard {

    public static final String ID = "chips";
    public static final int ORDER = 120;

    public ChipsCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        int handCount = context.player().getHand().size();
        List<CardInstance> discarded = context.discardHand(context.player(), List.of(), handCount, false);
        context.drawCards(context.player(), discarded.size() + 1);
    }
}
