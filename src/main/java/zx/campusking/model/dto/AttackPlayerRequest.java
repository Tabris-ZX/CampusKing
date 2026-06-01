package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 角色直接攻击玩家的请求体。
 */
@Setter
@Getter
public class AttackPlayerRequest {

    /** 发起攻击的玩家 id。 */
    private String playerId;
    /** 攻击方角色实例 id。 */
    private String attackerInstanceId;

}
