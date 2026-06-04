package zx.campusking.service.Impl;

import zx.campusking.service.*;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardInstance;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchPlayType;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

import java.util.Collections;
import java.util.List;

/**
 * 对局初始化服务。
 * 负责按玩法类型构筑牌堆、发初始手牌，并进入第一回合。
 */
@Service
public class MatchInitializerServiceImpl implements MatchInitializerService {

    private final DeckService deckService;
    private final StatusEffectService statusEffectService;
    private final TurnStartCharacterService turnStartCharacterService;

    public MatchInitializerServiceImpl(
            DeckService deckService,
            StatusEffectService statusEffectService,
            TurnStartCharacterService turnStartCharacterService
    ) {
        this.deckService = deckService;
        this.statusEffectService = statusEffectService;
        this.turnStartCharacterService = turnStartCharacterService;
    }

    public void initialize(MatchState match) {
        MatchPlayType playType = match.getPlayType() == null ? MatchPlayType.SINGLE_SIDE : match.getPlayType();
        switch (playType) {
            case SINGLE_SIDE -> initializeSingleSide(match, firstPlayerForRound(match));
            case DOUBLE_SIDE -> initializeDoubleSide(match);
        }
    }

    public void initializeRound(MatchState match, PlayerState firstPlayer) {
        MatchPlayType playType = match.getPlayType() == null ? MatchPlayType.SINGLE_SIDE : match.getPlayType();
        switch (playType) {
            case SINGLE_SIDE -> initializeSingleSide(match, firstPlayer);
            case DOUBLE_SIDE -> initializeDoubleSide(match);
        }
    }

    private void initializeSingleSide(MatchState match, PlayerState firstPlayer) {
        if (match.getPlayers().size() < 2) {
            throw new IllegalStateException("至少需要两名玩家才能初始化对局。");
        }
        resetRoundState(match);
        PlayerState secondPlayer = match.getPlayers().stream()
                .filter(player -> !player.getPlayerId().equals(firstPlayer.getPlayerId()))
                .findFirst()
                .orElseThrow();
        List<CardInstance> deck = deckService.buildDeck(firstPlayer.getPlayerId(), secondPlayer.getPlayerId());
        Collections.shuffle(deck);
        match.setDrawPile(deck);
        match.setReady(true);
        match.setCurrentPlayerId(firstPlayer.getPlayerId());
        match.setTurn(1);
        for (int index = 0; index < 2; index += 1) {
            deckService.drawOne(match, firstPlayer);
            deckService.drawOne(match, secondPlayer);
        }
        startTurn(match, firstPlayer);
    }

    private void initializeDoubleSide(MatchState match) {
        throw new IllegalStateException("双面玩法暂未实现。");
    }

    /**
     * 回合开始自动抽牌并进入行动阶段。
     * 先手玩家第 1 回合额外抽 1 张。
     */
    public void startTurn(MatchState match, PlayerState player) {
        player.setTurnsTaken(player.getTurnsTaken() + 1);
        player.setSummonsThisTurn(0);
        player.setActionPoints(PlayerState.MAX_ACTION_POINTS);
        int drawCount = match.getTurn() == 1 ? 3 : 2;
        for (int index = 0; index < drawCount; index += 1) {
            deckService.drawOne(match, player);
        }
        statusEffectService.tickStatusDurations(player, match);
        statusEffectService.applyTurnStartEffects(match, player);
        turnStartCharacterService.trigger(match, player);
        if (match.getPhase() == GamePhase.FINISHED) {
            return;
        }
        wakeBoard(player);
        match.setPhase(GamePhase.ACTION);
        match.getLogs().add(player.getName() + " 自动抽了 " + drawCount + " 张牌.");
    }

    private void wakeBoard(PlayerState player) {
        for (CardInstance card : player.getBoard()) {
            card.setSleeping(false);
        }
    }

    private PlayerState firstPlayerForRound(MatchState match) {
        String firstPlayerId = match.getRoundNumber() % 2 == 1
                ? match.getFirstRoundFirstPlayerId()
                : opponentId(match.getFirstRoundFirstPlayerId());
        return match.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(firstPlayerId))
                .findFirst()
                .orElse(match.getPlayers().get(0));
    }

    private String opponentId(String playerId) {
        return "P2".equals(playerId) ? "P1" : "P2";
    }

    private void resetRoundState(MatchState match) {
        match.getDiscardPile().clear();
        match.setDrawPile(new java.util.ArrayList<>());
        match.getLastPlayedSkills().clear();
        match.setRoundWinnerId(null);
        match.setWinnerId(null);
        match.setPhase(GamePhase.DRAW);
        for (PlayerState player : match.getPlayers()) {
            player.setHp(PlayerState.MAX_HP);
            player.setSummonsThisTurn(0);
            player.setActionPoints(PlayerState.MAX_ACTION_POINTS);
            player.getStatusEffects().clear();
            player.getHand().clear();
            player.getBoard().clear();
        }
    }
}
