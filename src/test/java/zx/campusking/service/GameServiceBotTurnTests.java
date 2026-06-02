package zx.campusking.service;

import org.junit.jupiter.api.Test;
import zx.campusking.model.BotMode;
import zx.campusking.model.CardInstance;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.StatusEffectType;
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.model.dto.EndTurnRequest;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.model.dto.SummonRequest;
import zx.campusking.websocket.GameWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(GamePhase.ACTION, result.getPhase());
        assertEquals(3, result.getTurn());
        assertTrue(result.getLogs().stream().anyMatch(log -> log.contains("瓦库结束了回合")));
    }

    @Test
    void endingTurnDiscardsHandDownToLimit() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = match.getPlayers().stream()
                .filter(player -> "P1".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();

        human.getHand().clear();
        for (int index = 0; index < 8; index += 1) {
            human.getHand().add(new CardInstance("meal", human.getPlayerId(), 100));
        }
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setDrawPile(new ArrayList<>());

        EndTurnRequest endTurnRequest = new EndTurnRequest();
        endTurnRequest.setPlayerId(human.getPlayerId());
        endTurnRequest.setDiscardInstanceIds(List.of(
                human.getHand().get(0).getInstanceId(),
                human.getHand().get(1).getInstanceId()
        ));

        MatchState result = service.endTurn(match.getMatchId(), endTurnRequest);

        assertEquals(PlayerState.HAND_LIMIT, human.getHand().size());
        assertTrue(result.getLogs().stream().anyMatch(log -> log.contains("回合结束弃置了 2 张手牌")));
    }

    @Test
    void summonConsumesActionPointAndRequiresEnoughPoints() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = match.getPlayers().stream()
                .filter(player -> "P1".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();

        human.getHand().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getHand().add(meal);
        human.setActionPoints(PlayerState.MAX_ACTION_POINTS);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        SummonRequest summonRequest = new SummonRequest();
        summonRequest.setPlayerId(human.getPlayerId());
        summonRequest.setHandInstanceId(meal.getInstanceId());

        service.summon(match.getMatchId(), summonRequest);

        assertEquals(PlayerState.MAX_ACTION_POINTS - 1, human.getActionPoints());

        human.getHand().add(new CardInstance("meal", human.getPlayerId(), 100));
        human.setSummonsThisTurn(0);
        human.setActionPoints(0);
        summonRequest.setHandInstanceId(human.getHand().get(0).getInstanceId());

        assertThrows(IllegalStateException.class, () -> service.summon(match.getMatchId(), summonRequest));
    }

    @Test
    void rareDefeatGrantsCardAndActionPoint() throws IOException {
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

        human.getHand().clear();
        human.getHand().add(new CardInstance("bunch", human.getPlayerId(), 0));
        human.setActionPoints(3);
        bot.getBoard().clear();
        bot.getBoard().add(new CardInstance("sniper", bot.getPlayerId(), 10));
        match.setDrawPile(new ArrayList<>(List.of(new CardInstance("meal", human.getPlayerId(), 100))));
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(human.getHand().get(0).getInstanceId());

        service.playSkill(match.getMatchId(), playEffectRequest);

        assertEquals(1, human.getHand().size());
        assertEquals(3, human.getActionPoints());
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("击败稀有角色, 获得了 1 点行动点")));
    }

    @Test
    void foresightCostsNoActionPointAndDamagesPlayer() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = match.getPlayers().stream()
                .filter(player -> "P1".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();

        human.getHand().clear();
        human.getHand().add(new CardInstance("foresight", human.getPlayerId(), 0));
        human.setActionPoints(0);
        human.setHp(100);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(human.getHand().get(0).getInstanceId());

        service.playSkill(match.getMatchId(), playEffectRequest);

        assertEquals(1, human.getActionPoints());
        assertEquals(95, human.getHp());
    }

    @Test
    void invalidSodaTargetDoesNotPartiallyResolveSkill() throws IOException {
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

        human.getHand().clear();
        CardInstance soda = new CardInstance("soda", human.getPlayerId(), 0);
        human.getHand().add(soda);
        human.setActionPoints(3);
        bot.getBoard().clear();
        match.setDrawPile(new ArrayList<>(List.of(
                new CardInstance("meal", human.getPlayerId(), 100),
                new CardInstance("sim", human.getPlayerId(), 70)
        )));
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(soda.getInstanceId());
        playEffectRequest.setTargetPlayerId(bot.getPlayerId());

        assertThrows(IllegalStateException.class, () -> service.playSkill(match.getMatchId(), playEffectRequest));
        assertEquals(1, human.getHand().size());
        assertEquals(soda.getInstanceId(), human.getHand().get(0).getInstanceId());
        assertEquals(3, human.getActionPoints());
        assertEquals(2, match.getDrawPile().size());
        assertTrue(match.getDiscardPile().stream().noneMatch(card -> card.getInstanceId().equals(soda.getInstanceId())));
    }

    @Test
    void repeatedCharacterBuffStacksOnSameBoardCard() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = match.getPlayers().stream()
                .filter(player -> "P1".equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();

        human.getHand().clear();
        human.getHand().add(new CardInstance("sanctuary", human.getPlayerId(), 0));
        human.getHand().add(new CardInstance("sanctuary", human.getPlayerId(), 0));
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(meal);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest firstRequest = new PlayEffectRequest();
        firstRequest.setPlayerId(human.getPlayerId());
        firstRequest.setHandInstanceId(human.getHand().get(0).getInstanceId());
        service.playSkill(match.getMatchId(), firstRequest);

        PlayEffectRequest secondRequest = new PlayEffectRequest();
        secondRequest.setPlayerId(human.getPlayerId());
        secondRequest.setHandInstanceId(human.getHand().get(0).getInstanceId());
        service.playSkill(match.getMatchId(), secondRequest);

        assertTrue(meal.getStatusEffects().stream()
                .anyMatch(effect -> effect.getType() == StatusEffectType.ATTACK_UP && effect.getValue() == 10 && effect.getStacks() == 2));
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
