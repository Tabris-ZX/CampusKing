package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

public final class DragonCard extends BaseCharacterCard {

    public static final String ID = "dragon";
    public static final int ORDER = 50;

    public DragonCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public int modifyAttack(PlayerState owner, CardInstance card, CardDefinition definition, int baseHealth, int attack) {
        return attack + Math.max(0, baseHealth - card.getCurrentHealth());
    }

    @Override
    public boolean handleDefeated(MatchState match, PlayerState owner, CardInstance card, CardDefinition definition) {
        if (card.isRevived()) {
            return false;
        }
        card.setRevived(true);
        card.setCurrentHealth(1);
        match.getLogs().add(definition.getName() + " 首次被击败后回复到 1 点体力.");
        return true;
    }
}
