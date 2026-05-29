package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

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
