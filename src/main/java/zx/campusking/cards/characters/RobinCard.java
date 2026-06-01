package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.cards.CardDefeatContext;

public final class RobinCard extends BaseCharacterCard {

    public RobinCard() {
        super(40, "robin", "鸟女", "双角色，两个角色都死亡才会下场。", 20, "20/50", 20, 50, 50);
    }

    @Override
    public boolean handleDefeated(CardDefeatContext context) {
        if (context.card().getExtraLives() <= 0) {
            return false;
        }
        context.card().setExtraLives(context.card().getExtraLives() - 1);
        context.card().setFormIndex(context.card().getFormIndex() + 1);
        context.card().setCurrentHealth(context.healthForForm(context.card().getFormIndex()));
        context.match().getLogs().add(context.definition().getName() + " 触发了额外命效果。");
        return true;
    }
}
