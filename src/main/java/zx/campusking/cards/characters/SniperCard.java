package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;

public final class SniperCard extends BaseCharacterCard {

    public SniperCard() {
        super(30, "sniper", "枪", "上场首回合要预瞄，无法行动。", 9999, "+∞", 50, null, null);
    }

    @Override
    public boolean sleepsOnSummon() {
        return true;
    }
}
