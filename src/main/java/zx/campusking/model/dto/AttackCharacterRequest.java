package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 角色攻击对方角色的请求体。
 */
@Setter
@Getter
public class AttackCharacterRequest {

    /** 发起攻击的玩家 id。 */
    private String playerId;
    /** 攻击方角色实例 id。 */
    private String attackerInstanceId;
    /** 防守方角色实例 id。 */
    private String defenderInstanceId;

}
