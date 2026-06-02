package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.cards.CardDefeatContext;
import zx.campusking.model.CardDefinition;

public final class RobinCard extends BaseCharacterCard {

    public static final String ID = "robin";
    public static final int ORDER = 40;

    public RobinCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public boolean handleDefeated(CardDefeatContext context) {
        if (context.card().getExtraLives() <= 0) {
            return false;
        }
        context.card().setExtraLives(context.card().getExtraLives() - 1);
        context.card().setFormIndex(context.card().getFormIndex() + 1);
        context.card().setCurrentHealth(context.healthForForm(context.card().getFormIndex()));
        context.match().getLogs().add(context.definition().getName() + " 触发了额外命效果.");
        return true;
    }
}
