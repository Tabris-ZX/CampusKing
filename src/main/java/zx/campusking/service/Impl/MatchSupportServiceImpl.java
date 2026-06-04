package zx.campusking.service.Impl;

import zx.campusking.service.*;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardInstance;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

@Service
public class MatchSupportServiceImpl implements MatchSupportService {

    public String defaultName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public PlayerState requireCurrentPlayer(MatchState match, String playerId) {
        ensureNotFinished(match);
        if (!match.getCurrentPlayerId().equals(playerId)) {
            throw new IllegalStateException("当前不是玩家 " + playerId + " 的回合。");
        }
        return requirePlayer(match, playerId);
    }

    public PlayerState requirePlayer(MatchState match, String playerId) {
        return match.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知玩家: " + playerId));
    }

    public PlayerState requireOpponent(MatchState match, String playerId) {
        return match.getPlayers().stream()
                .filter(player -> !player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow();
    }

    public void ensureActionPhase(MatchState match) {
        ensureNotFinished(match);
        ensurePhase(match, GamePhase.ACTION);
    }

    public void ensureReady(MatchState match) {
        if (!match.isReady()) {
            throw new IllegalStateException("房间还在等待第二位玩家加入。");
        }
    }

    public void ensurePhase(MatchState match, GamePhase expectedPhase) {
        if (match.getPhase() != expectedPhase) {
            throw new IllegalStateException("当前阶段不是 " + expectedPhase + "。");
        }
    }

    public void ensureNotFinished(MatchState match) {
        if (match.getPhase() == GamePhase.FINISHED) {
            throw new IllegalStateException("对局已经结束。");
        }
    }

    public CardInstance removeFromHand(PlayerState player, String instanceId) {
        CardInstance card = requireHandCard(player, instanceId);
        player.getHand().remove(card);
        return card;
    }

    public CardInstance requireHandCard(PlayerState player, String instanceId) {
        return player.getHand().stream()
                .filter(card -> card.getInstanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("手牌中不存在该卡牌: " + instanceId));
    }

    public CardInstance requireBoardCard(PlayerState player, String instanceId) {
        return player.getBoard().stream()
                .filter(card -> card.getInstanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("场上不存在该角色: " + instanceId));
    }
}
