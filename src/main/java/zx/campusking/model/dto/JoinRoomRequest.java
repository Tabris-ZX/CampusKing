package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 加入房间请求。
 */
@Setter
@Getter
public class JoinRoomRequest {

    /** 加入玩家展示名称。 */
    private String playerName;
    /** 浏览器会话 token，用于刷新恢复身份。 */
    private String playerToken;

}
