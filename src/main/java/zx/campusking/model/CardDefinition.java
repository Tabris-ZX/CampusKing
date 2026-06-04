package zx.campusking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 卡牌静态定义。
 * 由 cards 包下的具体卡牌类生成，用于前端展示、牌堆构筑和基础战斗数值读取。
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardDefinition {

    /** 卡牌唯一 id，同时用于贴图路径和对局实例关联。 */
    private String id;
    /** 前端展示的中文名称。 */
    private String name;
    /** 卡牌类型：角色牌或技能牌。 */
    private CardType type;
    /** 卡牌规则描述。 */
    private String description;
    /** 使用或召唤该卡牌消耗的行动点。 */
    private Integer actionCost;
    /** 卡牌稀有度。 */
    private CardRarity rarity;
    /** 角色基础攻击力。 */
    private Integer attack;
    /** 角色基础生命值。 */
    private Integer health;
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
    /** 技能额外需要从自己手牌中选择弃置的牌数。 */
    private Integer requiredHandDiscardCount;
    /** 角色或技能专属参数，例如鸟女的第二形态。 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> exclusive = new LinkedHashMap<>();

    @JsonIgnore
    public Integer getSecondaryAttack() {
        return exclusiveInteger("secondaryAttack");
    }

    @JsonSetter("secondaryAttack")
    public void setSecondaryAttack(Integer secondaryAttack) {
        setExclusiveInteger("secondaryAttack", secondaryAttack);
    }

    @JsonIgnore
    public Integer getSecondaryHealth() {
        return exclusiveInteger("secondaryHealth");
    }

    @JsonSetter("secondaryHealth")
    public void setSecondaryHealth(Integer secondaryHealth) {
        setExclusiveInteger("secondaryHealth", secondaryHealth);
    }

    private Integer exclusiveInteger(String key) {
        Object value = exclusive == null ? null : exclusive.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void setExclusiveInteger(String key, Integer value) {
        if (exclusive == null) {
            exclusive = new LinkedHashMap<>();
        }
        if (value == null) {
            exclusive.remove(key);
            return;
        }
        exclusive.put(key, value);
    }

}
