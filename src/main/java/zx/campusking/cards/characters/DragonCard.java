package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.cards.CardCombatContext;
import zx.campusking.cards.CardDefeatContext;

public final class DragonCard extends BaseCharacterCard {

    public DragonCard() {
        super(50, "dragon", "龙骑", "攻击时会额外造成已损失体力值的伤害；第一次死亡时回复至 1 点体力。", 70, null, 20, null, null);
    }

    @Override
    public int modifyAttack(CardCombatContext context, int attack) {
        return attack + Math.max(0, context.baseHealth() - context.card().getCurrentHealth());
    }

    @Override
    public boolean handleDefeated(CardDefeatContext context) {
        if (context.card().isRevived()) {
            return false;
        }
        context.card().setRevived(true);
        context.card().setCurrentHealth(1);
        context.match().getLogs().add(context.definition().getName() + " 首次被击败后回复到 1 点体力。");
        return true;
    }
}
