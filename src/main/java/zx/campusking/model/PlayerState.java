package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PlayerState {

    public static final int MAX_HP = 100;
    public static final int SUMMON_SLOTS = 3;

    private final String playerId;
    private final String name;
    private final String playerToken;
    private int hp = MAX_HP;
    private int summonsThisTurn;
    private final List<StatusEffect> statusEffects = new ArrayList<>();
    private final List<CardInstance> hand = new ArrayList<>();
    private final List<CardInstance> board = new ArrayList<>();

    public PlayerState(String playerId, String name, String playerToken) {
        this.playerId = playerId;
        this.name = name;
        this.playerToken = playerToken;
    }

}
