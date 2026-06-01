package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 创建房间请求。
 * 可创建普通 PVP 房间，也可通过 botMode 创建 PVE 人机房间。
 */
@Setter
@Getter
public class CreateRoomRequest {

    /** 房主名称。 */
    private String hostName;
    /** 浏览器会话恢复 token。 */
    private String playerToken;
    /** 是否直接创建人机模式房间。 */
    private Boolean botMode;

}
