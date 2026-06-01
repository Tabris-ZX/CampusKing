package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class HandsCard extends BaseSkillCard {

    public HandsCard() {
        super(130, "hands", "第三只手", "弃置对方 1 张手牌。", EffectType.DISCARD_ENEMY_HAND, EffectCategory.INSTANT, 1, 0, SkillRange.ENEMY);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.discardEnemyHand();
    }
}
