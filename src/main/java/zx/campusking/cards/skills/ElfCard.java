package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.CardDefinition;

public final class ElfCard extends BaseSkillCard {

    public static final String ID = "elf";
    public static final int ORDER = 90;

    public ElfCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        context.applyReviveOnDeath();
    }
}
