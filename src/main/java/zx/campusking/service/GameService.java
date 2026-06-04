package zx.campusking.service;

import zx.campusking.model.MatchState;
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

/**
 * 对局编排接口。
 * 负责房间生命周期、玩家动作入口、回合切换、人机行动和对局快照查询。
 */
public interface GameService {

    /** 创建房间或人机房间。 */
    MatchState createRoom(CreateRoomRequest request);

    /** 通过房间码加入房间。 */
    MatchState joinRoom(String roomCode, JoinRoomRequest request);

    /** 离开房间并处理剩余玩家胜负。 */
    void leaveRoom(String roomCode, LeaveRoomRequest request);

    /** 通过房间码和玩家 token 恢复会话。 */
    RestoreSessionResponse restoreRoomSession(String roomCode, String playerToken);

    /** 按对局 id 获取对局快照。 */
    MatchState getMatch(String matchId);

    /** 按房间码获取对局快照。 */
    MatchState getMatchByRoomCode(String roomCode);

    /** 执行抽牌阶段进入行动阶段。 */
    MatchState drawPhase(String matchId, String playerId);

    /** 召唤手牌中的角色牌。 */
    MatchState summon(String matchId, SummonRequest request);

    /** 献祭己方召唤区角色。 */
    MatchState sacrifice(String matchId, SacrificeRequest request);

    /** 整理当前玩家手牌顺序。 */
    MatchState sortHand(String matchId, String playerId);

    /** 使用技能牌。 */
    MatchState playSkill(String matchId, PlayEffectRequest request);

    /** 使用角色攻击对方角色。 */
    MatchState attackCharacter(String matchId, AttackCharacterRequest request);

    /** 使用角色直接攻击对方玩家。 */
    MatchState attackPlayer(String matchId, AttackPlayerRequest request);

    /** 兼容旧接口的结束回合入口。 */
    MatchState endTurn(String matchId, String playerId);

    /** 结束当前玩家回合。 */
    MatchState endTurn(String matchId, EndTurnRequest request);
}
