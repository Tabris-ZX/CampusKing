package zx.campusking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import zx.campusking.model.BotMode;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardType;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.dto.AttackCharacterRequest;
import zx.campusking.model.dto.AttackPlayerRequest;
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.model.dto.EndTurnRequest;
import zx.campusking.model.dto.JoinRoomRequest;
import zx.campusking.model.dto.LeaveRoomRequest;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.model.dto.RestoreSessionResponse;
import zx.campusking.model.dto.SacrificeRequest;
import zx.campusking.model.dto.SummonRequest;
import zx.campusking.websocket.GameWebSocketHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 对局编排服务。
 * 这一层不直接承担全部规则，而是组合房间、牌堆、战斗、状态和技能服务，
 * 作为 controller 层和各专用服务之间的总调度入口。
 */
@Service
public class GameService {

    /** 卡牌静态目录服务。 */
    private final CardCatalogService cardCatalogService;
    /** WebSocket 广播器，用于把最新对局快照推送给房间内客户端。 */
    private final GameWebSocketHandler gameWebSocketHandler;
    /** 对局基础辅助服务。 */
    private final MatchSupportService matchSupportService;
    /** 牌堆与抽牌服务。 */
    private final DeckService deckService;
    /** 状态效果服务。 */
    private final StatusEffectService statusEffectService;
    /** 战斗结算服务。 */
    private final BattleService battleService;
    /** 技能结算服务。 */
    private final SkillResolverService skillResolverService;
    /** 用于广播对局快照。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 当前所有对局，键为 matchId。 */
    private final Map<String, MatchState> matches = new LinkedHashMap<>();
    /** 房间码到 matchId 的映射。 */
    private final Map<String, String> roomIndex = new LinkedHashMap<>();

    public GameService(
            CardCatalogService cardCatalogService,
            GameWebSocketHandler gameWebSocketHandler,
            MatchSupportService matchSupportService,
            DeckService deckService,
            StatusEffectService statusEffectService,
            BattleService battleService,
            SkillResolverService skillResolverService
    ) {
        this.cardCatalogService = cardCatalogService;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.matchSupportService = matchSupportService;
        this.deckService = deckService;
        this.statusEffectService = statusEffectService;
        this.battleService = battleService;
        this.skillResolverService = skillResolverService;
    }

    /**
     * 创建房间。
     * 可创建普通双人房间，也可创建最小可用的人机调试房间。
     */
    public MatchState createRoom(CreateRoomRequest request) {
        MatchState match = new MatchState();
        match.setMatchId(UUID.randomUUID().toString());
        match.setRoomCode(generateRoomCode());
        match.setMode(Boolean.TRUE.equals(request.getBotMode()) ? BotMode.PVE : BotMode.PVP);
        match.setPlayers(new ArrayList<>(List.of(
                new PlayerState("P1", matchSupportService.defaultName(request.getHostName(), "房主"), normalizeOrCreateToken(request.getPlayerToken()))
        )));
        match.setPhase(GamePhase.DRAW);
        match.setCurrentPlayerId("P1");
        match.setTurn(1);
        match.setReady(false);

        if (Boolean.TRUE.equals(request.getBotMode())) {
            match.getLogs().add("人机房间已创建。");
            addBotOpponent(match);
        } else {
            match.getLogs().add("房间已创建，等待第二位玩家加入。");
        }

        matches.put(match.getMatchId(), match);
        roomIndex.put(match.getRoomCode(), match.getMatchId());
        broadcastMatch(match);
        return match;
    }

    /**
     * 玩家通过房间码加入。
     * 这里必须严格区分“房间不存在”和“房间已满”。
     */
    public MatchState joinRoom(String roomCode, JoinRoomRequest request) {
        MatchState match = getMatchByRoomCode(roomCode);
        if (match.getPlayers().size() >= 2) {
            throw new IllegalStateException("房间已满，无法加入。");
        }

        PlayerState playerB = new PlayerState("P2", matchSupportService.defaultName(request.getPlayerName(), "玩家B"), normalizeOrCreateToken(request.getPlayerToken()));
        match.getPlayers().add(playerB);

        List<CardInstance> deck = deckService.buildDeck(match.getPlayers().get(0).getPlayerId(), playerB.getPlayerId());
        Collections.shuffle(deck);
        match.setDrawPile(deck);
        match.setReady(true);
        match.getLogs().add(playerB.getName() + " 已加入房间。");

        for (int i = 0; i < 2; i++) {
            deckService.drawOne(match, match.getPlayers().get(0));
            deckService.drawOne(match, playerB);
        }
        startTurn(match, match.getPlayers().get(0));

        broadcastMatch(match);
        return match;
    }

    public void leaveRoom(String roomCode, LeaveRoomRequest request) {
        if (request.getPlayerToken() == null || request.getPlayerToken().isBlank()) {
            throw new IllegalArgumentException("缺少玩家会话标识。");
        }

        MatchState match = getMatchByRoomCode(roomCode);
        PlayerState leavingPlayer = match.getPlayers().stream()
                .filter(player -> request.getPlayerToken().equals(player.getPlayerToken()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("该房间中不存在当前浏览器会话。"));

        match.getPlayers().removeIf(player -> player.getPlayerId().equals(leavingPlayer.getPlayerId()));
        match.getLogs().add(leavingPlayer.getName() + " 退出了房间。");

        if (match.getPlayers().isEmpty()) {
            removeMatch(match);
            return;
        }

        if (match.getPhase() != GamePhase.FINISHED) {
            PlayerState remainingPlayer = match.getPlayers().get(0);
            match.setWinnerId(remainingPlayer.getPlayerId());
            match.setPhase(GamePhase.FINISHED);
            match.setReady(false);
            match.getLogs().add(remainingPlayer.getName() + " 因对手离开获胜。");
        }

        broadcastMatch(match);
    }

    /**
     * 浏览器刷新恢复。
     * 前端持久化 roomCode + playerToken，再由后端换回 playerId。
     */
    public RestoreSessionResponse restoreRoomSession(String roomCode, String playerToken) {
        MatchState match = getMatchByRoomCode(roomCode);
        PlayerState player = match.getPlayers().stream()
                .filter(candidate -> candidate.getPlayerToken().equals(playerToken))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("该房间中不存在当前浏览器会话。"));
        return new RestoreSessionResponse(match, player.getPlayerId());
    }

    /**
     * 根据对局 id 获取对局。
     */
    public MatchState getMatch(String matchId) {
        MatchState match = matches.get(matchId);
        if (match == null) {
            throw new NoSuchElementException("对局不存在: " + matchId);
        }
        return match;
    }

    /**
     * 根据房间码获取当前房间对应的对局。
     */
    public MatchState getMatchByRoomCode(String roomCode) {
        String matchId = roomIndex.get(roomCode);
        if (matchId == null) {
            throw new NoSuchElementException("房间不存在: " + roomCode);
        }
        return getMatch(matchId);
    }

    /**
     * 玩家自己的抽牌阶段开始时，先抽牌，再结算回合开始效果。
     */
    public MatchState drawPhase(String matchId, String playerId) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, playerId);
        matchSupportService.ensureNotFinished(match);
        matchSupportService.ensureReady(match);
        matchSupportService.ensurePhase(match, GamePhase.DRAW);

        startTurn(match, player);

        broadcastMatch(match);
        return match;
    }

    /**
     * 召唤角色到场上。
     * 当前规则：每回合最多召唤 1 个角色。
     */
    public MatchState summon(String matchId, SummonRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        matchSupportService.ensureReady(match);
        matchSupportService.ensureActionPhase(match);

        if (player.getBoard().size() >= PlayerState.SUMMON_SLOTS) {
            throw new IllegalStateException("召唤区已满。");
        }
        if (player.getSummonsThisTurn() >= 1) {
            throw new IllegalStateException("每回合最多召唤 1 个角色");
        }

        CardInstance card = matchSupportService.removeFromHand(player, request.getHandInstanceId());
        CardDefinition definition = cardCatalogService.require(card.getCardId());
        if (definition.getType() != CardType.CHARACTER) {
            throw new IllegalStateException("只有角色牌可以召唤。");
        }

        // 召唤后的休整规则由具体卡牌类提供，避免主流程按卡牌 id 分支。
        card.setSleeping(cardCatalogService.requireCard(definition.getId()).sleepsOnSummon());
        player.getBoard().add(card);
        player.setSummonsThisTurn(player.getSummonsThisTurn() + 1);
        match.getLogs().add(player.getName() + " 召唤了 " + definition.getName());
        checkWinner(match);

        broadcastMatch(match);
        return match;
    }

    /**
     * 献祭己方召唤区一名角色：该角色进入墓地，然后当前玩家抽 1 张牌。
     */
    public MatchState sacrifice(String matchId, SacrificeRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        matchSupportService.ensureReady(match);
        matchSupportService.ensureActionPhase(match);

        CardInstance target = matchSupportService.requireBoardCard(player, request.getTargetInstanceId());
        player.getBoard().remove(target);
        match.getDiscardPile().add(target);
        deckService.drawOne(match, player);
        match.getLogs().add(player.getName() + " 献祭了 " + battleService.cardName(target) + "，抽取了 1 张牌。");

        broadcastMatch(match);
        return match;
    }

    /**
     * 手牌排序：角色牌在前，技能牌在后，同类型保持原相对顺序。
     */
    public MatchState sortHand(String matchId, String playerId) {
        MatchState match = getMatch(matchId);
        matchSupportService.ensureNotFinished(match);
        matchSupportService.ensureReady(match);
        PlayerState player = matchSupportService.requirePlayer(match, playerId);
        player.getHand().sort(Comparator.comparingInt(card -> handSortRank(card.getCardId())));
        match.getLogs().add(player.getName() + " 整理了手牌。");

        broadcastMatch(match);
        return match;
    }

    /**
     * 使用技能牌。
     * 若技能范围是单体，则必须额外传目标角色或目标玩家。
     */
    public MatchState playSkill(String matchId, PlayEffectRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        PlayerState enemy = matchSupportService.requireOpponent(match, player.getPlayerId());
        matchSupportService.ensureReady(match);
        matchSupportService.ensureActionPhase(match);

        CardInstance card = matchSupportService.removeFromHand(player, request.getHandInstanceId());
        CardDefinition definition = cardCatalogService.require(card.getCardId());
        if (definition.getType() != CardType.SKILL) {
            throw new IllegalStateException("只有技能牌可以这样使用!");
        }

        if (skillResolverService.consumeNegateSkill(enemy, match, definition)) {
            match.getDiscardPile().add(card);
            match.getLogs().add(enemy.getName() + " 使技能 " + definition.getName() + " 无效");
            broadcastMatch(match);
            return match;
        }

        skillResolverService.resolveEffect(match, player, enemy, definition, request);
        match.getDiscardPile().add(card);
        match.getLogs().add(player.getName() + " 使用了技能 " + definition.getName());
        checkWinner(match);

        broadcastMatch(match);
        return match;
    }

    /**
     * 角色攻击角色。
     * 当前实现是单向伤害，不包含自动反击。
     */
    public MatchState attackCharacter(String matchId, AttackCharacterRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        PlayerState enemy = matchSupportService.requireOpponent(match, player.getPlayerId());
        matchSupportService.ensureReady(match);
        matchSupportService.ensureActionPhase(match);

        CardInstance attacker = matchSupportService.requireBoardCard(player, request.getAttackerInstanceId());
        CardInstance defender = matchSupportService.requireBoardCard(enemy, request.getDefenderInstanceId());
        battleService.ensureCanAttack(match, attacker);

        int attackValue = battleService.computeAttack(player, attacker);
        boolean defenderShielded = statusEffectService.consumeShield(enemy, match, battleService.cardName(defender));
        if (!defenderShielded) {
            battleService.damageCharacter(match, attacker, defender, attackValue);
        }

        attacker.setSleeping(true);
        battleService.cleanupDefeated(match, enemy);
        match.getLogs().add(battleService.cardName(attacker) + " 攻击了 " + battleService.cardName(defender));
        checkWinner(match);

        broadcastMatch(match);
        return match;
    }

    /**
     * 角色直接攻击玩家。
     * 只能在对方场上没有角色时使用。
     */
    public MatchState attackPlayer(String matchId, AttackPlayerRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        PlayerState enemy = matchSupportService.requireOpponent(match, player.getPlayerId());
        matchSupportService.ensureReady(match);
        matchSupportService.ensureActionPhase(match);

        if (!enemy.getBoard().isEmpty()) {
            throw new IllegalStateException("对方场上仍有角色，不能直接攻击玩家。");
        }

        CardInstance attacker = matchSupportService.requireBoardCard(player, request.getAttackerInstanceId());
        battleService.ensureCanAttack(match, attacker);

        if (!statusEffectService.consumeShield(enemy, match, enemy.getName())) {
            enemy.setHp(Math.max(0, enemy.getHp() - battleService.computeAttack(player, attacker)));
        }

        attacker.setSleeping(true);
        match.getLogs().add(battleService.cardName(attacker) + " 直接攻击了玩家 " + enemy.getName() + "。");
        checkWinner(match);

        broadcastMatch(match);
        return match;
    }

    /**
     * 结束当前玩家回合，并把回合控制权切给对手。
     */
    public MatchState endTurn(String matchId, String playerId) {
        EndTurnRequest request = new EndTurnRequest();
        request.setPlayerId(playerId);
        return endTurn(matchId, request);
    }

    /**
     * 结束当前玩家回合，并把回合控制权切给对手。
     */
    public MatchState endTurn(String matchId, EndTurnRequest request) {
        MatchState match = getMatch(matchId);
        PlayerState player = matchSupportService.requireCurrentPlayer(match, request.getPlayerId());
        PlayerState enemy = matchSupportService.requireOpponent(match, player.getPlayerId());
        matchSupportService.ensureNotFinished(match);
        matchSupportService.ensureReady(match);

        discardDownToHandLimit(match, player, request.getDiscardInstanceIds(), false);
        match.setCurrentPlayerId(enemy.getPlayerId());
        match.setTurn(match.getTurn() + 1);
        match.getLogs().add(player.getName() + " 结束了回合");
        startTurn(match, enemy);

        broadcastMatch(match);
        runBotTurnIfNeeded(match);
        return match;
    }

    /**
     * 回合开始自动抽 2 张牌并进入行动阶段。
     */
    private void startTurn(MatchState match, PlayerState player) {
        player.setSummonsThisTurn(0);
        deckService.drawOne(match, player);
        deckService.drawOne(match, player);
        statusEffectService.tickStatusDurations(player, match);
        statusEffectService.applyTurnStartEffects(match, player);
        checkWinner(match);
        if (match.getPhase() == GamePhase.FINISHED) {
            return;
        }
        wakeBoard(player);
        match.setPhase(GamePhase.ACTION);
        match.getLogs().add(player.getName() + " 自动抽了 2 张牌。");
    }

    /**
     * 每回合开始时，场上角色都会从休整状态恢复为可行动。
     */
    private void wakeBoard(PlayerState player) {
        for (CardInstance card : player.getBoard()) {
            card.setSleeping(false);
        }
    }

    /**
     * 检查胜负。
     * 任意玩家生命值小于等于 0 时，对手获胜。
     */
    private void checkWinner(MatchState match) {
        for (PlayerState player : match.getPlayers()) {
            if (player.getHp() <= 0) {
                PlayerState winner = matchSupportService.requireOpponent(match, player.getPlayerId());
                match.setWinnerId(winner.getPlayerId());
                match.setPhase(GamePhase.FINISHED);
                match.getLogs().add(winner.getName() + " 赢得了对局!");
                return;
            }
        }
    }

    private String generateRoomCode() {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        while (roomIndex.containsKey(code)) {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
        return code;
    }

    private String generatePlayerToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeOrCreateToken(String token) {
        return token == null || token.isBlank() ? generatePlayerToken() : token.trim();
    }

    /**
     * 在人机模式下补入一个最小可用的机器人对手。
     * 当前只负责占位和发牌，后续可继续扩展自动出牌逻辑。
     */
    private void addBotOpponent(MatchState match) {
        PlayerState bot = new PlayerState("P2", "瓦库", "BOT");
        match.getPlayers().add(bot);
        List<CardInstance> deck = deckService.buildDeck(match.getPlayers().get(0).getPlayerId(), bot.getPlayerId());
        Collections.shuffle(deck);
        match.setDrawPile(deck);
        match.setReady(true);
        match.getLogs().add("瓦库已加入房间。");
        for (int i = 0; i < 2; i++) {
            deckService.drawOne(match, match.getPlayers().get(0));
            deckService.drawOne(match, bot);
        }
        startTurn(match, match.getPlayers().get(0));
    }

    /**
     * 最小可用的人机逻辑：
     * 机器人在自己的回合会抽牌，然后从左到右优先召唤第一张角色牌；
     * 如果没有角色牌，则使用第一张技能牌；最后结束回合。
     */
    private void runBotTurnIfNeeded(MatchState match) {
        if (match.getMode() != BotMode.PVE || !"P2".equals(match.getCurrentPlayerId()) || match.getPhase() == GamePhase.FINISHED) {
            return;
        }

        PlayerState bot = matchSupportService.requirePlayer(match, "P2");
        matchSupportService.ensureActionPhase(match);

        CardInstance firstCharacter = bot.getHand().stream()
                .filter(card -> cardCatalogService.require(card.getCardId()).getType() == CardType.CHARACTER)
                .findFirst()
                .orElse(null);

        if (firstCharacter != null && bot.getBoard().size() < PlayerState.SUMMON_SLOTS) {
            CardDefinition definition = cardCatalogService.require(firstCharacter.getCardId());
            bot.getHand().remove(firstCharacter);
            firstCharacter.setSleeping(cardCatalogService.requireCard(definition.getId()).sleepsOnSummon());
            bot.getBoard().add(firstCharacter);
            bot.setSummonsThisTurn(1);
            match.getLogs().add("瓦库召唤了 " + definition.getName() + "。");
        } else {
            PlayerState player = matchSupportService.requirePlayer(match, "P1");
            CardInstance firstSkill = bot.getHand().stream()
                    .filter(card -> cardCatalogService.require(card.getCardId()).getType() == CardType.SKILL)
                    .filter(card -> canBotPrepareSkill(match, bot, player, card))
                    .findFirst()
                    .orElse(null);
            if (firstSkill != null) {
                CardDefinition definition = cardCatalogService.require(firstSkill.getCardId());
                PlayEffectRequest request = new PlayEffectRequest();
                request.setPlayerId("P2");
                request.setHandInstanceId(firstSkill.getInstanceId());
                assignBotSkillTarget(bot, player, definition, request);
                bot.getHand().remove(firstSkill);
                skillResolverService.resolveEffect(match, bot, player, definition, request);
                match.getDiscardPile().add(firstSkill);
                match.getLogs().add("瓦库使用了技能 " + definition.getName() + "。");
                checkWinner(match);
            }
        }

        if (match.getPhase() == GamePhase.FINISHED) {
            broadcastMatch(match);
            return;
        }

        discardDownToHandLimit(match, bot, null, true);
        match.setCurrentPlayerId("P1");
        match.setTurn(match.getTurn() + 1);
        match.getLogs().add("瓦库结束了回合。");
        startTurn(match, matchSupportService.requirePlayer(match, "P1"));
        broadcastMatch(match);
    }

    private void discardDownToHandLimit(MatchState match, PlayerState player, List<String> discardInstanceIds, boolean automatic) {
        int overflow = player.getHand().size() - PlayerState.HAND_LIMIT;
        if (overflow <= 0) {
            return;
        }
        List<String> selectedIds = discardInstanceIds == null ? List.of() : discardInstanceIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (!automatic && selectedIds.size() != overflow) {
            throw new IllegalStateException("手牌超过上限，请选择 " + overflow + " 张手牌弃置。");
        }
        if (automatic) {
            for (int index = 0; index < overflow; index += 1) {
                CardInstance discarded = player.getHand().remove(player.getHand().size() - 1);
                match.getDiscardPile().add(discarded);
            }
        } else {
            for (String discardId : selectedIds) {
                boolean inHand = player.getHand().stream()
                        .anyMatch(card -> card.getInstanceId().equals(discardId));
                if (!inHand) {
                    throw new IllegalArgumentException("手牌中不存在该卡牌: " + discardId);
                }
            }
            for (String discardId : selectedIds) {
                CardInstance discarded = matchSupportService.removeFromHand(player, discardId);
                match.getDiscardPile().add(discarded);
            }
        }
        match.getLogs().add(player.getName() + " 回合结束弃置了 " + overflow + " 张手牌。");
    }

    private int handSortRank(String cardId) {
        CardType type = cardCatalogService.require(cardId).getType();
        return type == CardType.CHARACTER ? 0 : 1;
    }

    private boolean canBotPrepareSkill(MatchState match, PlayerState bot, PlayerState player, CardInstance skill) {
        CardDefinition definition = cardCatalogService.require(skill.getCardId());
        PlayEffectRequest request = new PlayEffectRequest();
        request.setPlayerId("P2");
        request.setHandInstanceId(skill.getInstanceId());
        assignBotSkillTarget(bot, player, definition, request);
        return skillResolverService.canResolveEffect(match, bot, player, definition, request);
    }

    private void assignBotSkillTarget(PlayerState bot, PlayerState player, CardDefinition definition, PlayEffectRequest request) {
        if (definition.getSkillRange() == null) {
            return;
        }

        switch (definition.getSkillRange()) {
            case SELF, BOTH -> {
                request.setTargetPlayerId(bot.getPlayerId());
            }
            case ENEMY -> {
                request.setTargetPlayerId(player.getPlayerId());
            }
            case SINGLE -> {
                if ("soda".equals(definition.getId())) {
                    if (!player.getBoard().isEmpty()) {
                        request.setTargetPlayerId(player.getPlayerId());
                        request.setTargetInstanceId(player.getBoard().get(0).getInstanceId());
                        return;
                    }
                    if (!bot.getBoard().isEmpty()) {
                        request.setTargetPlayerId(bot.getPlayerId());
                        request.setTargetInstanceId(bot.getBoard().get(0).getInstanceId());
                    }
                    return;
                }
                request.setTargetPlayerId(bot.getPlayerId());
            }
        }
    }

    /**
     * REST 动作完成后，会把完整对局快照广播给房间内所有 WebSocket 连接。
     */
    private void broadcastMatch(MatchState match) {
        if (match.getRoomCode() == null || match.getRoomCode().isBlank()) {
            return;
        }
        try {
            gameWebSocketHandler.broadcast(match.getRoomCode(), objectMapper.writeValueAsString(match));
        } catch (Exception ignored) {
        }
    }

    private void removeMatch(MatchState match) {
        matches.remove(match.getMatchId());
        if (match.getRoomCode() != null) {
            roomIndex.remove(match.getRoomCode());
        }
    }
}
