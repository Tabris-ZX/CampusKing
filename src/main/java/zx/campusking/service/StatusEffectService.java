package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.StatusEffect;
import zx.campusking.model.StatusEffectType;

@Service
public class StatusEffectService {

    private final CardCatalogService cardCatalogService;

    public StatusEffectService(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public void applyTurnStartEffects(MatchState match, PlayerState player) {
        int healValue = sumEffectValue(player, StatusEffectType.TURN_HEAL);
        if (healValue <= 0) {
            return;
        }
        int maxPlayerHp = PlayerState.MAX_HP + sumEffectValue(player, StatusEffectType.MAX_HP_UP);
        player.setHp(Math.min(maxPlayerHp, player.getHp() + healValue));
        for (CardInstance card : player.getBoard()) {
            CardDefinition definition = cardCatalogService.require(card.getCardId());
            int maxHealth = effectiveMaxHealth(player, definition);
            card.setCurrentHealth(Math.min(maxHealth, card.getCurrentHealth() + healValue));
        }
        match.getLogs().add(player.getName() + " 触发了回合开始回血效果。");
    }

    public void tickStatusDurations(PlayerState player, MatchState match) {
        for (StatusEffect effect : player.getStatusEffects()) {
            if (effect.getRemainingTurns() != null) {
                effect.setRemainingTurns(effect.getRemainingTurns() - 1);
            }
        }
        removeExpiredEffects(player);
        match.getLogs().add(player.getName() + " 刷新了持续效果回合数。");
    }

    public void applyOrRefreshEffect(PlayerState player, StatusEffect incoming) {
        StatusEffect existing = findFirstEffect(player, incoming.getType());
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

    public StatusEffect findFirstEffect(PlayerState player, StatusEffectType type) {
        return player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .findFirst()
                .orElse(null);
    }

    public int sumEffectValue(PlayerState player, StatusEffectType type) {
        return player.getStatusEffects().stream()
                .filter(effect -> effect.getType() == type)
                .mapToInt(StatusEffect::getValue)
                .sum();
    }

    public int effectiveMaxHealth(PlayerState player, CardDefinition definition) {
        int base = definition.getHealth() == null ? 0 : definition.getHealth();
        return base + sumEffectValue(player, StatusEffectType.MAX_HP_UP);
    }

    public boolean consumeShield(PlayerState player, MatchState match, String targetName) {
        StatusEffect shield = findFirstEffect(player, StatusEffectType.SHIELD);
        if (shield == null) {
            return false;
        }
        shield.setStacks(Math.max(0, shield.getStacks() - 1));
        removeExpiredEffects(player);
        match.getLogs().add("护盾替 " + targetName + " 挡下了这次伤害。");
        return true;
    }

    public boolean consumeNegateSkill(PlayerState player, MatchState match) {
        StatusEffect negate = findFirstEffect(player, StatusEffectType.NEGATE_NEXT_SKILL);
        if (negate == null) {
            return false;
        }
        negate.setStacks(Math.max(0, negate.getStacks() - 1));
        removeExpiredEffects(player);
        match.getLogs().add(player.getName() + " 消耗了一层技能反制效果。");
        return true;
    }

    public void removeExpiredEffects(PlayerState player) {
        player.getStatusEffects().removeIf(effect ->
                (effect.getRemainingTurns() != null && effect.getRemainingTurns() <= 0)
                        || effect.getStacks() <= 0);
    }
}
