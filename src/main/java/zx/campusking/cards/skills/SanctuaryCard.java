package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.PlayerState;
import zx.campusking.model.StatusEffectType;

public final class SanctuaryCard extends BaseSkillCard {

    public static final String ID = "sanctuary";
    public static final int ORDER = 60;

    public SanctuaryCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        applyBuffs(context, context.player());
        applyBuffs(context, context.enemy());
    }

    private void applyBuffs(CardEffectContext context, PlayerState target) {
        int value = context.value();
        int duration = context.duration(2);
        context.applyBoardBuff(target, StatusEffectType.ATTACK_UP, value, 1, duration);
        context.applyBoardBuff(target, StatusEffectType.MAX_HP_UP, value, 1, duration);
        context.applyBoardBuff(target, StatusEffectType.TURN_HEAL, value, 1, duration);
    }
}
