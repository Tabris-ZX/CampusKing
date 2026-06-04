package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;
import zx.campusking.model.CardDefinition;
import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.service.TurnStartCharacterService;

public final class LcyCard extends BaseCharacterCard {

    public static final String ID = "lcy";
    public static final int ORDER = 125;

    public LcyCard(CardDefinition definition) {
        super(ORDER, definition);
    }

    @Override
    public void onTurnStart(
            MatchState match,
            PlayerState owner,
            CardInstance card,
            TurnStartCharacterService turnStartCharacterService
    ) {
        turnStartCharacterService.replayLastActiveSkill(match, owner, card);
    }
}
