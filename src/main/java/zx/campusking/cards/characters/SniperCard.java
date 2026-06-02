package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;

public final class SniperCard extends BaseCharacterCard {

    public static final String ID = "sniper";
    public static final int ORDER = 30;

    public SniperCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public boolean sleepsOnSummon() {
        return true;
    }
}
