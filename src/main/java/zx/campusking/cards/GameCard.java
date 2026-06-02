package zx.campusking.cards;

import zx.campusking.model.CardDefinition;

/**
 * 一张完整卡牌的通用接口。
 * 卡牌的静态定义、技能结算和战斗特性都从这里挂接，避免规则服务继续按卡牌 id 分支。
 */
public interface GameCard {

    /**
     * 返回卡牌的静态定义。
     * 前端展示、构筑牌堆和战斗数值都从这份定义读取基础信息。
     */
    CardDefinition definition();

    /**
     * 卡牌在目录中的展示和构筑顺序。
     * 数值越小越靠前；相同顺序时会按卡牌 id 排序。
     */
    default int order() {
        return 1000;
    }

    /**
     * 当前卡牌是否不会被“下一张技能无效”反制。
     * 例如反制牌自身通常需要返回 true，否则两张反制牌会互相卡住。
     */
    default boolean bypassesNegate() {
        return false;
    }

    /**
     * 角色牌被召唤后是否需要保持休整状态。
     * 普通角色返回 false；需要上场一回合后才能行动的角色返回 true。
     */
    default boolean sleepsOnSummon() {
        return false;
    }

    /**
     * 创建卡牌实例时附带的额外命次数。
     * 默认根据 secondaryHealth 推断一次额外形态，特殊角色可以覆写。
     */
    default int extraLives() {
        return definition().getSecondaryHealth() == null ? 0 : 1;
    }

    /**
     * 技能牌在当前上下文下是否允许结算。
     * 主要用于机器人自动出牌前判断，避免选择没有合法目标的技能。
     */
    default boolean canResolveSkill(CardEffectContext context) {
        return true;
    }

    /**
     * 技能牌的具体效果入口。
     * 技能实现只依赖 CardEffectContext 暴露的通用能力，不直接耦合到 GameService。
     */
    default void resolveSkill(CardEffectContext context) {
    }

    /**
     * 角色攻击力修正 hook。
     * BattleService 会先算基础攻击和通用 Buff，再交给卡牌处理自己的特殊加成。
     */
    default int modifyAttack(CardCombatContext context, int attack) {
        return attack;
    }

    /**
     * 角色攻击实际造成伤害后的 hook。
     * 被抵御或伤害为 0 时不会触发。
     */
    default void afterAttackDamage(CardAttackDamageContext context) {
    }

    /**
     * 角色被击败时的特殊处理 hook。
     * 返回 true 表示卡牌已经自行处理死亡结果，BattleService 不再进入通用死亡流程。
     */
    default boolean handleDefeated(CardDefeatContext context) {
        return false;
    }
}
