package zx.campusking.service;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Service;
import zx.campusking.cards.GameCard;
import zx.campusking.config.WebuiConfigFile;
import zx.campusking.model.CardDefinition;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardCatalogService {

    private final List<CardDefinition> cards;
    private final List<GameCard> gameCards;
    private final Map<String, CardDefinition> index;
    private final Map<String, GameCard> cardIndex;

    public CardCatalogService() {
        String cardsPackage = WebuiConfigFile.loadCardDefaults().cardsPackage();
        this.gameCards = scanCards(cardsPackage);
        this.cards = gameCards.stream().map(GameCard::definition).toList();
        this.index = cards.stream().collect(Collectors.toMap(CardDefinition::getId, Function.identity()));
        this.cardIndex = gameCards.stream().collect(Collectors.toMap(card -> card.definition().getId(), Function.identity()));
    }

    public List<CardDefinition> listAll() {
        return cards;
    }

    public CardDefinition require(String cardId) {
        CardDefinition definition = index.get(cardId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown card: " + cardId);
        }
        return definition;
    }

    public GameCard requireCard(String cardId) {
        GameCard card = cardIndex.get(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Unknown card: " + cardId);
        }
        return card;
    }

    private List<GameCard> scanCards(String cardsPackage) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(GameCard.class));

        List<GameCard> discovered = scanner.findCandidateComponents(cardsPackage).stream()
                .map(this::instantiate)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GameCard::order).thenComparing(card -> card.definition().getId()))
                .toList();

        if (discovered.isEmpty()) {
            throw new IllegalStateException("No cards found in package: " + cardsPackage);
        }
        return discovered;
    }

    private GameCard instantiate(BeanDefinition beanDefinition) {
        try {
            String className = beanDefinition.getBeanClassName();
            if (className == null) {
                throw new IllegalStateException("Card class name is missing.");
            }
            Class<?> cardClass = Class.forName(className);
            if (cardClass.isInterface() || Modifier.isAbstract(cardClass.getModifiers())) {
                return null;
            }
            return (GameCard) cardClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load card: " + beanDefinition.getBeanClassName(), exception);
        }
    }
}
