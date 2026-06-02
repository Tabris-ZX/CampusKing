package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.cards.CardCombatContext;
import zx.campusking.cards.CardDefeatContext;
import zx.campusking.model.CardDefinition;

public final class DragonCard extends BaseCharacterCard {

    public static final String ID = "dragon";
    public static final int ORDER = 50;

    public DragonCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public int modifyAttack(CardCombatContext context, int attack) {
        return attack + Math.max(0, context.baseHealth() - context.card().getCurrentHealth());
    }

    @Override
    public boolean handleDefeated(CardDefeatContext context) {
        if (context.card().isRevived()) {
            return false;
        }
        context.card().setRevived(true);
        context.card().setCurrentHealth(1);
        context.match().getLogs().add(context.definition().getName() + " 首次被击败后回复到 1 点体力.");
        return true;
    }
}
