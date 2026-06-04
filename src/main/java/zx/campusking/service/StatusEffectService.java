package zx.campusking.service;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.PreventableAction;
import zx.campusking.model.StatusEffect;
import zx.campusking.model.StatusEffectType;

/**
 * 状态效果接口。
 * 负责持续效果刷新、增益叠加、抵御效果消耗和最大生命等派生数值计算。
 */
public interface StatusEffectService {

    /** 结算玩家回合开始的回血效果。 */
    void applyTurnStartEffects(MatchState match, PlayerState player);

    /** 推进玩家和场上角色的持续效果回合数。 */
    void tickStatusDurations(PlayerState player, MatchState match);

    /** 对玩家添加或刷新状态效果。 */
    void applyOrRefreshEffect(PlayerState player, StatusEffect incoming);

    /** 对角色添加或刷新状态效果。 */
    void applyOrRefreshEffect(CardInstance card, StatusEffect incoming);

    /** 查找玩家身上的首个指定类型状态。 */
    StatusEffect findFirstEffect(PlayerState player, StatusEffectType type);

    /** 查找角色身上的首个指定类型状态。 */
    StatusEffect findFirstEffect(CardInstance card, StatusEffectType type);

    /** 汇总玩家身上指定类型状态的总数值。 */
    int sumEffectValue(PlayerState player, StatusEffectType type);

    /** 汇总角色身上指定类型状态的总数值。 */
    int sumEffectValue(CardInstance card, StatusEffectType type);

    /** 计算指定静态角色定义在玩家状态下的最大生命。 */
    int effectiveMaxHealth(PlayerState player, CardDefinition definition);

    /** 计算指定角色实例在玩家状态下的最大生命。 */
    int effectiveMaxHealth(PlayerState player, CardInstance card);

    /** 添加下一次动作抵御效果。 */
    void applyPreventNextAction(PlayerState player, StatusEffect incoming);

    /** 消耗下一次动作抵御效果。 */
    boolean consumeActionPrevention(PlayerState player, MatchState match, PreventableAction action, String actionName);

    /** 消耗死亡复活效果。 */
    StatusEffect consumeReviveOnDeath(PlayerState player, MatchState match);

    /** 移除玩家身上过期或层数耗尽的状态。 */
    void removeExpiredEffects(PlayerState player);

    /** 移除角色身上过期或层数耗尽的状态。 */
    void removeExpiredEffects(CardInstance card);
}
