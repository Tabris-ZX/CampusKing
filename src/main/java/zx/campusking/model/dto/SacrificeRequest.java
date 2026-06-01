package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 献祭己方召唤区角色的请求体。
 */
@Setter
@Getter
public class SacrificeRequest {

    /** 发起献祭的玩家 id。 */
    private String playerId;
    /** 被献祭的己方场上角色实例 id。 */
    private String targetInstanceId;

}
