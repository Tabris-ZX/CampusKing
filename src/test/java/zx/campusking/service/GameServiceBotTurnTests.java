package zx.campusking.service;

import org.junit.jupiter.api.Test;
import zx.campusking.model.BotMode;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardRarity;
import zx.campusking.model.CardType;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchPlayType;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.StatusEffectType;
import zx.campusking.model.dto.AttackCharacterRequest;
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.model.dto.EndTurnRequest;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.model.dto.SacrificeRequest;
import zx.campusking.model.dto.SummonRequest;
import zx.campusking.websocket.GameWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceBotTurnTests {

    @Test
    void deckUsesConfiguredSingleSideDistribution() throws IOException {
        CardCatalogService cardCatalogService = new CardCatalogService();
        DeckService deckService = new DeckService(cardCatalogService);

        List<CardInstance> deck = deckService.buildDeck("P1", "P2");

        assertEquals(30, deck.size());
        assertEquals(8, countCards(deck, cardCatalogService, CardType.CHARACTER, CardRarity.COMMON));
        assertEquals(4, countCards(deck, cardCatalogService, CardType.CHARACTER, CardRarity.RARE));
        assertEquals(12, countCards(deck, cardCatalogService, CardType.SKILL, CardRarity.COMMON));
        assertEquals(6, countCards(deck, cardCatalogService, CardType.SKILL, CardRarity.RARE));

        Map<String, Long> copies = deck.stream()
                .map(CardInstance::getCardId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        for (Map.Entry<String, Long> entry : copies.entrySet()) {
            CardDefinition definition = cardCatalogService.require(entry.getKey());
            int copyLimit = definition.getRarity() == CardRarity.RARE ? 2 : 3;
            assertTrue(entry.getValue() <= copyLimit);
        }
    }

    @Test
    void createRoomStoresSingleSidePlayTypeAndInitializesBotMatch() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");
        request.setPlayType("SINGLE_SIDE");

        MatchState match = service.createRoom(request);

        assertEquals(MatchPlayType.SINGLE_SIDE, match.getPlayType());
        assertEquals(2, match.getPlayers().size());
        assertTrue(match.isReady());
        assertEquals(GamePhase.ACTION, match.getPhase());
    }

    @Test
    void firstPlayerDrawsOneExtraCardOnFirstTurn() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState firstPlayer = requirePlayer(match, "P1");
        PlayerState secondPlayer = requirePlayer(match, "P2");

        assertEquals(5, firstPlayer.getHand().size());
        assertEquals(2, secondPlayer.getHand().size());
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("自动抽了 3 张牌")));
    }

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
    void botAttacksWhenReadyBoardCharacterExists() throws IOException {
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

        human.getBoard().clear();
        CardInstance defender = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(defender);
        bot.getBoard().clear();
        CardInstance attacker = new CardInstance("sim", bot.getPlayerId(), 70);
        attacker.setSleeping(false);
        bot.getBoard().add(attacker);
        bot.getHand().clear();
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setTurn(2);
        match.setDrawPile(new ArrayList<>());

        MatchState result = service.endTurn(match.getMatchId(), human.getPlayerId());

        assertEquals(70, defender.getCurrentHealth());
        assertTrue(attacker.isSleeping());
        assertEquals("P1", result.getCurrentPlayerId());
        assertTrue(result.getLogs().stream().anyMatch(log -> log.contains("瓦库使用 电话卡 攻击了 饭卡")));
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
        match.setMode(BotMode.PVP);
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
    void foresightCostsNoActionPointAndMakesPlayerLoseHealth() throws IOException {
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
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("失去了 5 点体力")));
    }

    @Test
    void chipsDiscardsAllRemainingHandAndDrawsOneMoreThanDiscarded() throws IOException {
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
        CardInstance chips = new CardInstance("chips", human.getPlayerId(), 0);
        human.getHand().add(chips);
        human.getHand().add(new CardInstance("meal", human.getPlayerId(), 100));
        human.getHand().add(new CardInstance("sim", human.getPlayerId(), 70));
        human.setActionPoints(3);
        match.setDrawPile(new ArrayList<>(List.of(
                new CardInstance("meal", human.getPlayerId(), 100),
                new CardInstance("sim", human.getPlayerId(), 70),
                new CardInstance("sniper", human.getPlayerId(), 30)
        )));
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(chips.getInstanceId());

        service.playSkill(match.getMatchId(), playEffectRequest);

        assertEquals(3, human.getHand().size());
        assertEquals(3, match.getDiscardPile().size());
        assertTrue(match.getDiscardPile().stream().anyMatch(card -> "chips".equals(card.getCardId())));
        assertEquals(0, match.getDrawPile().size());
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

    @Test
    void elfRevivesNextFriendlyCharacterDefeatedByAttack() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance elf = new CardInstance("elf", human.getPlayerId(), 0);
        human.getHand().add(elf);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 20);
        human.getBoard().add(meal);
        bot.getBoard().clear();
        CardInstance attacker = new CardInstance("sniper", bot.getPlayerId(), 30);
        attacker.setSleeping(false);
        bot.getBoard().add(attacker);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setTurn(2);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(elf.getInstanceId());
        service.playSkill(match.getMatchId(), playEffectRequest);

        assertTrue(human.getStatusEffects().stream()
                .anyMatch(effect -> effect.getType() == StatusEffectType.REVIVE_ON_DEATH && effect.getRemainingTurns() == 3));

        match.setCurrentPlayerId(bot.getPlayerId());
        AttackCharacterRequest attackRequest = new AttackCharacterRequest();
        attackRequest.setPlayerId(bot.getPlayerId());
        attackRequest.setAttackerInstanceId(attacker.getInstanceId());
        attackRequest.setDefenderInstanceId(meal.getInstanceId());
        service.attackCharacter(match.getMatchId(), attackRequest);

        assertTrue(human.getBoard().stream().anyMatch(card -> card.getInstanceId().equals(meal.getInstanceId())));
        assertEquals(20, meal.getCurrentHealth());
        assertTrue(human.getStatusEffects().stream().noneMatch(effect -> effect.getType() == StatusEffectType.REVIVE_ON_DEATH));
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("死亡后回复到 20 点体力")));
    }

    @Test
    void sacrificeDoesNotTriggerOrConsumeElfRevive() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");

        human.getHand().clear();
        CardInstance elf = new CardInstance("elf", human.getPlayerId(), 0);
        human.getHand().add(elf);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(meal);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest elfRequest = new PlayEffectRequest();
        elfRequest.setPlayerId(human.getPlayerId());
        elfRequest.setHandInstanceId(elf.getInstanceId());
        service.playSkill(match.getMatchId(), elfRequest);

        SacrificeRequest sacrificeRequest = new SacrificeRequest();
        sacrificeRequest.setPlayerId(human.getPlayerId());
        sacrificeRequest.setTargetInstanceId(meal.getInstanceId());
        service.sacrifice(match.getMatchId(), sacrificeRequest);

        assertTrue(human.getBoard().stream().noneMatch(card -> card.getInstanceId().equals(meal.getInstanceId())));
        assertTrue(match.getDiscardPile().stream().anyMatch(card -> card.getInstanceId().equals(meal.getInstanceId())));
        assertTrue(human.getStatusEffects().stream()
                .anyMatch(effect -> effect.getType() == StatusEffectType.REVIVE_ON_DEATH && effect.getStacks() == 1));
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("失去全部体力并进入墓地")));
    }

    @Test
    void sacrificeGrantsOneExtraSummonThisTurn() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");

        human.getHand().clear();
        CardInstance boardMeal = new CardInstance("meal", human.getPlayerId(), 100);
        CardInstance secondMeal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getHand().add(secondMeal);
        human.getBoard().clear();
        human.getBoard().add(boardMeal);
        human.setSummonsThisTurn(1);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        SacrificeRequest sacrificeRequest = new SacrificeRequest();
        sacrificeRequest.setPlayerId(human.getPlayerId());
        sacrificeRequest.setTargetInstanceId(boardMeal.getInstanceId());
        service.sacrifice(match.getMatchId(), sacrificeRequest);
        assertEquals(0, human.getSummonsThisTurn());

        SummonRequest secondSummon = new SummonRequest();
        secondSummon.setPlayerId(human.getPlayerId());
        secondSummon.setHandInstanceId(secondMeal.getInstanceId());
        service.summon(match.getMatchId(), secondSummon);

        assertTrue(human.getBoard().stream().anyMatch(card -> card.getInstanceId().equals(secondMeal.getInstanceId())));
        assertEquals(1, human.getSummonsThisTurn());
    }

    @Test
    void lightPreventsNextCharacterAttack() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance light = new CardInstance("light", human.getPlayerId(), 0);
        human.getHand().add(light);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(meal);
        bot.getBoard().clear();
        CardInstance attacker = new CardInstance("sniper", bot.getPlayerId(), 30);
        attacker.setSleeping(false);
        bot.getBoard().add(attacker);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setTurn(2);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(light.getInstanceId());
        service.playSkill(match.getMatchId(), playEffectRequest);

        match.setCurrentPlayerId(bot.getPlayerId());
        AttackCharacterRequest attackRequest = new AttackCharacterRequest();
        attackRequest.setPlayerId(bot.getPlayerId());
        attackRequest.setAttackerInstanceId(attacker.getInstanceId());
        attackRequest.setDefenderInstanceId(meal.getInstanceId());
        service.attackCharacter(match.getMatchId(), attackRequest);

        assertEquals(100, meal.getCurrentHealth());
        assertTrue(human.getStatusEffects().stream().noneMatch(effect -> effect.getType() == StatusEffectType.PREVENT_NEXT_ACTION));
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("抵御了") && log.contains("的攻击")));
    }

    @Test
    void umbrellaPreventsNextEnemySkillCard() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance umbrella = new CardInstance("umbrella", human.getPlayerId(), 0);
        human.getHand().add(umbrella);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 100);
        human.getBoard().add(meal);
        bot.getHand().clear();
        CardInstance bunch = new CardInstance("bunch", bot.getPlayerId(), 0);
        bot.getHand().add(bunch);
        human.setActionPoints(3);
        bot.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest umbrellaRequest = new PlayEffectRequest();
        umbrellaRequest.setPlayerId(human.getPlayerId());
        umbrellaRequest.setHandInstanceId(umbrella.getInstanceId());
        service.playSkill(match.getMatchId(), umbrellaRequest);

        match.setCurrentPlayerId(bot.getPlayerId());
        PlayEffectRequest bunchRequest = new PlayEffectRequest();
        bunchRequest.setPlayerId(bot.getPlayerId());
        bunchRequest.setHandInstanceId(bunch.getInstanceId());
        service.playSkill(match.getMatchId(), bunchRequest);

        assertEquals(100, meal.getCurrentHealth());
        assertTrue(human.getStatusEffects().stream().noneMatch(effect -> effect.getType() == StatusEffectType.PREVENT_NEXT_ACTION));
        assertTrue(match.getDiscardPile().stream().anyMatch(card -> card.getInstanceId().equals(bunch.getInstanceId())));
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("抵御了 普通一拳")));
    }

    @Test
    void elfRevivesFriendlyCharacterDefeatedBySkillDamage() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance elf = new CardInstance("elf", human.getPlayerId(), 0);
        human.getHand().add(elf);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 10);
        human.getBoard().add(meal);
        bot.getHand().clear();
        CardInstance bunch = new CardInstance("bunch", bot.getPlayerId(), 0);
        bot.getHand().add(bunch);
        human.setActionPoints(3);
        bot.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest elfRequest = new PlayEffectRequest();
        elfRequest.setPlayerId(human.getPlayerId());
        elfRequest.setHandInstanceId(elf.getInstanceId());
        service.playSkill(match.getMatchId(), elfRequest);

        match.setCurrentPlayerId(bot.getPlayerId());
        PlayEffectRequest bunchRequest = new PlayEffectRequest();
        bunchRequest.setPlayerId(bot.getPlayerId());
        bunchRequest.setHandInstanceId(bunch.getInstanceId());
        service.playSkill(match.getMatchId(), bunchRequest);

        assertTrue(human.getBoard().stream().anyMatch(card -> card.getInstanceId().equals(meal.getInstanceId())));
        assertEquals(20, meal.getCurrentHealth());
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("死亡后回复到 20 点体力")));
    }

    @Test
    void sanctuaryBuffsBoardCardsAndHealsAtTurnStart() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance sanctuary = new CardInstance("sanctuary", human.getPlayerId(), 0);
        human.getHand().add(sanctuary);
        human.getBoard().clear();
        CardInstance meal = new CardInstance("meal", human.getPlayerId(), 80);
        human.getBoard().add(meal);
        human.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);

        PlayEffectRequest playEffectRequest = new PlayEffectRequest();
        playEffectRequest.setPlayerId(human.getPlayerId());
        playEffectRequest.setHandInstanceId(sanctuary.getInstanceId());
        service.playSkill(match.getMatchId(), playEffectRequest);

        assertTrue(meal.getStatusEffects().stream().anyMatch(effect -> effect.getType() == StatusEffectType.ATTACK_UP && effect.getValue() == 10));
        assertTrue(meal.getStatusEffects().stream().anyMatch(effect -> effect.getType() == StatusEffectType.MAX_HP_UP && effect.getValue() == 10));
        assertTrue(meal.getStatusEffects().stream().anyMatch(effect -> effect.getType() == StatusEffectType.TURN_HEAL && effect.getValue() == 10));
        assertEquals(80, meal.getCurrentHealth());

        match.setCurrentPlayerId(bot.getPlayerId());
        EndTurnRequest endTurnRequest = new EndTurnRequest();
        endTurnRequest.setPlayerId(bot.getPlayerId());
        service.endTurn(match.getMatchId(), endTurnRequest);

        assertEquals(90, meal.getCurrentHealth());
    }

    @Test
    void rupanDiscardsRandomEnemyHandAfterAttackDamage() throws IOException {
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

        human.getBoard().clear();
        bot.getBoard().clear();
        bot.getHand().clear();

        CardInstance rupan = new CardInstance("rupan", human.getPlayerId(), 50);
        rupan.setSleeping(false);
        human.getBoard().add(rupan);
        CardInstance defender = new CardInstance("meal", bot.getPlayerId(), 100);
        bot.getBoard().add(defender);
        bot.getHand().add(new CardInstance("sim", bot.getPlayerId(), 70));
        bot.getHand().add(new CardInstance("sniper", bot.getPlayerId(), 30));

        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setTurn(2);

        AttackCharacterRequest attackRequest = new AttackCharacterRequest();
        attackRequest.setPlayerId(human.getPlayerId());
        attackRequest.setAttackerInstanceId(rupan.getInstanceId());
        attackRequest.setDefenderInstanceId(defender.getInstanceId());

        service.attackCharacter(match.getMatchId(), attackRequest);

        assertEquals(1, bot.getHand().size());
        assertEquals(60, defender.getCurrentHealth());
        assertEquals(1, match.getDiscardPile().stream()
                .filter(card -> bot.getPlayerId().equals(card.getOwnerId()))
                .filter(card -> "sim".equals(card.getCardId()) || "sniper".equals(card.getCardId()))
                .count());
        assertTrue(match.getLogs().stream().anyMatch(log -> log.contains("触发特性") && log.contains("弃置对方 1 张随机手牌")));
    }

    @Test
    void rupanAttackTriggersDefeatCleanupBeforeDiscardTrait() throws IOException {
        GameService service = createGameService();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBotMode(true);
        request.setHostName("测试玩家");

        MatchState match = service.createRoom(request);
        PlayerState human = requirePlayer(match, "P1");
        PlayerState bot = requirePlayer(match, "P2");

        human.getHand().clear();
        CardInstance elf = new CardInstance("elf", human.getPlayerId(), 0);
        human.getHand().add(elf);
        human.getBoard().clear();
        CardInstance defender = new CardInstance("meal", human.getPlayerId(), 10);
        human.getBoard().add(defender);
        bot.getBoard().clear();
        CardInstance rupan = new CardInstance("rupan", bot.getPlayerId(), 50);
        rupan.setSleeping(false);
        bot.getBoard().add(rupan);
        human.getHand().add(new CardInstance("sim", human.getPlayerId(), 70));
        human.setActionPoints(3);
        bot.setActionPoints(3);
        match.setCurrentPlayerId(human.getPlayerId());
        match.setPhase(GamePhase.ACTION);
        match.setReady(true);
        match.setTurn(2);

        PlayEffectRequest elfRequest = new PlayEffectRequest();
        elfRequest.setPlayerId(human.getPlayerId());
        elfRequest.setHandInstanceId(elf.getInstanceId());
        service.playSkill(match.getMatchId(), elfRequest);

        match.setCurrentPlayerId(bot.getPlayerId());
        AttackCharacterRequest attackRequest = new AttackCharacterRequest();
        attackRequest.setPlayerId(bot.getPlayerId());
        attackRequest.setAttackerInstanceId(rupan.getInstanceId());
        attackRequest.setDefenderInstanceId(defender.getInstanceId());
        service.attackCharacter(match.getMatchId(), attackRequest);

        assertEquals(20, defender.getCurrentHealth());
        int reviveIndex = indexOfLogContaining(match, "死亡后回复到 20 点体力");
        int rupanIndex = indexOfLogContaining(match, "触发特性");
        int attackIndex = indexOfLogContaining(match, "攻击了 饭卡");
        assertTrue(reviveIndex >= 0);
        assertTrue(rupanIndex > reviveIndex);
        assertTrue(attackIndex > rupanIndex);
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
        MatchInitializerService matchInitializerService = new MatchInitializerService(deckService, statusEffectService);
        return new GameService(
                cardCatalogService,
                new GameWebSocketHandler(),
                matchSupportService,
                deckService,
                statusEffectService,
                battleService,
                skillResolverService,
                matchInitializerService,
                0L
        );
    }

    private PlayerState requirePlayer(MatchState match, String playerId) {
        return match.getPlayers().stream()
                .filter(player -> playerId.equals(player.getPlayerId()))
                .findFirst()
                .orElseThrow();
    }

    private int indexOfLogContaining(MatchState match, String value) {
        for (int index = 0; index < match.getLogs().size(); index += 1) {
            if (match.getLogs().get(index).contains(value)) {
                return index;
            }
        }
        return -1;
    }

    private long countCards(List<CardInstance> deck, CardCatalogService cardCatalogService, CardType type, CardRarity rarity) {
        return deck.stream()
                .map(card -> cardCatalogService.require(card.getCardId()))
                .filter(definition -> definition.getType() == type)
                .filter(definition -> definition.getRarity() == rarity)
                .count();
    }
}
