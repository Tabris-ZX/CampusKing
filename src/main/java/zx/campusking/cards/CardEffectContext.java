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
import java.util.Collections;
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
     * 让技能使用者抽指定数量的牌。
     */
    public int drawCards(PlayerState targetPlayer, int count) {
        int drawCount = Math.max(0, count);
        int before = targetPlayer.getHand().size();
        for (int index = 0; index < drawCount; index += 1) {
            deckService.drawOne(match, targetPlayer);
        }
        return Math.max(0, targetPlayer.getHand().size() - before);
    }

    /**
     * 让技能使用者获得指定数量行动点，可超过回合上限。
     */
    public void gainActionPoints(int count) {
        int gain = Math.max(0, count);
        player.setActionPoints(player.getActionPoints() + gain);
        match.getLogs().add(player.getName() + " 获得了 " + gain + " 点行动点.");
    }

    /**
     * 对技能使用者造成伤害。
     */
    public void damageSelf(int damage) {
        player.setHp(Math.max(0, player.getHp() - Math.max(0, damage)));
        match.getLogs().add(player.getName() + " 受到了 " + Math.max(0, damage) + " 点伤害.");
    }

    /**
     * 给目标角色添加一个状态效果。
     */
    public void applyCardBuff(CardInstance target, StatusEffectType type, int value, int stacks, Integer remainingTurns) {
        statusEffectService.applyOrRefreshEffect(target, new StatusEffect(type, "buff", Math.max(0, value), Math.max(1, stacks), remainingTurns, definition.getId()));
    }

    /**
     * 给目标玩家场上每名角色添加一个状态效果。
     */
    public void applyBoardBuff(PlayerState targetPlayer, StatusEffectType type, int value, int stacks, Integer remainingTurns) {
        for (CardInstance card : targetPlayer.getBoard()) {
            applyCardBuff(card, type, value, stacks, remainingTurns);
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
     * 治疗目标角色。
     */
    public void healCharacter(PlayerState owner, CardInstance target, int amount) {
        int heal = Math.max(0, amount);
        int maxHealth = statusEffectService.effectiveMaxHealth(owner, target);
        target.setCurrentHealth(Math.min(maxHealth, target.getCurrentHealth() + heal));
        match.getLogs().add(battleService.cardName(target) + " 恢复了 " + heal + " 点生命.");
    }

    /**
     * 对目标角色造成伤害，并处理死亡清理和击败奖励。
     */
    public void damageCharacter(PlayerState owner, CardInstance target, int amount) {
        if (owner.getPlayerId().equals(enemy.getPlayerId()) && statusEffectService.consumeActionPrevention(enemy, match, PreventableAction.SKILL_CARD, definition.getName())) {
            return;
        }
        target.setCurrentHealth(target.getCurrentHealth() - Math.max(0, amount));
        battleService.cleanupDefeated(match, owner, player, deckService);
        match.getLogs().add(battleService.cardName(target) + " 受到了 " + Math.max(0, amount) + " 点伤害.");
    }

    /**
     * 读取本次技能请求指定的场上角色。
     */
    public CardInstance targetBoardCard(PlayerState owner) {
        if (request.getTargetInstanceId() == null || request.getTargetInstanceId().isBlank()) {
            throw new IllegalArgumentException("需要指定目标角色。");
        }
        return matchSupportService.requireBoardCard(owner, request.getTargetInstanceId());
    }

    /**
     * 判断本次技能请求是否指定了使用者自己的角色。
     */
    public boolean targetsSelfBoard() {
        return player.getPlayerId().equals(request.getTargetPlayerId());
    }

    /**
     * 判断本次技能请求是否指定了对手的角色。
     */
    public boolean targetsEnemyBoard() {
        return enemy.getPlayerId().equals(request.getTargetPlayerId());
    }

    /**
     * 对目标玩家场上全部角色造成伤害。
     */
    public void damageBoard(PlayerState owner, int amount) {
        if (owner.getPlayerId().equals(enemy.getPlayerId()) && statusEffectService.consumeActionPrevention(enemy, match, PreventableAction.SKILL_CARD, definition.getName())) {
            return;
        }
        for (CardInstance target : owner.getBoard()) {
            target.setCurrentHealth(target.getCurrentHealth() - Math.max(0, amount));
        }
        battleService.cleanupDefeated(match, owner, player, deckService);
    }

    /**
     * 从目标玩家手牌中弃置指定数量的牌。
     *
     * @param targetPlayer 被弃牌玩家
     * @param requestedIds 指定弃置的手牌 id；为空时按 random 决定随机或从左到右
     * @param count 弃置数量上限
     * @param random 未指定 requestedIds 时是否随机弃置
     * @return 实际弃置的牌
     */
    public List<CardInstance> discardHand(PlayerState targetPlayer, List<String> requestedIds, int count, boolean random) {
        int discardLimit = Math.max(0, count);
        if (discardLimit <= 0 || targetPlayer.getHand().isEmpty()) {
            return List.of();
        }
        List<String> discardIds = requestedIds == null ? List.of() : requestedIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(discardLimit)
                .toList();
        if (discardIds.isEmpty()) {
            List<CardInstance> candidates = new ArrayList<>(targetPlayer.getHand());
            if (random) {
                Collections.shuffle(candidates);
            }
            discardIds = candidates.stream()
                    .limit(Math.min(discardLimit, candidates.size()))
                    .map(CardInstance::getInstanceId)
                    .toList();
        }

        List<CardInstance> discarded = new ArrayList<>();
        for (String discardId : discardIds) {
            if (discardId == null || discardId.isBlank()) {
                continue;
            }
            CardInstance card = matchSupportService.removeFromHand(targetPlayer, discardId);
            discarded.add(card);
            match.getDiscardPile().add(card);
        }
        if (!discarded.isEmpty()) {
            match.getLogs().add(targetPlayer.getName() + " 弃置了 " + discarded.size() + " 张手牌: " + cardNames(discarded) + ".");
        }
        return discarded;
    }

    private String cardNames(List<CardInstance> cards) {
        return cards.stream()
                .map(battleService::cardName)
                .toList()
                .toString()
                .replace("[", "")
                .replace("]", "");
    }
}
