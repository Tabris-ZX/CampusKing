package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class UmbrellaCard extends BaseSkillCard {

    public UmbrellaCard() {
        super(110, "umbrella", "伞", "使对方下一张技能牌无效。", EffectType.COUNTER_EFFECT, EffectCategory.DURATION, 1, 1, SkillRange.SELF);
    }

    @Override
    public boolean bypassesNegate() {
        return true;
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyNegateNextSkill();
    }
}
