package zx.campusking.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.EffectType;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.SkillRange;
import zx.campusking.model.StatusEffect;
import zx.campusking.model.StatusEffectType;
import zx.campusking.model.dto.PlayEffectRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能结算服务。
 * 负责按技能类型和作用范围分发到具体处理逻辑。
 */
@Service
public class SkillResolverService {

    /** 卡牌目录服务，用于读取技能静态定义。 */
    @Getter
    private final CardCatalogService cardCatalogService;
    /** 状态效果服务，用于增加或消耗 Buff / Debuff。 */
    private final StatusEffectService statusEffectService;
    /** 对局辅助服务，用于定位场上目标。 */
    private final MatchSupportService matchSupportService;
    /** 战斗服务，用于通用角色结算与日志。 */
    private final BattleService battleService;
    /** 牌堆服务，用于某些技能触发抽牌。 */
    private final DeckService deckService;

    public SkillResolverService(
            CardCatalogService cardCatalogService,
            StatusEffectService statusEffectService,
            MatchSupportService matchSupportService,
            BattleService battleService,
            DeckService deckService
    ) {
        this.cardCatalogService = cardCatalogService;
        this.statusEffectService = statusEffectService;
        this.matchSupportService = matchSupportService;
        this.battleService = battleService;
        this.deckService = deckService;
    }

    /**
     * 尝试消耗一层“下一张技能无效”的反制效果。
     * 反制牌本身不会被反制。
     */
    public boolean consumeNegateSkill(PlayerState defender, MatchState match, CardDefinition definition) {
        if (definition.getEffectType() == EffectType.COUNTER_EFFECT) {
            return false;
        }
        return statusEffectService.consumeNegateSkill(defender, match);
    }

    /**
     * 技能结算主入口。
     */
    public void resolveEffect(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request) {
        EffectType effectType = definition.getEffectType() == null ? EffectType.NONE : definition.getEffectType();
        int value = definition.getEffectValue() == null ? 0 : definition.getEffectValue();
        SkillRange skillRange = definition.getSkillRange() == null ? SkillRange.BOTH : definition.getSkillRange();

        switch (effectType) {
            case HEAL_BOTH -> healSingleTarget(match, player, enemy, request, value);
            case DAMAGE_ALL_ENEMIES -> {
                if (skillRange == SkillRange.SINGLE) {
                    damageSingleTarget(match, player, enemy, request, value);
                    break;
                }
                if (!statusEffectService.consumeShield(enemy, match, enemy.getName())) {
                    for (CardInstance target : enemy.getBoard()) {
                        target.setCurrentHealth(target.getCurrentHealth() - value);
                    }
                    battleService.cleanupDefeated(match, enemy);
                }
            }
            case GLOBAL_BUFF -> {
                applyGlobalBuff(player, definition, value);
                if (skillRange == SkillRange.BOTH) {
                    applyGlobalBuff(enemy, definition, value);
                }
            }
            case SHIELD -> {
                int duration = definition.getEffectDuration() == null ? 1 : definition.getEffectDuration();
                statusEffectService.applyOrRefreshEffect(player, new StatusEffect(StatusEffectType.SHIELD, "buff", 0, 1, duration, definition.getId()));
                statusEffectService.applyOrRefreshEffect(player, new StatusEffect(StatusEffectType.BLOCK_DAMAGE, "buff", 0, 1, duration, definition.getId()));
            }
            case COUNTER_EFFECT ->
                    statusEffectService.applyOrRefreshEffect(
                            player,
                            new StatusEffect(
                                    StatusEffectType.NEGATE_NEXT_SKILL,
                                    "debuff",
                                    0,
                                    1,
                                    definition.getEffectDuration() == null ? 1 : definition.getEffectDuration(),
                                    definition.getId()
                            )
                    );
            case DRAW_AND_MODIFY_UNIT -> {
                deckService.drawOne(match, player);
                deckService.drawOne(match, player);
                modifyUnit(match, player, enemy, request, value);
            }
            case REVIVE_ALLY -> applyReviveOnDeath(player, definition, value);
            case DISCARD_AND_DRAW -> discardAndDraw(match, player, request);
            case DISCARD_ENEMY_HAND -> discardEnemyHand(match, enemy);
            case NONE -> {
            }
        }
    }

    private void applyGlobalBuff(PlayerState player, CardDefinition definition, int value) {
        int duration = definition.getEffectDuration() == null ? 2 : definition.getEffectDuration();
        statusEffectService.applyOrRefreshEffect(player, new StatusEffect(StatusEffectType.ATTACK_UP, "buff", value, 1, duration, definition.getId()));
        statusEffectService.applyOrRefreshEffect(player, new StatusEffect(StatusEffectType.MAX_HP_UP, "buff", value, 1, duration, definition.getId()));
        statusEffectService.applyOrRefreshEffect(player, new StatusEffect(StatusEffectType.TURN_HEAL, "buff", value, 1, duration, definition.getId()));
        int maxHp = PlayerState.MAX_HP + statusEffectService.sumEffectValue(player, StatusEffectType.MAX_HP_UP);
        player.setHp(Math.min(maxHp, player.getHp() + value));
    }

    /**
     * 单体治疗：目标可以是玩家，也可以是角色。
     */
    private void healSingleTarget(MatchState match, PlayerState player, PlayerState enemy, PlayEffectRequest request, int value) {
        if (request.getTargetPlayerId() == null) {
            throw new IllegalArgumentException("单体技能需要指定目标玩家。");
        }
        PlayerState targetPlayer = request.getTargetPlayerId().equals(player.getPlayerId()) ? player : enemy;
        if (request.getTargetInstanceId() == null || request.getTargetInstanceId().isBlank()) {
            int maxHp = PlayerState.MAX_HP + statusEffectService.sumEffectValue(targetPlayer, StatusEffectType.MAX_HP_UP);
            targetPlayer.setHp(Math.min(maxHp, targetPlayer.getHp() + value));
            match.getLogs().add(targetPlayer.getName() + " 恢复了 " + value + " 点生命。");
            return;
        }

        CardInstance target = matchSupportService.requireBoardCard(targetPlayer, request.getTargetInstanceId());
        int maxHealth = statusEffectService.effectiveMaxHealth(targetPlayer, target);
        target.setCurrentHealth(Math.min(maxHealth, target.getCurrentHealth() + value));
        match.getLogs().add(battleService.cardName(target) + " 恢复了 " + value + " 点生命。");
    }

    /**
     * 单体伤害：目标可以是玩家，也可以是角色。
     */
    private void damageSingleTarget(MatchState match, PlayerState player, PlayerState enemy, PlayEffectRequest request, int value) {
        if (request.getTargetPlayerId() == null) {
            throw new IllegalArgumentException("单体技能需要指定目标玩家。");
        }
        PlayerState targetPlayer = request.getTargetPlayerId().equals(player.getPlayerId()) ? player : enemy;
        if (request.getTargetInstanceId() == null || request.getTargetInstanceId().isBlank()) {
            if (!statusEffectService.consumeShield(targetPlayer, match, targetPlayer.getName())) {
                targetPlayer.setHp(Math.max(0, targetPlayer.getHp() - value));
            }
            match.getLogs().add(targetPlayer.getName() + " 受到了 " + value + " 点伤害。");
            return;
        }

        CardInstance target = matchSupportService.requireBoardCard(targetPlayer, request.getTargetInstanceId());
        if (!statusEffectService.consumeShield(targetPlayer, match, battleService.cardName(target))) {
            target.setCurrentHealth(target.getCurrentHealth() - value);
        }
        battleService.cleanupDefeated(match, targetPlayer);
        match.getLogs().add(battleService.cardName(target) + " 受到了 " + value + " 点伤害。");
    }

    private void applyReviveOnDeath(PlayerState player, CardDefinition definition, int value) {
        int duration = definition.getEffectDuration() == null ? 2 : definition.getEffectDuration();
        statusEffectService.applyOrRefreshEffect(
                player,
                new StatusEffect(StatusEffectType.REVIVE_ON_DEATH, "buff", value, 1, duration, definition.getId())
        );
    }

    private void discardAndDraw(MatchState match, PlayerState player, PlayEffectRequest request) {
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
        for (int i = 0; i < discarded.size(); i++) {
            deckService.drawOne(match, player);
        }
        match.getLogs().add(player.getName() + " 弃置了 " + discarded.size() + " 张牌并抽取等量卡牌。");
    }

    private void discardEnemyHand(MatchState match, PlayerState enemy) {
        if (enemy.getHand().isEmpty()) {
            match.getLogs().add(enemy.getName() + " 没有手牌可弃置。");
            return;
        }
        CardInstance discarded = enemy.getHand().remove(0);
        match.getDiscardPile().add(discarded);
        match.getLogs().add(enemy.getName() + " 被弃置了 1 张手牌。");
    }

    private void modifyUnit(MatchState match, PlayerState player, PlayerState enemy, PlayEffectRequest request, int value) {
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
            target.setCurrentHealth(Math.min(maxHealth, target.getCurrentHealth() + value));
            match.getLogs().add(battleService.cardName(target) + " 恢复了 " + value + " 点生命。");
            return;
        }
        CardInstance target = matchSupportService.requireBoardCard(enemy, request.getTargetInstanceId());
        if (!statusEffectService.consumeShield(enemy, match, battleService.cardName(target))) {
            target.setCurrentHealth(target.getCurrentHealth() - value);
        }
        battleService.cleanupDefeated(match, enemy);
        match.getLogs().add(battleService.cardName(target) + " 受到了 " + value + " 点伤害。");
    }

}
