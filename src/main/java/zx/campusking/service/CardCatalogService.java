package zx.campusking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Service;
import zx.campusking.cards.GameCard;
import zx.campusking.config.WebuiConfigFile;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardRegistry;
import zx.campusking.model.CardType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardCatalogService {

    private static final Path REGISTRY_PATH = Path.of("src", "main", "java", "zx", "campusking", "cards", "cardRegistry.json");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final List<CardBinding> cardBindings;
    private final Map<String, CardBinding> bindingIndex;
    private List<CardDefinition> cards;
    private Map<String, CardDefinition> index;
    private List<GameCard> gameCards;
    private Map<String, GameCard> cardIndex;

    public CardCatalogService() {
        String cardsPackage = WebuiConfigFile.loadCardDefaults().cardsPackage();
        this.cardBindings = scanCards(cardsPackage);
        this.bindingIndex = cardBindings.stream().collect(Collectors.toMap(CardBinding::id, binding -> binding));
        reloadRegistry();
    }

    public synchronized List<CardDefinition> listAll() {
        return cards;
    }

    public synchronized CardDefinition require(String cardId) {
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

    public synchronized List<CardDefinition> saveRegistry(List<CardDefinition> incomingCards) {
        if (incomingCards == null || incomingCards.isEmpty()) {
            throw new IllegalArgumentException("卡牌配置不能为空");
        }
        List<CardDefinition> normalized = normalizeRegistry(toRegistryMap(incomingCards));
        writeRegistry(normalized);
        loadFrom(normalized);
        return cards;
    }

    private synchronized void reloadRegistry() {
        List<CardDefinition> registryCards = readRegistry();
        List<CardDefinition> normalized = normalizeRegistry(toRegistryMap(registryCards));
        if (normalized.isEmpty()) {
            throw new IllegalStateException("卡牌注册表不能为空: " + REGISTRY_PATH);
        }
        writeRegistry(normalized);
        loadFrom(normalized);
    }

    private void loadFrom(List<CardDefinition> loadedCards) {
        this.cards = loadedCards.stream()
                .sorted(Comparator.comparingInt(this::orderOf).thenComparing(CardDefinition::getId))
                .toList();
        this.index = cards.stream().collect(Collectors.toMap(CardDefinition::getId, Function.identity()));
        this.gameCards = cards.stream()
                .map(this::instantiateConfigured)
                .sorted(Comparator.comparingInt(GameCard::order).thenComparing(card -> card.definition().getId()))
                .toList();
        this.cardIndex = gameCards.stream().collect(Collectors.toMap(card -> card.definition().getId(), Function.identity()));
    }

    private List<CardDefinition> normalizeRegistry(Map<String, CardDefinition> registry) {
        return registry.values().stream()
                .filter(definition -> bindingIndex.containsKey(definition.getId()))
                .map(this::normalizeDefinition)
                .sorted(Comparator.comparingInt(this::orderOf).thenComparing(CardDefinition::getId))
                .toList();
    }

    private CardDefinition normalizeDefinition(CardDefinition source) {
        CardDefinition definition = copyDefinition(source);
        definition.setName(nonBlankOrDefault(definition.getName(), definition.getId()));
        definition.setDescription(normalizePunctuation(definition.getDescription() == null ? "" : definition.getDescription()));
        definition.setActionCost(nonNegativeOrDefault(definition.getActionCost(), 1));
        if (definition.getRarity() == null) {
            throw new IllegalArgumentException("卡牌缺少稀有度: " + definition.getId());
        }
        if (definition.getType() == CardType.CHARACTER) {
            definition.setAttack(nonNegativeOrDefault(definition.getAttack(), 0));
            definition.setHealth(nonNegativeOrDefault(definition.getHealth(), 0));
            definition.setSecondaryAttack(nonNegativeOrDefault(definition.getSecondaryAttack(), null));
            definition.setSecondaryHealth(nonNegativeOrDefault(definition.getSecondaryHealth(), null));
        } else if (definition.getType() == CardType.SKILL) {
            definition.setEffectValue(nonNegativeOrDefault(definition.getEffectValue(), 0));
            definition.setEffectDuration(nonNegativeOrDefault(definition.getEffectDuration(), 0));
        } else {
            throw new IllegalArgumentException("卡牌缺少类型: " + definition.getId());
        }
        normalizeFields(definition);
        return definition;
    }

    private CardDefinition copyDefinition(CardDefinition source) {
        CardDefinition copy = new CardDefinition();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setType(source.getType());
        copy.setDescription(source.getDescription());
        copy.setActionCost(source.getActionCost());
        copy.setRarity(source.getRarity());
        copy.setAttack(source.getAttack());
        copy.setHealth(source.getHealth());
        copy.setEffectType(source.getEffectType());
        copy.setEffectCategory(source.getEffectCategory());
        copy.setEffectValue(source.getEffectValue());
        copy.setEffectDuration(source.getEffectDuration());
        copy.setSkillRange(source.getSkillRange());
        copy.setExclusive(source.getExclusive() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getExclusive()));
        return copy;
    }

    private Map<String, CardDefinition> toRegistryMap(List<CardDefinition> registryCards) {
        Map<String, CardDefinition> registry = new LinkedHashMap<>();
        for (CardDefinition definition : registryCards) {
            if (definition == null || definition.getId() == null || definition.getId().isBlank()) {
                continue;
            }
            if (!bindingIndex.containsKey(definition.getId())) {
                continue;
            }
            registry.put(definition.getId(), definition);
        }
        return registry;
    }

    private List<CardDefinition> readRegistry() {
        if (!Files.isRegularFile(REGISTRY_PATH)) {
            return List.of();
        }
        try {
            if (Files.size(REGISTRY_PATH) == 0) {
                return List.of();
            }
            CardRegistry registry = objectMapper.readValue(REGISTRY_PATH.toFile(), CardRegistry.class);
            if (registry == null) {
                return List.of();
            }
            return combinedRegistryCards(registry);
        } catch (IOException exception) {
            try {
                List<CardDefinition> list = objectMapper.readValue(REGISTRY_PATH.toFile(), new TypeReference<>() {});
                return list == null ? List.of() : list;
            } catch (IOException ignored) {
                try {
                    Map<String, CardDefinition> map = objectMapper.readValue(REGISTRY_PATH.toFile(), new TypeReference<>() {});
                    return map == null ? List.of() : map.values().stream().toList();
                } catch (IOException mapException) {
                    throw new UncheckedIOException("读取卡牌注册表失败", exception);
                }
            }
        }
    }

    private void writeRegistry(List<CardDefinition> definitions) {
        try {
            Files.createDirectories(REGISTRY_PATH.getParent());
            String nextContent = objectMapper.writeValueAsString(toRegistry(definitions)) + System.lineSeparator();
            if (Files.isRegularFile(REGISTRY_PATH)) {
                String currentContent = Files.readString(REGISTRY_PATH);
                if (currentContent.equals(nextContent)) {
                    return;
                }
            }
            Files.writeString(REGISTRY_PATH, nextContent);
        } catch (IOException exception) {
            throw new UncheckedIOException("保存卡牌注册表失败", exception);
        }
    }

    private void normalizeFields(CardDefinition definition) {
        if (definition.getType() == CardType.CHARACTER) {
            clearSkillFields(definition);
        } else if (definition.getType() == CardType.SKILL) {
            clearCharacterFields(definition);
        }
    }

    private CardRegistry toRegistry(List<CardDefinition> definitions) {
        CardRegistry registry = new CardRegistry();
        registry.setCharacters(definitions.stream()
                .filter(definition -> definition.getType() == CardType.CHARACTER)
                .map(this::registryDefinition)
                .toList());
        registry.setSkills(definitions.stream()
                .filter(definition -> definition.getType() == CardType.SKILL)
                .map(this::registryDefinition)
                .toList());
        return registry;
    }

    private CardDefinition registryDefinition(CardDefinition source) {
        CardDefinition definition = copyDefinition(source);
        if (definition.getType() == CardType.CHARACTER) {
            clearSkillFields(definition);
        } else if (definition.getType() == CardType.SKILL) {
            clearCharacterFields(definition);
        }
        return definition;
    }

    private List<CardDefinition> combinedRegistryCards(CardRegistry registry) {
        return java.util.stream.Stream.concat(
                        registry.getCharacters() == null ? java.util.stream.Stream.empty() : registry.getCharacters().stream(),
                        registry.getSkills() == null ? java.util.stream.Stream.empty() : registry.getSkills().stream()
                )
                .toList();
    }

    private void clearCharacterFields(CardDefinition definition) {
        definition.setAttack(null);
        definition.setHealth(null);
    }

    private void clearSkillFields(CardDefinition definition) {
        definition.setEffectType(null);
        definition.setEffectCategory(null);
        definition.setEffectValue(null);
        definition.setEffectDuration(null);
        definition.setSkillRange(null);
    }

    private int orderOf(CardDefinition definition) {
        CardBinding binding = bindingIndex.get(definition.getId());
        return binding == null ? 1000 : binding.order();
    }

    private String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer nonNegativeOrDefault(Integer value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, value);
    }

    private String normalizePunctuation(String value) {
        return value
                .replace("，", ", ")
                .replace("。", ".")
                .replace("；", "; ")
                .replace("：", ": ")
                .replace("！", "!")
                .replace("？", "?")
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .replaceAll("([,.;:!?])(?=\\S)", "$1 ");
    }

    private List<CardBinding> scanCards(String cardsPackage) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(GameCard.class));

        List<CardBinding> discovered = scanner.findCandidateComponents(cardsPackage).stream()
                .map(this::binding)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(CardBinding::order).thenComparing(CardBinding::id))
                .toList();

        if (discovered.isEmpty()) {
            throw new IllegalStateException("No cards found in package: " + cardsPackage);
        }
        return discovered;
    }

    private CardBinding binding(BeanDefinition beanDefinition) {
        try {
            String className = beanDefinition.getBeanClassName();
            if (className == null) {
                throw new IllegalStateException("Card class name is missing.");
            }
            Class<?> cardClass = Class.forName(className);
            if (cardClass.isInterface() || Modifier.isAbstract(cardClass.getModifiers())) {
                return null;
            }
            if (!GameCard.class.isAssignableFrom(cardClass)) {
                return null;
            }
            String id = (String) cardClass.getField("ID").get(null);
            int order = ((Number) cardClass.getField("ORDER").get(null)).intValue();
            return new CardBinding(id, order, cardClass.asSubclass(GameCard.class));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("卡牌类必须声明 public static final ID 和 ORDER: " + beanDefinition.getBeanClassName(), exception);
        }
    }

    private GameCard instantiateConfigured(CardDefinition definition) {
        CardBinding binding = bindingIndex.get(definition.getId());
        if (binding == null) {
            throw new IllegalArgumentException("Unknown card: " + definition.getId());
        }
        try {
            Class<? extends GameCard> cardClass = binding.cardClass();
            Constructor<? extends GameCard> constructor = cardClass.getDeclaredConstructor(CardDefinition.class);
            return constructor.newInstance(copyDefinition(definition));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load card: " + definition.getId(), exception);
        }
    }

    private record CardBinding(String id, int order, Class<? extends GameCard> cardClass) {
    }
}
