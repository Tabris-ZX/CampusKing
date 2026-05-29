package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter

public class MatchState {

    private String matchId;
    private String roomCode;
    private List<PlayerState> players = new ArrayList<>();
    private List<CardInstance> drawPile = new ArrayList<>();
    private List<CardInstance> discardPile = new ArrayList<>();
    private String currentPlayerId;
    private GamePhase phase;
    private int turn;
    private String winnerId;
    private boolean ready;
    private BotMode mode = BotMode.PVP;
    private List<String> logs = new ArrayList<>();

}
