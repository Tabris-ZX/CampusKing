package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

public final class RobinCard extends BaseCharacterCard {

    public static final String ID = "robin";
    public static final int ORDER = 40;

    public RobinCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public boolean handleDefeated(MatchState match, PlayerState owner, CardInstance card, CardDefinition definition) {
        if (card.getExtraLives() <= 0) {
            return false;
        }
        card.setExtraLives(card.getExtraLives() - 1);
        card.setFormIndex(card.getFormIndex() + 1);
        card.setCurrentHealth(healthForForm(definition, card.getFormIndex()));
        match.getLogs().add(definition.getName() + " 触发了额外命效果.");
        return true;
    }

    private int healthForForm(CardDefinition definition, int formIndex) {
        if (formIndex > 0 && definition.getSecondaryHealth() != null) {
            return definition.getSecondaryHealth();
        }
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }
}
