package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 卡牌静态定义。
 * 由 cards 包下的具体卡牌类生成，用于前端展示、牌堆构筑和基础战斗数值读取。
 */
@Setter
@Getter
public class CardDefinition {

    /** 卡牌唯一 id，同时用于贴图路径和对局实例关联。 */
    private String id;
    /** 前端展示的中文名称。 */
    private String name;
    /** 卡牌类型：角色牌或技能牌。 */
    private CardType type;
    /** 卡牌规则描述。 */
    private String description;
    /** 角色基础攻击力，技能牌通常为 0。 */
    private Integer attack;
    /** 前端展示用攻击文本，例如“+∞”或“20/50”。 */
    private String attackText;
    /** 角色基础生命值，技能牌通常为 0。 */
    private Integer health;
    /** 第二形态攻击力，例如鸟女变形后的攻击。 */
    private Integer secondaryAttack;
    /** 第二形态生命值，例如鸟女变形后的生命。 */
    private Integer secondaryHealth;
    /** 前端展示用效果类型，实际结算由具体卡牌类实现。 */
    private EffectType effectType;
    /** 前端展示用效果分类。 */
    private EffectCategory effectCategory;
    /** 技能效果数值，供卡牌结算上下文读取。 */
    private Integer effectValue;
    /** 技能持续回合，0 通常表示即时效果。 */
    private Integer effectDuration;
    /** 技能作用范围，供前端选择目标和机器人预填目标使用。 */
    private SkillRange skillRange;

}
