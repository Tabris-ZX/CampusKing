package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;

public final class WaterCard extends BaseCharacterCard {

    public static final String ID = "water";
    public static final int ORDER = 30;

    public WaterCard(CardDefinition definition) {
        super(ORDER, definition);
    }
}
