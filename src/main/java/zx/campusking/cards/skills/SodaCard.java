package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class SodaCard extends BaseSkillCard {

    public SodaCard() {
        super(80, "soda", "汽水", "抽 2，我方召唤区一名角色 +40 生命，或者对方召唤区一名角色 -40 生命。", EffectType.DRAW_AND_MODIFY_UNIT, EffectCategory.INSTANT, 40, 0, SkillRange.SINGLE);
    }

    @Override
    public boolean canResolveSkill(CardEffectContext context) {
        return !context.player().getBoard().isEmpty() || !context.enemy().getBoard().isEmpty();
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.drawOne();
        context.drawOne();
        context.modifyUnit();
    }
}
