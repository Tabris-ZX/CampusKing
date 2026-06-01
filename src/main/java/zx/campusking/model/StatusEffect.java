package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 玩家身上的状态效果实例。
 * 同时用于表示 Buff 和 Debuff。
 */
@Setter
@Getter
public class StatusEffect {

    /** 效果类型。 */
    private StatusEffectType type;
    /** 分类：buff / debuff。 */
    private String category;
    /** 效果数值。 */
    private int value;
    /** 当前层数。 */
    private int stacks;
    /** 剩余持续回合，空表示不按回合衰减。 */
    private Integer remainingTurns;
    /** 效果来源卡牌 id。 */
    private String sourceCardId;

    /**
     * JSON 反序列化用无参构造。
     */
    public StatusEffect() {}

    /**
     * 创建状态效果。
     *
     * @param type 效果类型
     * @param category 分类，通常为 buff 或 debuff
     * @param value 效果数值
     * @param stacks 层数
     * @param remainingTurns 剩余回合，空表示不按回合衰减
     * @param sourceCardId 来源卡牌 id
     */
    public StatusEffect(StatusEffectType type, String category, int value, int stacks, Integer remainingTurns, String sourceCardId) {
        this.type = type;
        this.category = category;
        this.value = value;
        this.stacks = stacks;
        this.remainingTurns = remainingTurns;
        this.sourceCardId = sourceCardId;
    }
}
