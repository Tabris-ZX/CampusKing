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
    /** 胜利玩家 id，未结束时为空。 */
    private String winnerId;
    /** 房间是否已满足开局条件。 */
    private boolean ready;
    /** 对局模式，默认双人 PVP。 */
    private BotMode mode = BotMode.PVP;
    /** 对局日志，前端用来展示最近发生的规则事件。 */
    private List<String> logs = new ArrayList<>();

}
