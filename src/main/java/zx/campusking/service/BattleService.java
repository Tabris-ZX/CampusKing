package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.cards.CardCombatContext;
import zx.campusking.cards.CardDefeatContext;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardRarity;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.StatusEffectType;

import java.util.List;

/**
 * 战斗结算服务。
 * 负责攻击资格判断、攻击力计算、伤害结算，以及角色死亡后的清理逻辑。
 */
@Service
public class BattleService {

    /** 卡牌静态目录服务，用于读取基础属性和特性。 */
    private final CardCatalogService cardCatalogService;
    /** 状态效果服务，用于读取攻击增益等战斗修正。 */
    private final StatusEffectService statusEffectService;

    public BattleService(CardCatalogService cardCatalogService, StatusEffectService statusEffectService) {
        this.cardCatalogService = cardCatalogService;
        this.statusEffectService = statusEffectService;
    }

    /**
     * 判断当前角色是否允许发起攻击。
     * 规则：
     * 1. 处于休整状态时不能攻击
     * 2. 先手玩家首回合不能使用召唤区角色攻击
     */
    public void ensureCanAttack(MatchState match, CardInstance attacker) {
        if (attacker.isSleeping()) {
            throw new IllegalStateException("该角色本回合不能攻击。");
        }
        if (match.getTurn() == 1) {
            throw new IllegalStateException("先手玩家首回合不能使用召唤区角色攻击。");
        }
    }

    /**
     * 计算角色当前攻击力。
     * 会叠加攻击 Buff，并处理龙骑按已损失生命追加伤害的特性。
     */
    public int computeAttack(PlayerState owner, CardInstance cardInstance) {
        CardDefinition definition = cardCatalogService.require(cardInstance.getCardId());
        int attack = baseAttack(definition, cardInstance);
        attack += statusEffectService.sumEffectValue(owner, StatusEffectType.ATTACK_UP);
        attack += statusEffectService.sumEffectValue(cardInstance, StatusEffectType.ATTACK_UP);
        return cardCatalogService.requireCard(definition.getId())
                .modifyAttack(new CardCombatContext(owner, cardInstance, definition), attack);
    }

    /**
     * 对角色造成伤害并写入日志。
     */
    public boolean damageCharacter(MatchState match, PlayerState sourceOwner, PlayerState targetOwner, CardInstance source, CardInstance target, int damage) {
        if (target.getCurrentHealth() <= 0 || damage <= 0) {
            return false;
        }
        int actualDamage = Math.max(0, Math.min(damage, target.getCurrentHealth()));
        target.setCurrentHealth(target.getCurrentHealth() - damage);
        recordDamage(sourceOwner, targetOwner, actualDamage);
        match.getLogs().add(cardName(source) + " 对 " + cardName(target) + " 造成了 " + actualDamage + " 点伤害.");
        return true;
    }

    public void recordDamage(PlayerState sourceOwner, PlayerState targetOwner, int damage) {
        int actualDamage = Math.max(0, damage);
        if (actualDamage <= 0) {
            return;
        }
        if (sourceOwner != null) {
            sourceOwner.setDamageDealt(sourceOwner.getDamageDealt() + actualDamage);
        }
        if (targetOwner != null) {
            targetOwner.setDamageTaken(targetOwner.getDamageTaken() + actualDamage);
        }
    }

    /**
     * 清理被击败角色。
     * 角色自身的死亡特性优先处理，然后再处理通用复活 Buff。
     */
    public void cleanupDefeated(MatchState match, PlayerState player) {
        cleanupDefeated(match, player, null, null);
    }

    /**
     * 清理被击败角色，并按被击败角色稀有度给奖励归属玩家发放奖励。
     */
    public void cleanupDefeated(MatchState match, PlayerState player, PlayerState rewardOwner, DeckService deckService) {
        List<CardInstance> defeated = player.getBoard().stream()
                .filter(card -> card.getCurrentHealth() <= 0)
                .toList();

        for (CardInstance card : defeated) {
            CardDefinition definition = cardCatalogService.require(card.getCardId());

            if (cardCatalogService.requireCard(definition.getId())
                    .handleDefeated(new CardDefeatContext(match, player, card, definition))) {
                continue;
            }

            var reviveEffect = statusEffectService.consumeReviveOnDeath(player, match);
            if (reviveEffect != null) {
                int reviveHealth = Math.max(1, baseHealth(definition, card) / Math.max(1, reviveEffect.getValue()));
                card.setCurrentHealth(reviveHealth);
                card.setSleeping(true);
                match.getLogs().add(definition.getName() + " 死亡后回复到 " + reviveHealth + " 点体力.");
                continue;
            }

            player.getBoard().remove(card);
            match.getDiscardPile().add(card);
            match.getLogs().add(definition.getName() + " 被击败.");
            grantDefeatReward(match, rewardOwner, definition, deckService);
        }
    }

    /**
     * 读取实例对应的中文卡牌名。
     */
    public String cardName(CardInstance instance) {
        return cardCatalogService.require(instance.getCardId()).getName();
    }

    private int baseAttack(CardDefinition definition, CardInstance card) {
        if (card.getFormIndex() > 0 && definition.getSecondaryAttack() != null) {
            return definition.getSecondaryAttack();
        }
        return definition.getAttack() == null ? 0 : definition.getAttack();
    }

    private int baseHealth(CardDefinition definition, CardInstance card) {
        if (card.getFormIndex() > 0 && definition.getSecondaryHealth() != null) {
            return definition.getSecondaryHealth();
        }
        return definition.getHealth() == null ? 0 : definition.getHealth();
    }

    private void grantDefeatReward(MatchState match, PlayerState rewardOwner, CardDefinition defeatedDefinition, DeckService deckService) {
        if (rewardOwner == null || deckService == null || defeatedDefinition.getRarity() == null) {
            return;
        }
        deckService.drawOne(match, rewardOwner);
        match.getLogs().add(rewardOwner.getName() + " 击败角色, 摸了 1 张牌.");
        if (defeatedDefinition.getRarity() == CardRarity.RARE) {
            rewardOwner.setActionPoints(rewardOwner.getActionPoints() + 1);
            match.getLogs().add(rewardOwner.getName() + " 击败稀有角色, 获得了 1 点行动点.");
        }
    }
}
