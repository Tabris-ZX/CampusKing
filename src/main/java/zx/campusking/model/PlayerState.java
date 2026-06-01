package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 对局中的玩家状态。
 * 包含玩家身份、生命值、状态效果、手牌和召唤区。
 */
@Getter
@Setter
public class PlayerState {

    /** 玩家默认最大生命。 */
    public static final int MAX_HP = 100;
    /** 每名玩家的召唤区格子数。 */
    public static final int SUMMON_SLOTS = 3;

    /** 对局内玩家席位 id，例如 P1 / P2。 */
    private final String playerId;
    /** 玩家展示名称。 */
    private final String name;
    /** 浏览器会话 token，用于刷新后恢复玩家身份。 */
    private final String playerToken;
    /** 当前生命值。 */
    private int hp = MAX_HP;
    /** 本回合已召唤次数。 */
    private int summonsThisTurn;
    /** 玩家身上的持续状态效果。 */
    private final List<StatusEffect> statusEffects = new ArrayList<>();
    /** 玩家当前手牌。 */
    private final List<CardInstance> hand = new ArrayList<>();
    /** 玩家当前召唤区角色。 */
    private final List<CardInstance> board = new ArrayList<>();

    /**
     * 创建玩家状态。
     *
     * @param playerId 对局内席位 id
     * @param name 展示名称
     * @param playerToken 浏览器会话 token
     */
    public PlayerState(String playerId, String name, String playerToken) {
        this.playerId = playerId;
        this.name = name;
        this.playerToken = playerToken;
    }

}
