package zx.campusking.service.Impl;

import zx.campusking.service.*;

import lombok.Getter;
import org.springframework.stereotype.Service;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.cards.GameCard;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.PreventableAction;
import zx.campusking.model.dto.PlayEffectRequest;

/**
 * 技能结算服务。
 * 负责组装通用结算上下文，并把具体效果委托给对应卡牌类。
 */
@Service
public class SkillResolverServiceImpl implements SkillResolverService {

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

    public SkillResolverServiceImpl(
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
    public boolean consumeSkillPrevention(PlayerState defender, MatchState match, CardDefinition definition) {
        if (cardCatalogService.requireCard(definition.getId()).bypassesNegate()) {
            return false;
        }
        return statusEffectService.consumeActionPrevention(defender, match, PreventableAction.SKILL_CARD, definition.getName());
    }

    public boolean canResolveEffect(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request) {
        GameCard card = cardCatalogService.requireCard(definition.getId());
        return card.canResolveSkill(effectContext(match, player, enemy, definition, request));
    }

    /**
     * 技能结算主入口。
     */
    public void resolveEffect(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request) {
        GameCard card = cardCatalogService.requireCard(definition.getId());
        card.resolveSkill(effectContext(match, player, enemy, definition, request));
    }

    private CardEffectContext effectContext(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request) {
        return new CardEffectContext(
                match,
                player,
                enemy,
                definition,
                request,
                statusEffectService,
                matchSupportService,
                battleService,
                deckService
        );
    }
}
