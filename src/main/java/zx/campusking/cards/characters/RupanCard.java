package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.cards.CardAttackDamageContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;

import java.util.List;

public final class RupanCard extends BaseCharacterCard {

    public static final String ID = "rupan";
    public static final int ORDER = 55;

    public RupanCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void afterAttackDamage(CardAttackDamageContext context) {
        List<CardInstance> discarded = context.discardRandomDefenderHand(1);
        if (!discarded.isEmpty()) {
            context.match().getLogs().add(context.attackerDefinition().getName() + " 触发特性, 弃置对方 1 张随机手牌: " + context.cardName(discarded.get(0)) + ".");
        }
    }
}
