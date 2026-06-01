# 开发需求

开发时一定要注意解耦性,还有可拓展性,后期卡片一定是越来越多的
注意关键接口要注释

## 文件结构样例

campus-king-server/
├── src/main/java/com/campusking/
│   ├── CampusKingServerApplication.java
│   │
│   ├── config/                          # 配置
│   │   ├── WebSocketConfig.java
│   │   └── GameConfig.java
│   │
│   ├── controller/                      # HTTP 接口
│   │   ├── AuthController.java
│   │   └── MatchController.java
│   │
│   ├── websocket/                       # WebSocket 通信
│   │   ├── GameWebSocketHandler.java
│   │   ├── SessionManager.java
│   │   └── MessageRouter.java
│   │
│   ├── model/                           # 数据模型
│   │   ├── card/
│   │   │   ├── CardDefinition.java      # 卡牌静态定义
│   │   │   ├── CardInstance.java        # 对局中的卡实例
│   │   │   ├── CharacterCard.java       # 角色牌
│   │   │   ├── EffectCard.java          # 效果牌
│   │   │   └── CardZone.java            # 卡所在区域枚举
│   │   ├── player/
│   │   │   ├── Player.java
│   │   │   └── PlayerState.java
│   │   ├── match/
│   │   │   ├── Match.java               # 对局核心
│   │   │   ├── MatchPhase.java          # 回合阶段枚举
│   │   │   └── MatchStatus.java         # 对局状态枚举
│   │   └── event/
│   │       ├── GameEvent.java           # 事件基类
│   │       └── EventType.java           # 事件类型枚举
│   │
│   ├── engine/                          # 游戏引擎（核心）
│   │   ├── GameEngine.java              # 主引擎，调度所有流程
│   │   ├── TurnManager.java             # 回合管理
│   │   ├── PhaseManager.java            # 阶段管理
│   │   ├── SummonResolver.java          # 召唤结算
│   │   ├── BattleResolver.java          # 战斗结算
│   │   ├── EffectResolver.java          # 效果牌结算
│   │   ├── DeckManager.java             # 抽牌堆/墓地管理
│   │   └── WinConditionChecker.java     # 胜负判定
│   │
│   ├── validator/                       # 合法性校验
│   │   ├── ActionValidator.java         # 校验接口
│   │   ├── SummonValidator.java
│   │   ├── AttackValidator.java
│   │   └── EffectValidator.java
│   │
│   ├── event/                           # 事件系统
│   │   ├── EventDispatcher.java         # 事件分发器
│   │   └── handler/                     # 事件处理器
│   │       ├── OnTurnStartHandler.java
│   │       ├── OnTurnEndHandler.java
│   │       ├── OnCharacterDefeatedHandler.java
│   │       └── OnEffectPlayedHandler.java
│   │
│   ├── action/                          # 玩家动作处理
│   │   ├── ActionHandler.java           # 动作处理接口
│   │   ├── SummonAction.java
│   │   ├── AttackCharacterAction.java
│   │   ├── AttackPlayerAction.java
│   │   ├── PlayEffectAction.java
│   │   └── EndTurnAction.java
│   │
│   ├── dto/                             # 数据传输对象
│   │   ├── request/
│   │   │   ├── ClientMessage.java       # 客户端发来的消息
│   │   │   ├── SummonRequest.java
│   │   │   ├── AttackRequest.java
│   │   │   └── PlayEffectRequest.java
│   │   └── response/
│   │       ├── ServerMessage.java       # 服务端推送的消息
│   │       ├── GameStateSnapshot.java
│   │       ├── GameEventDTO.java
│   │       └── ErrorResponse.java
│   │
│   └── service/                         # 业务服务
│       ├── PlayerService.java
│       ├── MatchmakingService.java      # 匹配服务
│       └── CardDefinitionService.java   # 卡牌数据加载
│
└── src/main/resources/
├── application.yml
└── cards.json                       # 卡牌数据定义文件

## 游戏流程

┌──────────────┐
│   客户端      │
│  WebSocket   │
└──────┬───────┘
│ ClientMessage
▼
┌──────────────┐
│  Handler     │  接收消息
└──────┬───────┘
▼
┌──────────────┐
│  Router      │  路由到对局
└──────┬───────┘
▼
┌──────────────┐
│ GameEngine   │  调度所有逻辑
└──────┬───────┘
│
├──────► Validator     （校验合法性）
├──────► Resolver      （结算效果/战斗）
├──────► EventDispatcher（触发事件）
└──────► WinChecker    （检查胜负）
│
▼
┌──────────────┐
│ 更新 Match   │  状态变更
└──────┬───────┘
▼
┌──────────────┐
│ Broadcast    │  分别给双方发状态快照
└──────────────┘

## 开发步骤

第1阶段（能跑）
├── Match / PlayerState 数据模型
├── GameEngine 基础调度
├── 召唤、攻击角色、攻击玩家
├── 回合切换
└── 胜负判定

第2阶段（能联机）
├── WebSocket 通信
├── 消息路由
├── 状态快照同步
└── 房间/匹配

第3阶段（能扩展）
├── 事件系统
├── 效果牌结算
├── 技能系统
└── 动画事件推送