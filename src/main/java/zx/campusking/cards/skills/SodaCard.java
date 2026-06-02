package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class SodaCard extends BaseSkillCard {

    public static final String ID = "soda";
    public static final int ORDER = 80;

    public SodaCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public boolean canResolveSkill(CardEffectContext context) {
        String targetPlayerId = context.request().getTargetPlayerId();
        String targetInstanceId = context.request().getTargetInstanceId();
        if (targetPlayerId == null || targetPlayerId.isBlank() || targetInstanceId == null || targetInstanceId.isBlank()) {
            return false;
        }
        if (targetPlayerId.equals(context.player().getPlayerId())) {
            return context.player().getBoard().stream().anyMatch(card -> card.getInstanceId().equals(targetInstanceId));
        }
        if (targetPlayerId.equals(context.enemy().getPlayerId())) {
            return context.enemy().getBoard().stream().anyMatch(card -> card.getInstanceId().equals(targetInstanceId));
        }
        return false;
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.drawCards(context.player(), 2);
        if (context.targetsSelfBoard()) {
            context.healCharacter(context.player(), context.targetBoardCard(context.player()), context.value());
            return;
        }
        context.damageCharacter(context.enemy(), context.targetBoardCard(context.enemy()), context.value());
    }
}
