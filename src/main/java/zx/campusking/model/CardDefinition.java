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
    private Integer health;
    private EffectType effectType;
    private Integer effectValue;
    private SkillRange skillRange;
    private List<String> traits;

}
