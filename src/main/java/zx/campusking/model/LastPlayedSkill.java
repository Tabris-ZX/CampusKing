package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录玩家行动阶段主动使用的最后一张技能牌, 供回合开始特性读取。
 */
@Getter
@Setter
public class LastPlayedSkill {

    private String playerId;
    private String cardId;
    private String targetPlayerId;
    private String targetInstanceId;
    private List<String> discardInstanceIds = new ArrayList<>();
    private int turn;
    private int roundNumber;

}
