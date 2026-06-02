package zx.campusking.cards;

import zx.campusking.model.CardDefinition;

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
}
