package zx.campusking.service;

import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

/**
 * 对局初始化接口。
 * 负责新局牌堆、初始手牌、先手和回合开始自动流程。
 */
public interface MatchInitializerService {

    /** 按当前对局玩法初始化第一局。 */
    void initialize(MatchState match);

    /** 按指定先手初始化下一小局。 */
    void initializeRound(MatchState match, PlayerState firstPlayer);

    /** 结算玩家回合开始流程并进入行动阶段。 */
    void startTurn(MatchState match, PlayerState player);
}
