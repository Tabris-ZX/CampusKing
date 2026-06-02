package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;

public final class SimCard extends BaseCharacterCard {

    public static final String ID = "sim";
    public static final int ORDER = 20;

    public SimCard(CardDefinition definition) {
        super(ORDER, definition);
    }
}
