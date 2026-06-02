package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.cards.GameCard;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardRarity;
import zx.campusking.model.CardType;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private static final int COMMON_CHARACTER_COUNT = 8;
    private static final int RARE_CHARACTER_COUNT = 4;
    private static final int COMMON_SKILL_COUNT = 12;
    private static final int RARE_SKILL_COUNT = 6;
    private static final int COMMON_COPY_LIMIT = 3;
    private static final int RARE_COPY_LIMIT = 2;

    private final CardCatalogService cardCatalogService;

    public DeckService(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public List<CardInstance> buildDeck(String playerAId, String playerBId) {
        List<CardInstance> deck = new ArrayList<>();
        deck.addAll(buildCategory(playerAId, playerBId, CardType.CHARACTER, CardRarity.COMMON, COMMON_CHARACTER_COUNT));
        deck.addAll(buildCategory(playerAId, playerBId, CardType.CHARACTER, CardRarity.RARE, RARE_CHARACTER_COUNT));
        deck.addAll(buildCategory(playerAId, playerBId, CardType.SKILL, CardRarity.COMMON, COMMON_SKILL_COUNT));
        deck.addAll(buildCategory(playerAId, playerBId, CardType.SKILL, CardRarity.RARE, RARE_SKILL_COUNT));
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
        match.getLogs().add("墓地已洗回抽牌堆.");
    }

    private int defaultHealth(CardDefinition definition) {
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }

    private List<CardInstance> buildCategory(String playerAId, String playerBId, CardType type, CardRarity rarity, int count) {
        List<CardDefinition> candidates = cardCatalogService.listAll().stream()
                .filter(definition -> definition.getType() == type)
                .filter(definition -> definition.getRarity() == rarity)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("缺少牌库候选: " + type + " / " + rarity);
        }

        int copyLimit = rarity == CardRarity.RARE ? RARE_COPY_LIMIT : COMMON_COPY_LIMIT;
        if (candidates.size() * copyLimit < count) {
            throw new IllegalStateException("牌库候选不足: " + type + " / " + rarity + ", 需要 " + count + " 张.");
        }

        List<CardDefinition> pool = new ArrayList<>();
        for (CardDefinition definition : candidates) {
            for (int copy = 0; copy < copyLimit; copy += 1) {
                pool.add(definition);
            }
        }
        Collections.shuffle(pool);

        Map<String, Long> ownerCounts = pool.stream()
                .limit(count)
                .collect(Collectors.groupingBy(CardDefinition::getId, Collectors.counting()));

        List<CardInstance> cards = new ArrayList<>();
        boolean ownerA = true;
        for (Map.Entry<String, Long> entry : ownerCounts.entrySet()) {
            CardDefinition definition = cardCatalogService.require(entry.getKey());
            for (int copy = 0; copy < entry.getValue(); copy += 1) {
                cards.add(createCard(definition, ownerA ? playerAId : playerBId));
                ownerA = !ownerA;
            }
        }
        return cards;
    }

    private CardInstance createCard(CardDefinition definition, String ownerId) {
        GameCard gameCard = cardCatalogService.requireCard(definition.getId());
        CardInstance card = new CardInstance(definition.getId(), ownerId, defaultHealth(definition));
        card.setExtraLives(gameCard.extraLives());
        return card;
    }

}
