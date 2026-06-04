package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardRarity;

import java.util.List;

public final class AlchemyCard extends BaseSkillCard {

    public static final String ID = "alchemy";
    public static final int ORDER = 63;

    public AlchemyCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public int requiredHandDiscardCount() {
        return 1;
    }

    @Override
    public boolean canResolveSkill(CardEffectContext context) {
        List<String> discardIds = context.request().getDiscardInstanceIds();
        if (discardIds == null || discardIds.size() != 1 || discardIds.get(0) == null || discardIds.get(0).isBlank()) {
            return false;
        }
        String discardId = discardIds.get(0);
        if (discardId.equals(context.request().getHandInstanceId())) {
            return false;
        }
        return context.player().getHand().stream()
                .anyMatch(card -> card.getInstanceId().equals(discardId));
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        List<CardInstance> discarded = context.discardHand(context.player(), context.request().getDiscardInstanceIds(), 1, false);
        if (discarded.isEmpty()) {
            return;
        }
        CardDefinition discardedDefinition = context.cardDefinition(discarded.get(0));
        int repeats = discardedDefinition.getRarity() == CardRarity.RARE ? 2 : 1;
        context.gainActionPoints(repeats);
        int drawn = context.drawCards(context.player(), repeats);
        context.match().getLogs().add(context.player().getName() + " 通过炼金术转化了 "
                + context.cardName(discarded.get(0)) + ", 抽了 " + drawn + " 张牌.");
    }
}
