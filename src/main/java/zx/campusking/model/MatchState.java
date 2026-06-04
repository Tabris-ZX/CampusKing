package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局对战的完整状态快照。
 * 该对象会通过 REST 和 WebSocket 发送给前端，因此字段保持可序列化的简单结构。
 */
@Setter
@Getter
public class MatchState {

    /** 对局唯一 id，动作接口通过它定位对局。 */
    private String matchId;
    /** 玩家加入房间时使用的短房间码。 */
    private String roomCode;
    /** 当前房间中的玩家状态，最多两名。 */
    private List<PlayerState> players = new ArrayList<>();
    /** 公共抽牌堆。 */
    private List<CardInstance> drawPile = new ArrayList<>();
    /** 公共弃牌堆/墓地。 */
    private List<CardInstance> discardPile = new ArrayList<>();
    /** 当前行动玩家 id。 */
    private String currentPlayerId;
    /** 当前对局阶段。 */
    private GamePhase phase;
    /** 当前回合计数。 */
    private int turn;
    /** 整场比赛胜利玩家 id，未结束时为空。 */
    private String winnerId;
    /** 最近一局胜利玩家 id，未结算小局时为空。 */
    private String roundWinnerId;
    /** 当前小局序号，从 1 开始。 */
    private int roundNumber = 1;
    /** 三局两胜所需胜场数。 */
    private int winsRequired = 2;
    /** P1 当前小局胜场。 */
    private int p1Wins;
    /** P2 当前小局胜场。 */
    private int p2Wins;
    /** 首局先手玩家 id。 */
    private String firstRoundFirstPlayerId = "P1";
    /** 房间是否已满足开局条件。 */
    private boolean ready;
    /** 对局模式，默认双人 PVP。 */
    private BotMode mode = BotMode.PVP;
    /** 玩法类型，默认单面玩法。 */
    private MatchPlayType playType = MatchPlayType.SINGLE_SIDE;
    /** 每名玩家上一回合行动阶段主动使用的最后一张技能牌。 */
    private List<LastPlayedSkill> lastPlayedSkills = new ArrayList<>();
    /** 对局日志，前端用来展示最近发生的规则事件。 */
    private List<String> logs = new ArrayList<>();

}
