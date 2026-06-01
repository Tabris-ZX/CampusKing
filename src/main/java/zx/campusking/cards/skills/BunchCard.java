package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class BunchCard extends BaseSkillCard {

    public BunchCard() {
        super(100, "bunch", "普通一拳", "行动阶段可用，对对方全体单位造成 15 点伤害。", EffectType.DAMAGE_ALL_ENEMIES, EffectCategory.INSTANT, 15, 0, SkillRange.ENEMY);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.damageAllEnemies();
    }
}
