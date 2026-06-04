package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对局中的卡牌实例。
 * 同一张静态卡牌定义可以在不同玩家或不同对局中生成多个实例。
 */
@Setter
@Getter
public class CardInstance {

    /** 实例唯一标识，前后端交互都依赖它。 */
    private final String instanceId;
    /** 对应的静态卡牌定义 id。 */
    private final String cardId;
    /** 当前归属玩家 id。 */
    private final String ownerId;
    /** 当前生命值，仅角色牌有意义。 */
    private int currentHealth;
    /** 是否处于休整状态，休整时不能攻击。 */
    private boolean sleeping;
    /** 是否已经触发过一次复活。 */
    private boolean revived;
    /** 额外命次数，例如鸟女的额外命。 */
    private int extraLives;
    /** 当前形态序号，0 为初始形态。 */
    private int formIndex;
    /** 当前召唤区格位，1 到 3；不在场上时为 0。 */
    private int boardSlot;
    /** 当前作用在这名场上角色身上的状态效果。 */
    private final List<StatusEffect> statusEffects = new ArrayList<>();

    /**
     * 创建一张进入对局的卡牌实例。
     *
     * @param cardId 对应的静态卡牌 id
     * @param ownerId 初始归属玩家 id
     * @param currentHealth 初始当前生命值
     */
    public CardInstance(String cardId, String ownerId, int currentHealth) {
        this.instanceId = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.ownerId = ownerId;
        this.currentHealth = currentHealth;
        this.sleeping = true;
        this.formIndex = 0;
    }
}
