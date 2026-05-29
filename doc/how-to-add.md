# 新增卡牌教程

本文说明如何在当前项目里新增一张卡牌，并让它在后端规则与前端牌面中生效。

## 1. 明确卡牌类型

当前系统支持两类卡牌：

- `CHARACTER`
  角色牌，有攻击、生命、特性。
- `SKILL`
  技能牌，有技能效果、数值、作用范围。

## 2. 在 `cards.json` 中新增定义

文件位置：

- `src/main/resources/cards.json`

### 角色牌示例

```json
{
  "id": "sample_warrior",
  "name": "示例战士",
  "type": "CHARACTER",
  "description": "无特性",
  "attack": 40,
  "health": 80,
  "effectType": "NONE",
  "effectValue": 0,
  "traits": []
}
```

### 技能牌示例

```json
{
  "id": "sample_heal",
  "name": "示例治疗",
  "type": "SKILL",
  "description": "单体恢复30点生命",
  "attack": 0,
  "health": 0,
  "effectType": "HEAL_BOTH",
  "effectValue": 30,
  "skillRange": "SINGLE",
  "traits": []
}
```

## 3. 字段说明

### 通用字段

- `id`
  卡牌唯一 id。前后端、贴图路径、日志都依赖它。
- `name`
  中文显示名称。
- `type`
  `CHARACTER` 或 `SKILL`。
- `description`
  前端展示描述。

### 角色牌字段

- `attack`
  攻击值。
- `health`
  生命值。
- `traits`
  特性标签数组。

当前已使用的特性有：

- `sniper`
  上场首回合不能攻击。
- `dragon`
  攻击时追加已损失生命值；首次死亡后复活。
- `lark`
  当前原型里按“额外一条命”处理。

### 技能牌字段

- `effectType`
  技能效果类型。
- `effectValue`
  技能数值。
- `skillRange`
  技能作用范围。

当前支持的 `skillRange`：

- `SINGLE`
  单体，需要玩家额外选择目标角色或目标玩家。
- `ALLY`
  我方范围。
- `BOTH`
  双方范围。

## 4. 当前支持的技能类型

定义位置：

- `src/main/java/zx/campusking/model/EffectType.java`

当前已支持：

- `NONE`
  无效果。
- `HEAL_BOTH`
  治疗类效果。
  说明：
  如果 `skillRange=SINGLE`，会进入单体治疗逻辑。
- `DAMAGE_ALL_ENEMIES`
  伤害类效果。
  说明：
  如果 `skillRange=SINGLE`，会进入单体伤害逻辑。
- `GLOBAL_BUFF`
  给玩家添加持续增益。
- `SHIELD`
  添加护盾/挡伤害效果。
- `COUNTER_EFFECT`
  添加“下一张敌方技能无效”效果。

## 5. 如果是新效果类型，要改哪里

新增一个全新的技能效果时，通常需要同步修改这些位置：

1. `src/main/java/zx/campusking/model/EffectType.java`

   增加新的枚举值。

2. `src/main/java/zx/campusking/service/SkillResolverService.java`

   在 `resolveEffect(...)` 里补对应分支。

3. 如果效果会产生持续状态：

   - `src/main/java/zx/campusking/model/StatusEffectType.java`
   - `src/main/java/zx/campusking/service/StatusEffectService.java`

4. 如果前端需要更好的中文展示：

   - `src/main/resources/static/app.js`
   - `describeEffectType(...)`
   - `describeEffect(...)`

## 6. 贴图如何生效

前端现在按通用规则自动找贴图，不再手写映射。

### 角色牌贴图路径

```text
/texture/characters/{cardId}.png
```

例如：

```text
id = dragon
=> src/main/resources/static/texture/characters/dragon.png
```

### 技能牌贴图路径

```text
/texture/skills/{cardId}.png
```

例如：

```text
id = soda
=> src/main/resources/static/texture/skills/soda.png
```

## 7. 新增卡牌后的验证步骤

1. 修改 `cards.json`
2. 如需要，补贴图文件
3. 如果是新机制，补后端效果结算逻辑
4. 编译验证：

```powershell
mvn "-Dmaven.repo.local=data/.m2repo" -q -DskipTests compile
```

5. 测试验证：

```powershell
mvn "-Dmaven.repo.local=data/.m2repo" -q test
```

6. 启动后进入浏览器，创建房间并实测卡牌表现

## 8. 建议

- 新卡优先复用已有 `effectType` 和 `skillRange`
- 只有当现有规则无法表达时，再新增后端效果类型
- 每新增一个新机制，最好同步补一条注释和一条日志，方便调试
