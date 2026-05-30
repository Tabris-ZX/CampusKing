package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CardDefinition {

    private String id;
    private String name;
    private CardType type;
    private String description;
    private Integer attack;
    private String attackText;
    private Integer health;
    private Integer secondaryAttack;
    private Integer secondaryHealth;
    private EffectType effectType;
    private EffectCategory effectCategory;
    private Integer effectValue;
    private Integer effectDuration;
    private SkillRange skillRange;
    private List<String> traits;

}
