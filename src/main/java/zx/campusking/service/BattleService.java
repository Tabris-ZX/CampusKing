package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
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
        int attack = definition.getAttack() == null ? 0 : definition.getAttack();
        attack += statusEffectService.sumEffectValue(owner, StatusEffectType.ATTACK_UP);
        if (hasTrait(definition, "dragon")) {
            attack += Math.max(0, definition.getHealth() - cardInstance.getCurrentHealth());
        }
        return attack;
    }

    /**
     * 对角色造成伤害并写入日志。
     */
    public void damageCharacter(MatchState match, CardInstance source, CardInstance target, int damage) {
        if (target.getCurrentHealth() <= 0) {
            return;
        }
        target.setCurrentHealth(target.getCurrentHealth() - damage);
        match.getLogs().add(cardName(source) + " 对 " + cardName(target) + " 造成了 " + damage + " 点伤害。");
    }

    /**
     * 清理被击败角色。
     * 包含龙骑首次死亡复活，以及鸟女额外命效果。
     */
    public void cleanupDefeated(MatchState match, PlayerState player) {
        List<CardInstance> defeated = player.getBoard().stream()
                .filter(card -> card.getCurrentHealth() <= 0)
                .toList();

        for (CardInstance card : defeated) {
            CardDefinition definition = cardCatalogService.require(card.getCardId());

            if (hasTrait(definition, "dragon") && !card.isRevived()) {
                card.setRevived(true);
                card.setCurrentHealth(1);
                match.getLogs().add(definition.getName() + " 首次被击败后回复到 1 点体力。");
                continue;
            }

            if (card.getExtraLives() > 0) {
                card.setExtraLives(card.getExtraLives() - 1);
                card.setCurrentHealth(definition.getHealth());
                match.getLogs().add(definition.getName() + " 触发了额外命效果。");
                continue;
            }

            player.getBoard().remove(card);
            match.getDiscardPile().add(card);
            match.getLogs().add(definition.getName() + " 被击败并进入墓地。");
        }
    }

    /**
     * 读取实例对应的中文卡牌名。
     */
    public String cardName(CardInstance instance) {
        return cardCatalogService.require(instance.getCardId()).getName();
    }

    private boolean hasTrait(CardDefinition definition, String trait) {
        return definition.getTraits() != null && definition.getTraits().contains(trait);
    }
}
