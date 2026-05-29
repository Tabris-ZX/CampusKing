package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DeckService {

    private final CardCatalogService cardCatalogService;

    public DeckService(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public List<CardInstance> buildDeck(String playerAId, String playerBId) {
        List<CardInstance> deck = new ArrayList<>();
        for (CardDefinition definition : cardCatalogService.listAll()) {
            CardInstance aCard = new CardInstance(definition.getId(), playerAId, defaultHealth(definition));
            aCard.setExtraLives(defaultExtraLives(definition));
            deck.add(aCard);

            CardInstance bCard = new CardInstance(definition.getId(), playerBId, defaultHealth(definition));
            bCard.setExtraLives(defaultExtraLives(definition));
            deck.add(bCard);
        }
        return deck;
    }

    public void drawOne(MatchState match, PlayerState player) {
        refillDeckIfNeeded(match);
        if (match.getDrawPile().isEmpty()) {
            return;
        }
        CardInstance top = match.getDrawPile().remove(0);
        player.getHand().add(top);
    }

    private void refillDeckIfNeeded(MatchState match) {
        if (!match.getDrawPile().isEmpty() || match.getDiscardPile().isEmpty()) {
            return;
        }
        Collections.shuffle(match.getDiscardPile());
        match.getDrawPile().addAll(match.getDiscardPile());
        match.getDiscardPile().clear();
        match.getLogs().add("墓地已洗回抽牌堆。");
    }

    private int defaultHealth(CardDefinition definition) {
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }

    private int defaultExtraLives(CardDefinition definition) {
        return definition.getTraits() != null && definition.getTraits().contains("lark") ? 1 : 0;
    }
}
