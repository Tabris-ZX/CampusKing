# CampusKing

`CampusKing` 是一个基于 Spring Boot + Vue 的肉鸽卡牌PVP游戏原型。

当前项目包含：

- Java 后端：负责房间、对局状态、回合流转、卡牌结算和 WebSocket 广播。
- Vue 前端：提供首页、卡牌大全、对局页面、公告和文档弹窗。
- Java 卡牌类：每张卡是一个 `GameCard` 实现，位于 `src/main/java/zx/campusking/cards/`。
- 资源目录：图片和 favicon 放在项目根目录 `resources/`，可同步到 COS 桶。

## 游戏玩法
[完整规则(暂未完整实现)](https://github.com/Tabris-ZX/CampusKing/blob/master/docs/rule.md)

### 当前玩法
- 支持 `PVP` 双人房间和 `PVE` 人机房间。
- 每名玩家初始 `100 HP`，每回合默认抽 `2` 张牌。
- 每名玩家有 `3` 个召唤位，每回合最多召唤 `1` 个角色。
- 角色牌可以攻击对方角色；对方场上没有角色时可以直接攻击玩家。
- 技能牌由具体卡牌类实现效果，已包含护盾、反制、群伤、持续增益、抽牌换牌、弃置手牌等机制。

## 快速启动

### 一键启动

根目录下的`setup.sh`

### 前后端分别启动

后端:
```bash
bash ./mvnw spring-boot:run
```
前端:
```bash
cd webui
npm install
npm run dev
```

## 开发指南

### 目录结构

```text
src/main/java/zx/campusking
├── cards         # 卡牌定义与卡牌 hook
├── config        # 配置读取、跨域和 WebSocket 配置
├── controller    # REST API
├── model         # 对局模型、DTO 和枚举
├── service       # 房间、对局、战斗、状态、牌堆和资源服务
└── websocket     # 房间内实时广播

resources
├── favicon.ico
└── images        # 卡图、标题图、顶部背景等静态资源

webui
├── src           # Vue 前端源码
└── package.json
```

### 配置

主要配置在 `config/config.yaml`：

```yaml
server:
  backendPort: 8080

frontend:
  baseUrl: ""
  apiBaseUrl: ""
  wsBaseUrl: ""
  githubUrl: "https://github.com/Tabris-ZX/CampusKing"

asset:
  baseUrl: ""
  localRoot: "resources"
  maxResponseBytes: 1048576

cards:
  package: "zx.campusking.cards"

websocket:
  gamePath: "/ws/game"
```

`server.backendPort` 同时控制 Spring Boot 后端端口和 Vite 本地开发代理目标端口。

资源路径示例：

```text
resources/favicon.ico
resources/images/texture/characters/dragon.png
resources/images/texture/skills/soda.png
```

### 新增卡牌

[指南](https://github.com/Tabris-ZX/CampusKing/blob/master/docs/how-to-add.md)


<div align="center">

## 🌟 如果这个项目对你有帮助，请给个 Star！

**有任何问题欢迎来提issue/pr!**
</div>