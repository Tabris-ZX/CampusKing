package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.PreventableAction;
import zx.campusking.model.StatusEffect;
import zx.campusking.model.StatusEffectType;

@Service
public class StatusEffectService {

    private final CardCatalogService cardCatalogService;

    public StatusEffectService(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public void applyTurnStartEffects(MatchState match, PlayerState player) {
        boolean healed = false;
        int playerHealValue = sumEffectValue(player, StatusEffectType.TURN_HEAL);
        if (playerHealValue > 0) {
            int maxPlayerHp = PlayerState.MAX_HP + sumEffectValue(player, StatusEffectType.MAX_HP_UP);
            player.setHp(Math.min(maxPlayerHp, player.getHp() + playerHealValue));
            healed = true;
        }
        for (CardInstance card : player.getBoard()) {
            int healValue = sumEffectValue(card, StatusEffectType.TURN_HEAL);
            if (healValue <= 0) {
                continue;
            }
            int maxHealth = effectiveMaxHealth(player, card);
            card.setCurrentHealth(Math.min(maxHealth, card.getCurrentHealth() + healValue));
            healed = true;
        }
        if (healed) {
            match.getLogs().add(player.getName() + " 触发了回合开始回血效果.");
        }
    }

    public void tickStatusDurations(PlayerState player, MatchState match) {
        for (StatusEffect effect : player.getStatusEffects()) {
            if (effect.getRemainingTurns() != null) {
                effect.setRemainingTurns(effect.getRemainingTurns() - 1);
            }
        }
        removeExpiredEffects(player);
        for (CardInstance card : player.getBoard()) {
            for (StatusEffect effect : card.getStatusEffects()) {
                if (effect.getRemainingTurns() != null) {
                    effect.setRemainingTurns(effect.getRemainingTurns() - 1);
                }
            }
            removeExpiredEffects(card);
        }
        match.getLogs().add(player.getName() + " 刷新了持续效果回合数.");
    }

    public void applyOrRefreshEffect(PlayerState player, StatusEffect incoming) {
        StatusEffect existing = findMergeTarget(player, incoming);
        if (existing == null) {
            player.getStatusEffects().add(incoming);
            return;
        }
        existing.setValue(Math.max(existing.getValue(), incoming.getValue()));
        existing.setStacks(existing.getStacks() + incoming.getStacks());
        if (incoming.getRemainingTurns() != null) {
            int current = existing.getRemainingTurns() == null ? 0 : existing.getRemainingTurns();
            existing.setRemainingTurns(Math.max(current, incoming.getRemainingTurns()));
        }
        existing.setSourceCardId(incoming.getSourceCardId());
    }

    public void applyOrRefreshEffect(CardInstance card, StatusEffect incoming) {
        StatusEffect existing = findFirstEffect(card, incoming.getType());
        if (existing == null) {
            card.getStatusEffects().add(incoming);
            return;
        }
        existing.setValue(Math.max(existing.getValue(), incoming.getValue()));
        existing.setStacks(existing.getStacks() + incoming.getStacks());
        if (incoming.getRemainingTurns() != null) {
            int current = existing.getRemainingTurns() == null ? 0 : existing.getRemainingTurns();
            existing.setRemainingTurns(Math.max(current, incoming.getRemainingTurns()));
        }
        existing.setSourceCardId(incoming.getSourceCardId());
    }

    public StatusEffect findFirstEffect(PlayerState player, StatusEffectType type) {
        return player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .findFirst()
                .orElse(null);
    }

    public StatusEffect findFirstEffect(CardInstance card, StatusEffectType type) {
        return card.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .findFirst()
                .orElse(null);
    }

    public int sumEffectValue(PlayerState player, StatusEffectType type) {
        return player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .mapToInt(this::stackedValue)
                .sum();
    }

    public int sumEffectValue(CardInstance card, StatusEffectType type) {
        return card.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .mapToInt(this::stackedValue)
                .sum();
    }

    public int effectiveMaxHealth(PlayerState player, CardDefinition definition) {
        int base = definition.getHealth() == null ? 0 : definition.getHealth();
        return base + sumEffectValue(player, StatusEffectType.MAX_HP_UP);
    }

    public int effectiveMaxHealth(PlayerState player, CardInstance card) {
        CardDefinition definition = cardCatalogService.require(card.getCardId());
        int base;
        if (card.getFormIndex() > 0 && definition.getSecondaryHealth() != null) {
            base = definition.getSecondaryHealth();
        } else {
            base = definition.getHealth() == null ? 0 : definition.getHealth();
        }
        return base
                + sumEffectValue(player, StatusEffectType.MAX_HP_UP)
                + sumEffectValue(card, StatusEffectType.MAX_HP_UP);
    }

    public void applyPreventNextAction(PlayerState player, StatusEffect incoming) {
        applyOrRefreshEffect(player, incoming);
    }

    public boolean consumeActionPrevention(PlayerState player, MatchState match, PreventableAction action, String actionName) {
        StatusEffect prevention = player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == StatusEffectType.PREVENT_NEXT_ACTION)
                .filter(effect -> effect.getValue() == action.ordinal())
                .findFirst()
                .orElse(null);
        if (prevention == null) {
            return false;
        }
        prevention.setStacks(Math.max(0, prevention.getStacks() - 1));
        removeExpiredEffects(player);
        match.getLogs().add(player.getName() + " 抵御了 " + actionName + ".");
        return true;
    }

    public StatusEffect consumeReviveOnDeath(PlayerState player, MatchState match) {
        StatusEffect revive = findFirstEffect(player, StatusEffectType.REVIVE_ON_DEATH);
        if (revive == null) {
            return null;
        }
        revive.setStacks(Math.max(0, revive.getStacks() - 1));
        removeExpiredEffects(player);
        match.getLogs().add(player.getName() + " 消耗了一层死亡复活效果.");
        return revive;
    }

    public void removeExpiredEffects(PlayerState player) {
        player.getStatusEffects().removeIf(effect ->
                (effect.getRemainingTurns() != null && effect.getRemainingTurns() <= 0)
                        || effect.getStacks() <= 0);
    }

    public void removeExpiredEffects(CardInstance card) {
        card.getStatusEffects().removeIf(effect ->
                (effect.getRemainingTurns() != null && effect.getRemainingTurns() <= 0)
                        || effect.getStacks() <= 0);
    }

    private StatusEffect findMergeTarget(PlayerState player, StatusEffect incoming) {
        return player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == incoming.getType())
                .filter(effect -> incoming.getType() != StatusEffectType.PREVENT_NEXT_ACTION || effect.getValue() == incoming.getValue())
                .findFirst()
                .orElse(null);
    }

    private int stackedValue(StatusEffect effect) {
        return effect.getValue() * Math.max(1, effect.getStacks());
    }
}
