package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;
import zx.campusking.model.MatchState;

/**
 * 浏览器刷新恢复房间会话的响应。
 * 返回完整房间快照和当前 token 对应的玩家 id。
 */
@Setter
@Getter
public class RestoreSessionResponse {

    /** 当前房间对应的对局快照。 */
    private MatchState match;
    /** 当前浏览器会话对应的玩家 id。 */
    private String playerId;

    /**
     * 创建恢复会话响应。
     *
     * @param match 对局快照
     * @param playerId 当前会话玩家 id
     */
    public RestoreSessionResponse(MatchState match, String playerId) {
        this.match = match;
        this.playerId = playerId;
    }

}
