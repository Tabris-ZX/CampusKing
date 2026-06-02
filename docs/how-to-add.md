# 自制卡牌指南

本文说明如何新增或扩展一张卡牌。当前项目把卡牌参数和卡牌逻辑分开：

- 参数写在 `src/main/java/zx/campusking/cards/cardRegistry.json`
- 逻辑写在 `src/main/java/zx/campusking/cards/characters/` 或 `src/main/java/zx/campusking/cards/skills/`

## 1. 基本结构

角色牌继承 `BaseCharacterCard`，技能牌继承 `BaseSkillCard`。卡牌类只声明 `ID`、`ORDER` 和接收 `CardDefinition` 的构造方法，具体参数由注册表提供。

```java
public final class SampleCard extends BaseSkillCard {

    public static final String ID = "sample";
    public static final int ORDER = 140;

    public SampleCard(CardDefinition definition) {
        super(ORDER, definition);
    }
}
```

`ID` 必须和 `cardRegistry.json` 中的 `id` 一致。`ORDER` 只控制展示和构筑顺序。

## 2. 注册表

角色牌放在 `characters`：

```json
{
  "id": "sample_unit",
  "name": "示例角色",
  "type": "CHARACTER",
  "description": "无特性.",
  "actionCost": 1,
  "rarity": "COMMON",
  "attack": 20,
  "health": 80
}
```

技能牌放在 `skills`：

```json
{
  "id": "sample_skill",
  "name": "示例技能",
  "type": "SKILL",
  "description": "抽 1 张牌.",
  "actionCost": 1,
  "rarity": "COMMON",
  "effectType": "NONE",
  "effectCategory": "INSTANT",
  "effectValue": 1,
  "effectDuration": 0,
  "skillRange": "SELF"
}
```

通用字段：`id`、`name`、`type`、`description`、`actionCost`、`rarity`。

角色字段：`attack`、`health`、`exclusive`。

技能字段：`effectType`、`effectCategory`、`effectValue`、`effectDuration`、`skillRange`。

## 3. 常用 Hook

- `resolveSkill(CardEffectContext context)`：技能结算入口。
- `canResolveSkill(CardEffectContext context)`：校验当前目标是否合法。非法时应返回 `false`，避免技能半结算。
- `bypassesNegate()`：是否无视“下一张技能无效”。
- `sleepsOnSummon()`：角色上场后是否休整。
- `modifyAttack(CardCombatContext context, int attack)`：角色攻击力修正。
- `handleDefeated(CardDefeatContext context)`：角色被击败时的特殊处理。

## 4. CardEffectContext 原子能力

优先用这些方法组合技能，不要在技能类里直接操作手牌、牌堆、墓地、buff 列表。

资源类：

- `value()`：读取注册表里的 `effectValue`。
- `duration(fallback)`：读取注册表里的 `effectDuration`，为空时用 fallback。
- `drawCards(PlayerState targetPlayer, int count)`：让目标玩家摸指定数量的牌。
- `gainActionPoints(int count)`：让技能使用者获得指定数量行动点，可超过上限。

玩家生命：

- `damageSelf(int damage)`：对技能使用者造成伤害。

角色生命：

- `targetBoardCard(PlayerState owner)`：读取本次请求指定的场上角色。
- `targetsSelfBoard()`：本次请求是否指定己方场上角色。
- `targetsEnemyBoard()`：本次请求是否指定敌方场上角色。
- `healCharacter(PlayerState owner, CardInstance target, int amount)`：治疗一个角色。
- `damageCharacter(PlayerState owner, CardInstance target, int amount)`：对一个角色造成伤害，并处理死亡清理和奖励。
- `damageBoard(PlayerState owner, int amount)`：对某玩家场上全部角色造成伤害。

状态效果：

- `applyCardBuff(CardInstance target, StatusEffectType type, int value, int stacks, Integer remainingTurns)`：给单个角色添加 buff。
- `applyBoardBuff(PlayerState targetPlayer, StatusEffectType type, int value, int stacks, Integer remainingTurns)`：给某玩家场上全部角色添加同一种 buff。
- `applyPrevention(PreventableAction action)`：给使用者添加抵御指定动作的效果。
- `applyReviveOnDeath()`：给使用者添加死亡复活效果。

手牌：

- `discardHand(PlayerState targetPlayer, List<String> requestedIds, int count, boolean random)`：弃置目标玩家手牌。
- `requestedIds` 为空时，`random = true` 随机弃牌，`random = false` 从左到右弃牌。
- 返回值是实际弃置的牌，可以用来决定抽牌数量。

## 5. 组合示例

预见时间：获得行动点并自伤。

```java
context.gainActionPoints(context.value());
context.damageSelf(5);
```

汽水：先摸牌，再按目标归属治疗或伤害。

```java
context.drawCards(context.player(), 2);
if (context.targetsSelfBoard()) {
    context.healCharacter(context.player(), context.targetBoardCard(context.player()), context.value());
    return;
}
context.damageCharacter(context.enemy(), context.targetBoardCard(context.enemy()), context.value());
```

筹码：弃几张，摸几张。

```java
List<CardInstance> discarded = context.discardHand(context.player(), discardIds, context.value(), false);
context.drawCards(context.player(), discarded.size());
```

圣域：给双方场上角色分别添加攻击、生命上限和回合回血 buff。

```java
context.applyBoardBuff(target, StatusEffectType.ATTACK_UP, value, 1, duration);
context.applyBoardBuff(target, StatusEffectType.MAX_HP_UP, value, 1, duration);
context.applyBoardBuff(target, StatusEffectType.TURN_HEAL, value, 1, duration);
```

## 6. 目标校验

单体技能必须在 `canResolveSkill` 中校验目标。不要只检查场上是否存在任意角色，否则非法目标可能导致前端和后端状态不一致。

```java
@Override
public boolean canResolveSkill(CardEffectContext context) {
    if (!context.targetsSelfBoard() && !context.targetsEnemyBoard()) {
        return false;
    }
    PlayerState owner = context.targetsSelfBoard() ? context.player() : context.enemy();
    return owner.getBoard().stream()
            .anyMatch(card -> card.getInstanceId().equals(context.request().getTargetInstanceId()));
}
```

## 7. 资源路径

贴图资源在 COS 桶和本地 `resources` 挂载目录下保持同样结构。

```text
images/texture/characters/{cardId}.png
images/texture/skills/{cardId}.png
```

## 8. 验证

```bash
mvn test
cd webui
npm run build
```

新增机制时优先补后端测试，至少覆盖合法结算、非法目标、死亡清理和行动点变化。
