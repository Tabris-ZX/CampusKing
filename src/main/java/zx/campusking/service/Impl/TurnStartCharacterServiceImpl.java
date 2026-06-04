package zx.campusking.service.Impl;

import zx.campusking.service.*;

import org.springframework.stereotype.Service;
import zx.campusking.model.CardInstance;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardType;
import zx.campusking.model.GamePhase;
import zx.campusking.model.LastPlayedSkill;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.dto.PlayEffectRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class TurnStartCharacterServiceImpl implements TurnStartCharacterService {

    private final CardCatalogService cardCatalogService;
    private final MatchSupportService matchSupportService;
    private final SkillResolverService skillResolverService;

    public TurnStartCharacterServiceImpl(
            CardCatalogService cardCatalogService,
            MatchSupportService matchSupportService,
            SkillResolverService skillResolverService
    ) {
        this.cardCatalogService = cardCatalogService;
        this.matchSupportService = matchSupportService;
        this.skillResolverService = skillResolverService;
    }

    public void trigger(MatchState match, PlayerState player) {
        if (match.getPhase() == GamePhase.FINISHED) {
            return;
        }
        for (CardInstance card : new ArrayList<>(player.getBoard())) {
            cardCatalogService.requireCard(card.getCardId())
                    .onTurnStart(match, player, card, this);
            if (match.getPhase() == GamePhase.FINISHED) {
                return;
            }
        }
    }

    public void replayLastActiveSkill(MatchState match, PlayerState player, CardInstance source) {
        LastPlayedSkill lastSkill = lastPlayedSkill(match, player.getPlayerId());
        if (lastSkill == null || lastSkill.getRoundNumber() != match.getRoundNumber() || lastSkill.getTurn() != match.getTurn() - 2) {
            return;
        }
        CardDefinition definition = cardCatalogService.require(lastSkill.getCardId());
        if (definition.getType() != CardType.SKILL) {
            return;
        }
        PlayerState enemy = matchSupportService.requireOpponent(match, player.getPlayerId());
        PlayEffectRequest replayRequest = replayRequest(player, lastSkill);
        String sourceName = cardCatalogService.require(source.getCardId()).getName();
        if (!skillResolverService.canResolveEffect(match, player, enemy, definition, replayRequest)) {
            match.getLogs().add(sourceName + " 尝试再次打出 " + definition.getName() + ", 但当前条件不合法.");
            return;
        }
        if (skillResolverService.consumeSkillPrevention(enemy, match, definition)) {
            match.getLogs().add(sourceName + " 免费再次打出 " + definition.getName() + ", " + enemy.getName() + " 抵御了该技能.");
            return;
        }
        skillResolverService.resolveEffect(match, player, enemy, definition, replayRequest);
        match.getLogs().add(sourceName + " 触发特性, 免费再次打出 " + definition.getName() + ".");
    }

    private LastPlayedSkill lastPlayedSkill(MatchState match, String playerId) {
        return match.getLastPlayedSkills().stream()
                .filter(record -> playerId.equals(record.getPlayerId()))
                .findFirst()
                .orElse(null);
    }

    private PlayEffectRequest replayRequest(PlayerState player, LastPlayedSkill lastSkill) {
        PlayEffectRequest request = new PlayEffectRequest();
        request.setPlayerId(player.getPlayerId());
        request.setTargetPlayerId(lastSkill.getTargetPlayerId());
        request.setTargetInstanceId(lastSkill.getTargetInstanceId());
        request.setDiscardInstanceIds(sanitizedIds(lastSkill.getDiscardInstanceIds()));
        return request;
    }

    private List<String> sanitizedIds(List<String> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

}
