package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class ChipsCard extends BaseSkillCard {

    public ChipsCard() {
        super(120, "chips", "赌徒筹码", "弃置至多 3 张牌，然后抽等量的牌数。", EffectType.DISCARD_AND_DRAW, EffectCategory.INSTANT, 3, 0, SkillRange.SELF);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.discardAndDraw();
    }
}
