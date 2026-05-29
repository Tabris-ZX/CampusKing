package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
/**
 * 对局中的卡牌实例。
 * 同一张静态卡牌定义可以在不同玩家或不同对局中生成多个实例。
 */
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

    public CardInstance(String cardId, String ownerId, int currentHealth) {
        this.instanceId = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.ownerId = ownerId;
        this.currentHealth = currentHealth;
        this.sleeping = true;
    }
}
