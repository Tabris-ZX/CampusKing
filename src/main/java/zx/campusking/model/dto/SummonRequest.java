package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 召唤角色牌请求。
 */
@Setter
@Getter
public class SummonRequest {

    /** 发起召唤的玩家 id。 */
    private String playerId;
    /** 手牌中待召唤的卡牌实例 id。 */
    private String handInstanceId;

}
