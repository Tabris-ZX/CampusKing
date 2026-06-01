package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class SanctuaryCard extends BaseSkillCard {

    public SanctuaryCard() {
        super(60, "sanctuary", "圣域", "行动阶段可用，全场最大生命 +10，每回合回复 10 点生命，攻击力 +10。", EffectType.GLOBAL_BUFF, EffectCategory.DURATION, 10, 2, SkillRange.BOTH);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyGlobalBuff(context.player());
        context.applyGlobalBuff(context.enemy());
    }
}
