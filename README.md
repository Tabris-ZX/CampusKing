# CampusKing

~~我常常追忆过去...~~

`CampusKing` 是一个基于 Spring Boot + Vue 的肉鸽卡牌对战游戏

当前项目包含：

- Java 后端：负责房间、对局状态、卡牌结算、回合流转和 WebSocket 广播
- Vue 前端：提供首页、卡牌图鉴和对局界面
- JSON 卡牌配置：卡牌属性和技能描述集中定义在 `src/main/resources/cards.json`

## 当前实现玩法

- 双人回合制对战，支持 `PVP` 和简单 `PVE`
- 每名玩家初始 `100 HP`
- 每回合默认抽 `2` 张牌
- 每名玩家有 `3` 个召唤位
- 角色牌可召唤上场并攻击
- 技能牌分为：
  - `即时效果`
  - `持续效果`

当前已经实现的卡牌机制包括：

- 角色攻击与直接攻击玩家
- 护盾、反制、回血、群伤、持续增益
- 鸟女双阶段形态
- 龙骑首次死亡复活
- 精灵持续两回合内死亡回复 `1/5`

## 目录结构

```text
src/main/java/zx/campusking
├── config        # 配置类
├── controller    # REST API
├── model         # 对局模型 / DTO / 枚举
├── service       # 核心规则与结算逻辑
└── websocket     # 房间内实时广播

src/main/resources
├── application.properties
└── cards.json    # 卡牌静态定义

WebUI
├── src           # Vue 前端源码
└── package.json
```

## 运行环境

- `JDK 21`
- `Node.js 18+`
- `npm`
- `Maven`

## 快速启动

### 一键启动
根目录下的`setup.*`脚本

### 启动后端

在项目根目录执行：

```powershell
mvn spring-boot:run
```

默认会启动在：

```text
http://localhost:8080
```

后端会提供：

- REST API：`/api/*`
- WebSocket：`/ws/game`

### 启动前端开发环境

在 `WebUI` 目录执行：

```powershell
npm install
npm run dev
```

Vite 默认地址通常是：

```text
http://localhost:5173
```

前端会直接请求当前页面源站的 `/api` 和 `/ws/game`，本地联调时通常需要让前端和后端处于可互通环境。

## 构建前端

```powershell
cd WebUI
npm run build
```

## 常用校验命令

后端编译：

```powershell
mvn -q -DskipTests compile
```

前端构建：

```powershell
cd WebUI
npm run build
```

## 主要页面

- `/`：首页
- `/cards`：卡牌大全
- `/battle`：对局页面

## 说明

- 当前项目仍是规则原型，部分设计稿机制尚未全部落地
- 卡牌数据以 `cards.json` 为准
- 更详细的设计说明可参考：
  - `doc/design.md`
  - `doc/rule.md`
  - `doc/how-to-add.md`
