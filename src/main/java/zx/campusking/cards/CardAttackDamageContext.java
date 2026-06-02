package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.service.BattleService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 角色攻击实际造成伤害后的结算上下文。
 */
public record CardAttackDamageContext(
        MatchState match,
        PlayerState attackerOwner,
        PlayerState defenderOwner,
        CardInstance attacker,
        CardDefinition attackerDefinition,
        int damage,
        BattleService battleService
) {

    public List<CardInstance> discardRandomDefenderHand(int count) {
        int discardCount = Math.max(0, count);
        if (discardCount <= 0 || defenderOwner.getHand().isEmpty()) {
            return List.of();
        }

        List<CardInstance> discarded = new ArrayList<>();
        for (int index = 0; index < discardCount && !defenderOwner.getHand().isEmpty(); index += 1) {
            int handIndex = ThreadLocalRandom.current().nextInt(defenderOwner.getHand().size());
            CardInstance card = defenderOwner.getHand().remove(handIndex);
            match.getDiscardPile().add(card);
            discarded.add(card);
        }
        return discarded;
    }

    public String cardName(CardInstance card) {
        return battleService.cardName(card);
    }
}
