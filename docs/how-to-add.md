# 自制卡牌指南

本文说明如何在当前项目中新增一张卡牌。现在卡牌不再写在 `cards.json` 中，而是每张卡对应一个 Java 类，放在 `src/main/java/zx/campusking/cards/` 下。

## 1. 选择卡牌类型

当前卡牌分为两类：

- 角色牌：放在 `src/main/java/zx/campusking/cards/characters/`，继承 `BaseCharacterCard`
- 技能牌：放在 `src/main/java/zx/campusking/cards/skills/`，继承 `BaseSkillCard`

卡牌扫描入口由 `config/config.yaml` 配置：

```yaml
cards:
  package: "zx.campusking.cards"
```

只要新卡类位于这个包或子包下，并实现 `GameCard`，后端启动时就会自动读入。

## 2. 新增角色牌

在 `cards/characters/` 下新增类，例如：

```java
package zx.campusking.cards.characters;

import zx.campusking.cards.BaseCharacterCard;

public final class SampleWarriorCard extends BaseCharacterCard {

    public SampleWarriorCard() {
        super(
                140,
                "sample_warrior",
                "示例战士",
                "无特性",
                40,
                null,
                80,
                null,
                null
        );
    }
}
```

构造参数依次是：

1. 展示顺序
2. 卡牌 id
3. 中文名称
4. 描述
5. 攻击力
6. 攻击文本，普通数值可填 `null`
7. 生命值
8. 第二形态攻击力，没有则填 `null`
9. 第二形态生命值，没有则填 `null`

## 3. 新增技能牌

在 `cards/skills/` 下新增类，例如：

```java
package zx.campusking.cards.skills;

import zx.campusking.cards.BaseSkillCard;
import zx.campusking.cards.CardEffectContext;
import zx.campusking.model.EffectCategory;
import zx.campusking.model.EffectType;
import zx.campusking.model.SkillRange;

public final class SampleHealCard extends BaseSkillCard {

    public SampleHealCard() {
        super(
                150,
                "sample_heal",
                "示例治疗",
                "恢复我方玩家 30 点生命。",
                EffectType.NONE,
                EffectCategory.INSTANT,
                30,
                0,
                SkillRange.SELF
        );
    }

    @Override
    public void resolveSkill(CardEffectContext context) {
        int maxHp = 100;
        context.player().setHp(Math.min(maxHp, context.player().getHp() + context.value()));
        context.match().getLogs().add(context.player().getName() + " 恢复了 " + context.value() + " 点生命。");
    }
}
```

技能构造参数依次是：

1. 展示顺序
2. 卡牌 id
3. 中文名称
4. 描述
5. 展示用效果类型
6. 效果分类，`INSTANT` 或 `DURATION`
7. 效果数值
8. 持续回合，即时效果一般为 `0`
9. 作用范围

## 4. 常用 hook

所有卡牌都实现 `GameCard`，常用 hook 有：

- `resolveSkill(CardEffectContext context)`：技能牌效果结算入口
- `canResolveSkill(CardEffectContext context)`：机器人出牌前判断技能是否有合法目标
- `bypassesNegate()`：是否无视“下一张技能无效”
- `sleepsOnSummon()`：角色上场后是否休整
- `modifyAttack(CardCombatContext context, int attack)`：角色攻击力修正
- `handleDefeated(CardDefeatContext context)`：角色死亡时的特殊处理

优先把新卡自己的逻辑写在卡牌类里，不要在 `GameService` 或 `SkillResolverService` 里按卡牌 id 写分支。

## 5. 贴图路径

贴图资源在 COS 桶和本地 `resources` 挂载目录下保持同样结构。

角色牌：

```text
images/texture/characters/{cardId}.png
```

技能牌：

```text
images/texture/skills/{cardId}.png
```

例如 `id = dragon` 的角色贴图：

```text
images/texture/characters/dragon.png
```

## 6. 新增后的验证

1. 新增卡牌 Java 类
2. 如需要，补对应贴图
3. 如果新机制需要状态效果，补 `StatusEffectType` 和 `StatusEffectService`
4. 运行后端测试：

```bash
bash ./mvnw test
```

5. 运行前端构建：

```bash
cd webui
npm run build
```

6. 进入浏览器创建房间，实测卡牌显示和效果

## 7. 建议

- 新卡优先继承 `BaseCharacterCard` 或 `BaseSkillCard`
- 新机制优先封装进卡牌类自己的 hook
- 多张卡共用的复杂逻辑，再考虑抽到 `CardEffectContext` 或独立服务
- 每个新效果最好写日志，方便对局里直接看出发生了什么
