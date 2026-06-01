package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;

public abstract class BaseGameCard implements GameCard {

    private final int order;
    private final CardDefinition definition;

    protected BaseGameCard(int order, CardDefinition definition) {
        this.order = order;
        this.definition = definition;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public CardDefinition definition() {
        return definition;
    }

    protected static CardDefinition baseDefinition(String id, String name, CardType type, String description) {
        CardDefinition cardDefinition = new CardDefinition();
        cardDefinition.setId(id);
        cardDefinition.setName(name);
        cardDefinition.setType(type);
        cardDefinition.setDescription(description);
        return cardDefinition;
    }
}
