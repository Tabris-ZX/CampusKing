package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 离开房间请求。
 */
@Getter
@Setter
public class LeaveRoomRequest {

    /** 当前浏览器会话 token。 */
    private String playerToken;

}
