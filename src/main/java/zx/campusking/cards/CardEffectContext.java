package zx.campusking.cards;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.PreventableAction;
import zx.campusking.model.StatusEffect;
import zx.campusking.model.StatusEffectType;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.service.BattleService;
import zx.campusking.service.DeckService;
import zx.campusking.service.MatchSupportService;
import zx.campusking.service.StatusEffectService;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能牌结算上下文。
 * 传给 {@link GameCard#resolveSkill(CardEffectContext)} 和 {@link GameCard#canResolveSkill(CardEffectContext)}，
 * 集中暴露对局、双方玩家、请求参数以及少量通用规则操作，让具体技能类不用直接耦合 GameService。
 */
public record CardEffectContext(
        MatchState match,
        PlayerState player,
        PlayerState enemy,
        CardDefinition definition,
        PlayEffectRequest request,
        StatusEffectService statusEffectService,
        MatchSupportService matchSupportService,
        BattleService battleService,
        DeckService deckService
) {

    /**
     * 返回当前技能定义上的效果数值，未配置时按 0 处理。
     */
    public int value() {
        return definition.getEffectValue() == null ? 0 : definition.getEffectValue();
    }

    /**
     * 返回当前技能定义上的持续回合，未配置时使用调用方传入的默认值。
     */
    public int duration(int fallback) {
        return definition.getEffectDuration() == null ? fallback : definition.getEffectDuration();
    }

    /**
     * 让技能使用者抽 1 张牌。
     */
    public void drawOne() {
        deckService.drawOne(match, player);
    }

    /**
     * 让技能使用者获得 1 点行动点，可超过回合上限。
     */
    public void gainActionPoint() {
        player.setActionPoints(player.getActionPoints() + 1);
        match.getLogs().add(player.getName() + " 获得了 1 点行动点.");
    }

    /**
     * 对技能使用者造成伤害。
     */
    public void damageSelf(int damage) {
        player.setHp(Math.max(0, player.getHp() - Math.max(0, damage)));
        match.getLogs().add(player.getName() + " 受到了 " + Math.max(0, damage) + " 点伤害.");
    }

    /**
     * 把圣域类全局增益分发给目标玩家场上的每名角色。
     */
    public void applyGlobalBuff(PlayerState target) {
        int value = value();
        int duration = duration(2);
        for (CardInstance card : target.getBoard()) {
            statusEffectService.applyOrRefreshEffect(card, new StatusEffect(StatusEffectType.ATTACK_UP, "buff", value, 1, duration, definition.getId()));
            statusEffectService.applyOrRefreshEffect(card, new StatusEffect(StatusEffectType.MAX_HP_UP, "buff", value, 1, duration, definition.getId()));
            statusEffectService.applyOrRefreshEffect(card, new StatusEffect(StatusEffectType.TURN_HEAL, "buff", value, 1, duration, definition.getId()));
            int maxHealth = statusEffectService.effectiveMaxHealth(target, card);
            card.setCurrentHealth(Math.min(maxHealth, card.getCurrentHealth() + value));
        }
    }

    /**
     * 给技能使用者添加“抵御下一次指定动作”的持续效果。
     */
    public void applyPrevention(PreventableAction action) {
        statusEffectService.applyPreventNextAction(
                player,
                new StatusEffect(StatusEffectType.PREVENT_NEXT_ACTION, "buff", action.ordinal(), 1, duration(1), definition.getId())
        );
    }

    /**
     * 给技能使用者添加角色死亡后复活一次的持续效果。
     */
    public void applyReviveOnDeath() {
        statusEffectService.applyOrRefreshEffect(
                player,
                new StatusEffect(StatusEffectType.REVIVE_ON_DEATH, "buff", value(), 1, duration(2), definition.getId())
        );
    }

    /**
     * 对敌方场上全部角色造成当前技能数值的伤害。
     */
    public void damageAllEnemies() {
        if (statusEffectService.consumeActionPrevention(enemy, match, PreventableAction.SKILL_CARD, definition.getName())) {
            return;
        }
        for (CardInstance target : enemy.getBoard()) {
            target.setCurrentHealth(target.getCurrentHealth() - value());
        }
        battleService.cleanupDefeated(match, enemy, player, deckService);
    }

    /**
     * 汽水类单体角色修改：目标是己方角色时治疗，目标是敌方角色时造成伤害。
     */
    public void modifyUnit() {
        if (request.getTargetPlayerId() == null || request.getTargetPlayerId().isBlank()) {
            throw new IllegalArgumentException("汽水需要指定目标角色。");
        }
        PlayerState targetPlayer = request.getTargetPlayerId().equals(player.getPlayerId()) ? player : enemy;
        if (request.getTargetInstanceId() == null || request.getTargetInstanceId().isBlank()) {
            throw new IllegalArgumentException("汽水只能指定场上的角色。");
        }
        if (targetPlayer.getPlayerId().equals(player.getPlayerId())) {
            CardInstance target = matchSupportService.requireBoardCard(player, request.getTargetInstanceId());
            int maxHealth = statusEffectService.effectiveMaxHealth(player, target);
            target.setCurrentHealth(Math.min(maxHealth, target.getCurrentHealth() + value()));
            match.getLogs().add(battleService.cardName(target) + " 恢复了 " + value() + " 点生命.");
            return;
        }

        CardInstance target = matchSupportService.requireBoardCard(enemy, request.getTargetInstanceId());
        if (!statusEffectService.consumeActionPrevention(enemy, match, PreventableAction.SKILL_CARD, definition.getName())) {
            target.setCurrentHealth(target.getCurrentHealth() - value());
        }
        battleService.cleanupDefeated(match, enemy, player, deckService);
        match.getLogs().add(battleService.cardName(target) + " 受到了 " + value() + " 点伤害.");
    }

    /**
     * 弃置使用者指定的至多 3 张手牌，然后抽取等量卡牌。
     */
    public void discardAndDraw() {
        List<String> discardIds = request.getDiscardInstanceIds() == null ? List.of() : request.getDiscardInstanceIds();
        if (discardIds.size() > 3) {
            throw new IllegalArgumentException("最多只能弃置 3 张牌。");
        }
        if (discardIds.isEmpty()) {
            discardIds = player.getHand().stream()
                    .limit(Math.min(3, player.getHand().size()))
                    .map(CardInstance::getInstanceId)
                    .toList();
        }

        List<CardInstance> discarded = new ArrayList<>();
        for (String discardId : discardIds) {
            if (discardId == null || discardId.isBlank()) {
                continue;
            }
            CardInstance card = matchSupportService.removeFromHand(player, discardId);
            discarded.add(card);
            match.getDiscardPile().add(card);
        }
        for (int index = 0; index < discarded.size(); index += 1) {
            deckService.drawOne(match, player);
        }
        match.getLogs().add(player.getName() + " 弃置了 " + discarded.size() + " 张牌并抽取等量卡牌.");
    }

    /**
     * 弃置敌方第一张手牌。
     */
    public void discardEnemyHand() {
        if (enemy.getHand().isEmpty()) {
            match.getLogs().add(enemy.getName() + " 没有手牌可弃置.");
            return;
        }
        CardInstance discarded = enemy.getHand().remove(0);
        match.getDiscardPile().add(discarded);
        match.getLogs().add(enemy.getName() + " 被弃置了 1 张手牌.");
    }
}
