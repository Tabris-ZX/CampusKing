package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class ElfCard extends BaseSkillCard {

    public ElfCard() {
        super(90, "elf", "瓶中精灵", "使用后两回合内，如果有己方角色死亡，则其回复至体力上限的 1/5。", EffectType.REVIVE_ALLY, EffectCategory.DURATION, 5, 2, SkillRange.SELF);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyReviveOnDeath();
    }
}
