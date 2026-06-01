package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class LightCard extends BaseSkillCard {

    public LightCard() {
        super(70, "light", "圣光", "完全抵御下一次伤害。", EffectType.SHIELD, EffectCategory.DURATION, 1, 1, SkillRange.SELF);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyShield();
    }
}
