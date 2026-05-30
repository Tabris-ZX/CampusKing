package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
/**
 * 使用技能牌请求。
 * 若技能范围为单体，则需要同时指定目标玩家或目标角色。
 */
public class PlayEffectRequest {

    /** 发起动作的玩家 id。 */
    private String playerId;
    /** 手中待使用的技能牌实例 id。 */
    private String handInstanceId;
    /** 目标玩家 id。 */
    private String targetPlayerId;
    /** 目标角色实例 id，可为空表示直接指定玩家。 */
    private String targetInstanceId;
    /** 额外弃置的手牌实例 id，仅部分技能使用。 */
    private List<String> discardInstanceIds;

}
