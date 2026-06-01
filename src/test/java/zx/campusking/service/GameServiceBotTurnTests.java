package zx.campusking.service;

import org.junit.jupiter.api.Test;
import zx.campusking.model.BotMode;
import zx.campusking.model.CardInstance;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.websocket.GameWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceBotTurnTests {

    @Test
    void endingTurnInBotModeHandlesSingleTargetSkillWithoutThrowing() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = match.getPlayers().stream()
                .filter(player -> "P1".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();
        PlayerState bot = match.getPlayers().stream()
                .filter(player -> "P2".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();

        bot.getHand().clear();
        bot.getBoard().clear();
        human.getBoard().clear();

        CardInstance target = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(target);
        bot.getHand().add(new CardInstance("soda", bot.getPlayerId(), 0));

        match.setMode(BotMode.PVE);
        match.setReady(true);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setTurn(1);
        match.setDrawPile(new ArrayList<>());

        MatchState result = assertDoesNotThrow(() -> service.endTurn(match.getMatchId(), human.getPlayerId()));

        assertNotNull(result);
        assertEquals("P1", result.getCurrentPlayerId());
        assertEquals(GamePhase.DRAW, result.getPhase());
        assertEquals(3, result.getTurn());
        assertTrue(result.getDiscardPile().stream().anyMatch(card -> "soda".equals(card.getCardId())));
        assertTrue(target.getCurrentHealth() < 100);
    }

    private GameService createGameService() throws IOException {
        CardCatalogService cardCatalogService = new CardCatalogService();
        MatchSupportService matchSupportService = new MatchSupportService();
        DeckService deckService = new DeckService(cardCatalogService);
        StatusEffectService statusEffectService = new StatusEffectService(cardCatalogService);
        BattleService battleService = new BattleService(cardCatalogService, statusEffectService);
        SkillResolverService skillResolverService = new SkillResolverService(
                cardCatalogService,
                statusEffectService,
                matchSupportService,
                battleService,
                deckService
        );
        return new GameService(
                cardCatalogService,
                new GameWebSocketHandler(),
                matchSupportService,
                deckService,
                statusEffectService,
                battleService,
                skillResolverService
        );
    }
}
