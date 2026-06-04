package zx.campusking.service;

import zx.campusking.model.CardInstance;
import zx.campusking.model.GamePhase;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

/**
 * 对局辅助接口。
 * 提供玩家、手牌、场上角色定位以及阶段/状态校验。
 */
public interface MatchSupportService {

    /** 返回非空默认名称。 */
    String defaultName(String value, String fallback);

    /** 读取并校验当前行动玩家。 */
    PlayerState requireCurrentPlayer(MatchState match, String playerId);

    /** 按玩家 id 读取玩家。 */
    PlayerState requirePlayer(MatchState match, String playerId);

    /** 读取指定玩家的对手。 */
    PlayerState requireOpponent(MatchState match, String playerId);

    /** 校验当前处于行动阶段。 */
    void ensureActionPhase(MatchState match);

    /** 校验房间已可进行对局。 */
    void ensureReady(MatchState match);

    /** 校验当前阶段。 */
    void ensurePhase(MatchState match, GamePhase expectedPhase);

    /** 校验对局未结束。 */
    void ensureNotFinished(MatchState match);

    /** 从手牌中移除并返回指定实例。 */
    CardInstance removeFromHand(PlayerState player, String instanceId);

    /** 从手牌中读取指定实例。 */
    CardInstance requireHandCard(PlayerState player, String instanceId);

    /** 从召唤区读取指定实例。 */
    CardInstance requireBoardCard(PlayerState player, String instanceId);
}
