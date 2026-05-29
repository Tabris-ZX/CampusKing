package zx.campusking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardCatalogService {

    private final List<CardDefinition> cards;
    private final Map<String, CardDefinition> index;

    public CardCatalogService() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = new ClassPathResource("cards.json").getInputStream()) {
            this.cards = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
        this.index = cards.stream().collect(Collectors.toMap(CardDefinition::getId, Function.identity()));
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
}
