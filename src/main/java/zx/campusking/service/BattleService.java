package zx.campusking.service;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

/**
 * 战斗结算接口。
 * 提供攻击资格校验、攻击力计算、伤害统计、死亡清理和卡牌展示名读取能力。
 */
public interface BattleService {

    /**
     * 校验指定场上角色当前是否可以发起攻击。
     *
     * @param match 当前对局
     * @param attacker 攻击者实例
     */
    void ensureCanAttack(MatchState match, CardInstance attacker);

    /**
     * 计算角色当前攻击力，包含状态效果和卡牌自身攻击修正。
     *
     * @param owner 角色归属玩家
     * @param cardInstance 角色实例
     * @return 当前攻击力
     */
    int computeAttack(PlayerState owner, CardInstance cardInstance);

    /**
     * 对目标角色造成伤害并记录实际伤害数据。
     *
     * @param match 当前对局
     * @param sourceOwner 伤害来源玩家
     * @param targetOwner 伤害承受玩家
     * @param source 伤害来源卡牌
     * @param target 目标角色
     * @param damage 伤害数值
     * @return 是否造成了有效伤害
     */
    boolean damageCharacter(MatchState match, PlayerState sourceOwner, PlayerState targetOwner, CardInstance source, CardInstance target, int damage);

    /**
     * 记录玩家造成和承受的实际伤害。
     *
     * @param sourceOwner 伤害来源玩家
     * @param targetOwner 伤害承受玩家
     * @param damage 实际伤害
     */
    void recordDamage(PlayerState sourceOwner, PlayerState targetOwner, int damage);

    /**
     * 清理指定玩家召唤区中已经被击败的角色。
     *
     * @param match 当前对局
     * @param player 被清理的玩家
     */
    void cleanupDefeated(MatchState match, PlayerState player);

    /**
     * 清理指定玩家召唤区中已经被击败的角色，并向击败方发放奖励。
     *
     * @param match 当前对局
     * @param player 被清理的玩家
     * @param rewardOwner 奖励归属玩家
     * @param deckService 牌堆服务
     */
    void cleanupDefeated(MatchState match, PlayerState player, PlayerState rewardOwner, DeckService deckService);

    /**
     * 读取卡牌实例对应的展示名。
     *
     * @param instance 卡牌实例
     * @return 展示名
     */
    String cardName(CardInstance instance);

    /**
     * 读取卡牌实例对应的静态定义。
     *
     * @param instance 卡牌实例
     * @return 静态定义
     */
    CardDefinition cardDefinition(CardInstance instance);
}
