package zx.campusking.service;

import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

/**
 * 回合开始角色特性接口。
 * 负责遍历场上角色并派发回合开始 hook，同时提供通用回合开始能力。
 */
public interface TurnStartCharacterService {

    /** 触发指定玩家召唤区角色的回合开始特性。 */
    void trigger(MatchState match, PlayerState player);

    /** 由角色特性调用，免费再次打出该玩家上一回合主动使用的最后一张技能。 */
    void replayLastActiveSkill(MatchState match, PlayerState player, CardInstance source);
}
