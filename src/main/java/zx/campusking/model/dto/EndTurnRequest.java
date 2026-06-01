package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 结束回合请求。
 */
@Setter
@Getter
public class EndTurnRequest {

    /** 当前玩家 id。 */
    private String playerId;
    /** 手牌超限时，玩家选择弃置的手牌实例 id。 */
    private List<String> discardInstanceIds;

}
