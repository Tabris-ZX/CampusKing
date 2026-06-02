package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;

public final class MealCard extends BaseCharacterCard {

    public static final String ID = "meal";
    public static final int ORDER = 10;

    public MealCard(CardDefinition definition) {
        super(ORDER, definition);
    }
}
