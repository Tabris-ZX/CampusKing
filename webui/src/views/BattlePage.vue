<template>
  <main class="app-shell battle-shell">
    <AppTopbar subtitle="对局页面" :status="connectionInfo" />

    <section class="table panel">
      <div class="table-layout">
        <section class="arena">
          <div v-if="!match" class="board-empty">{{ emptyStateText }}</div>
          <section v-else class="battle-board">
            <div class="battle-top-strip">
              <div class="battle-headline">
                <div class="battle-status-item">
                  <span>回合数</span>
                  <strong>{{ turnInfo }}</strong>
                </div>
                <div class="battle-status-item">
                  <span>房间号</span>
                  <strong>{{ roomCode || "------" }}</strong>
                </div>
                <div class="battle-headline-actions">
                  <button
                    class="alt battle-inline-action"
                    :disabled="!canCopyInviteLink"
                    :title="inviteBlockedReason"
                    @click="copyInviteLink"
                  >
                    复制邀请链接
                  </button>
                </div>
              </div>
              <section class="battle-log-panel top-log-panel">
                <div class="zone-title">日志</div>
                <div ref="logsRef" class="logs">
                  <div v-if="!renderedBattleLogs.length" class="log-entry empty">暂无日志</div>
                  <div
                    v-for="log in renderedBattleLogs"
                    :key="log.key"
                    class="log-entry"
                    :class="{ latest: highlightedLogKey === log.key }"
                  >
                    <span class="log-index">{{ log.number }}</span>
                    <span class="log-text">
                      <span
                        v-for="(part, partIndex) in log.parts"
                        :key="`${log.key}-${partIndex}`"
                        :class="part.className"
                      >{{ part.text }}</span>
                    </span>
                  </div>
                </div>
              </section>
              <div class="top-log-actions">
                <button class="alt battle-inline-action" type="button" @click="showDeckComposition = true">
                  卡牌构成
                </button>
                <button
                  class="alt danger-button battle-exit-action"
                  :disabled="leavingBattle"
                  @click="handleLeaveBattle"
                >
                  退出对局
                </button>
              </div>
            </div>

            <div class="battle-main-grid">
              <div class="battle-lanes">
                <div class="battle-player-row">
                  <div class="player-panel">
                    <div
                      class="player-avatar"
                      :class="playerAvatarClass(opponentPlayer)"
                      :data-player-id="opponentPlayer?.playerId || null"
                      @click="handlePlayerTarget(opponentPlayer)"
                    >
                      <img
                        v-if="playerAvatarImage(opponentPlayer)"
                        class="player-avatar-image"
                        :src="playerAvatarImage(opponentPlayer)"
                        :alt="opponentPlayerLabel"
                        @error="handleAvatarImageError"
                      >
                    </div>
                    <div class="player-avatar-info">
                      <div class="player-name">{{ opponentPlayerLabel }}</div>
                      <div v-if="opponentPlayer" class="player-compact-status" :class="playerToneClass(opponentPlayer)">
                        <strong class="player-score">{{ playerWins(opponentPlayer) }}</strong>
                        <div class="player-health-track">
                          <div class="player-health-fill" :style="{ width: playerHpPercent(opponentPlayer) }"></div>
                        </div>
                        <span class="player-health-text">{{ opponentPlayer.hp }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="summon-zone">
                    <div
                      v-for="slot in enemySlots"
                      :key="slot.slot"
                      class="summon-slot"
                      :class="{ empty: !slot.card }"
                    >
                      <span v-if="!slot.card">对方位 {{ slot.slot }}</span>
                      <article
                        v-else
                        class="card"
                        :class="boardCardClass(slot.card, false)"
                        :style="boardCardStyle(slot.card)"
                        :data-board-card-id="slot.card.instanceId"
                        @click="handleBoardCardClick(slot.card, opponentPlayer.playerId, false)"
                        @dblclick="openDetail(slot.card)"
                      >
                        <div class="card-surface">
                          <span class="card-cost-badge">{{ cardActionCost(slot.card.cardId) }}</span>
                          <img v-if="cardRarityFrame(slot.card.cardId)" class="card-rarity-frame" :src="cardRarityFrame(slot.card.cardId)" alt="">
                          <div v-if="boardEffectBadges(slot.card).length" class="card-effect-stack">
                            <span
                              v-for="effect in boardEffectBadges(slot.card)"
                              :key="effect.key"
                              class="card-effect-badge"
                              :class="effect.category"
                              :title="effect.label"
                            >
                              {{ effect.shortLabel }}
                            </span>
                          </div>
                          <div
                            class="card-figure"
                            :class="{ 'no-image': !cardImageFor(slot.card.cardId) }"
                            :data-mark="cardMark(slot.card.cardId)"
                          >
                            <img
                              v-if="cardImageFor(slot.card.cardId)"
                              :src="cardImageFor(slot.card.cardId)"
                              :alt="cardDef(slot.card.cardId).name"
                              @error="handleImageError($event, slot.card.cardId)"
                            >
                          </div>
                          <div class="card-overlay"></div>
                          <div class="card-body">
                            <div class="card-top">
                              <div class="card-name-block">
                                <div class="card-name">{{ cardDef(slot.card.cardId).name }}</div>
                              </div>
                            </div>
                            <div class="mini-stats">
                              <span>攻 {{ boardAttack(slot.card) }}</span>
                              <span>体 {{ boardHealth(slot.card) }}</span>
                            </div>
                          </div>
                        </div>
                      </article>
                    </div>
                  </div>
                </div>

                <div class="battle-player-row">
                  <div class="player-panel">
                    <div
                      class="player-avatar"
                      :class="playerAvatarClass(selfPlayer)"
                      :data-player-id="selfPlayer?.playerId || null"
                      @click="handlePlayerTarget(selfPlayer)"
                    >
                      <img
                        v-if="playerAvatarImage(selfPlayer)"
                        class="player-avatar-image"
                        :src="playerAvatarImage(selfPlayer)"
                        :alt="selfPlayerLabel"
                        @error="handleAvatarImageError"
                      >
                    </div>
                    <div class="player-avatar-info">
                      <div class="player-name">{{ selfPlayerLabel }}</div>
                      <div v-if="selfPlayer" class="player-compact-status" :class="playerToneClass(selfPlayer)">
                        <strong class="player-score">{{ playerWins(selfPlayer) }}</strong>
                        <div class="player-health-track">
                          <div class="player-health-fill" :style="{ width: playerHpPercent(selfPlayer) }"></div>
                        </div>
                        <span class="player-health-text">{{ selfPlayer.hp }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="summon-zone">
                    <div
                      v-for="slot in selfSlots"
                      :key="slot.slot"
                      class="summon-slot"
                      :class="{ empty: !slot.card }"
                    >
                      <span v-if="!slot.card">我方位 {{ slot.slot }}</span>
                      <article
                        v-else
                        class="card"
                        :class="boardCardClass(slot.card, true)"
                        :style="boardCardStyle(slot.card)"
                        :data-board-card-id="slot.card.instanceId"
                        @click="handleBoardCardClick(slot.card, selfPlayer.playerId, true)"
                        @dblclick="openDetail(slot.card)"
                      >
                        <div class="card-surface">
                          <span class="card-cost-badge">{{ cardActionCost(slot.card.cardId) }}</span>
                          <img v-if="cardRarityFrame(slot.card.cardId)" class="card-rarity-frame" :src="cardRarityFrame(slot.card.cardId)" alt="">
                          <div v-if="boardEffectBadges(slot.card).length" class="card-effect-stack">
                            <span
                              v-for="effect in boardEffectBadges(slot.card)"
                              :key="effect.key"
                              class="card-effect-badge"
                              :class="effect.category"
                              :title="effect.label"
                            >
                              {{ effect.shortLabel }}
                            </span>
                          </div>
                          <div
                            class="card-figure"
                            :class="{ 'no-image': !cardImageFor(slot.card.cardId) }"
                            :data-mark="cardMark(slot.card.cardId)"
                          >
                            <img
                              v-if="cardImageFor(slot.card.cardId)"
                              :src="cardImageFor(slot.card.cardId)"
                              :alt="cardDef(slot.card.cardId).name"
                              @error="handleImageError($event, slot.card.cardId)"
                            >
                          </div>
                          <div class="card-overlay"></div>
                          <div class="card-body">
                            <div class="card-top">
                              <div class="card-name-block">
                                <div class="card-name">{{ cardDef(slot.card.cardId).name }}</div>
                              </div>
                            </div>
                            <div class="mini-stats">
                              <span>攻 {{ boardAttack(slot.card) }}</span>
                              <span>体 {{ boardHealth(slot.card) }}</span>
                            </div>
                          </div>
                        </div>
                      </article>
                    </div>
                  </div>
                </div>
              </div>

              <aside class="battle-right-rail">
                <section class="effects-panel player-effects-panel">
                  <div class="effects-title">对方状态</div>
                  <div class="row-effects">
                    <span
                      v-for="effect in opponentEffects"
                      :key="effect.key"
                      class="effect-badge"
                      :class="effect.category"
                    >
                      {{ effect.label }}
                    </span>
                    <span v-if="!opponentEffects.length" class="effect-badge">无状态</span>
                  </div>
                </section>

                <section class="effects-panel player-effects-panel">
                  <div class="effects-title">我方状态</div>
                  <div class="row-effects">
                    <span
                      v-for="effect in selfEffects"
                      :key="effect.key"
                      class="effect-badge"
                      :class="effect.category"
                    >
                      {{ effect.label }}
                    </span>
                    <span v-if="!selfEffects.length" class="effect-badge">无状态</span>
                  </div>
                </section>

                <div class="pile-pair">
                  <div class="pile-box compact-pile-box">
                    <span>墓地</span>
                    <strong>{{ match.discardPile?.length || 0 }}</strong>
                  </div>
                  <div class="pile-box compact-pile-box draw-pile-box" :class="{ 'fx-draw-source': drawPulse }">
                    <span>抽牌堆</span>
                    <strong>{{ match.drawPile?.length || 0 }}</strong>
                    <small>{{ topDrawPileName }}</small>
                  </div>
                </div>

                <div class="pile-box action-point-box">
                  <span>行动点</span>
                  <strong>{{ selfActionPoints }} / 3</strong>
                  <small>可召唤 {{ selfSummonRemaining }}</small>
                </div>
              </aside>
            </div>

            <div class="battle-bottom-row">
              <div class="hand-zone" :class="{ 'fx-draw-receive': drawPulse }">
                <div class="zone-title">手牌区</div>
                <div ref="handRowRef" class="hand-row" :style="handRowStyle">
                  <div v-if="!selfPlayer?.hand?.length" class="board-empty">暂无手牌</div>
                  <article
                    v-for="(card, index) in selfPlayer?.hand || []"
                    :key="card.instanceId"
                    class="card selectable"
                    :class="handCardClass(card)"
                    :style="handCardStyle(index)"
                    @click="handleHandCardClick(card)"
                    @dblclick="openDetail(card)"
                  >
                    <div class="card-surface">
                      <span class="card-cost-badge">{{ cardActionCost(card.cardId) }}</span>
                      <img v-if="cardRarityFrame(card.cardId)" class="card-rarity-frame" :src="cardRarityFrame(card.cardId)" alt="">
                      <div
                        class="card-figure"
                        :class="{ 'no-image': !cardImageFor(card.cardId) }"
                        :data-mark="cardMark(card.cardId)"
                      >
                        <img
                          v-if="cardImageFor(card.cardId)"
                          :src="cardImageFor(card.cardId)"
                          :alt="cardDef(card.cardId).name"
                          @error="handleImageError($event, card.cardId)"
                        >
                      </div>
                      <div class="card-overlay"></div>
                      <div class="card-body">
                        <div class="card-top">
                          <div class="card-name-block">
                            <div class="card-name">{{ cardDef(card.cardId).name }}</div>
                          </div>
                        </div>
                        <div class="mini-stats">
                          <template v-if="cardDef(card.cardId).type === 'CHARACTER'">
                            <span>攻 {{ describeAttack(cardDef(card.cardId)) }}</span>
                            <span>体 {{ cardDef(card.cardId).exclusive?.secondaryHealth != null ? `${cardDef(card.cardId).health || 0}/${cardDef(card.cardId).exclusive.secondaryHealth}` : (cardDef(card.cardId).health || 0) }}</span>
                          </template>
                          <template v-else>
                            <span>{{ describeSkillRange(cardDef(card.cardId).skillRange) }}</span>
                          </template>
                        </div>
                      </div>
                    </div>
                  </article>
                </div>
              </div>

              <div class="turn-actions-panel">
                <div class="turn-actions">
                  <button class="action-confirm" :disabled="!canConfirmSelectedHand" @click="confirmSelectedHand">确定</button>
                  <button class="action-sacrifice" :disabled="!canSacrifice" @click="toggleSacrificeMode">献祭</button>
                  <button class="action-sort" :disabled="!canSortHand" @click="sortHand">手牌排序</button>
                  <button class="action-end-turn" @click="endTurn">结束回合</button>
                </div>
              </div>
            </div>
          </section>
        </section>
      </div>
    </section>
  </main>

  <div class="detail-modal result-modal" :class="{ visible: showResultModal }" @click.self="showResultModal = false">
    <section v-if="showResultModal" class="result-dialog panel">
      <div class="result-head">
        <span>对局结算</span>
        <strong>{{ winnerText }}</strong>
        <small>{{ matchScoreText }} · 最终小局 {{ match?.roundNumber || 1 }} · 房间 {{ roomCode || "------" }}</small>
      </div>
      <div class="result-stat-grid">
        <article
          v-for="item in resultPlayers"
          :key="item.player.playerId"
          class="result-player-card"
          :class="{ winner: item.isWinner, self: item.isSelf }"
        >
          <div class="result-player-top">
            <span>{{ item.isSelf ? "我方" : "对方" }}</span>
            <strong>{{ item.name }}</strong>
            <em>{{ item.isWinner ? "胜利" : "落败" }}</em>
          </div>
          <div class="result-metrics">
            <div>
              <span>造成伤害</span>
              <strong>{{ item.damageDealt }}</strong>
            </div>
            <div>
              <span>受到伤害</span>
              <strong>{{ item.damageTaken }}</strong>
            </div>
            <div>
              <span>行动回合</span>
              <strong>{{ item.turnsTaken }}</strong>
            </div>
            <div>
              <span>剩余生命</span>
              <strong>{{ item.hp }}</strong>
            </div>
          </div>
        </article>
      </div>
      <div class="result-actions">
        <button class="alt" type="button" @click="showResultModal = false">继续查看</button>
        <button class="danger-button" type="button" :disabled="leavingBattle" @click="leaveBattle({ remote: false, silent: true })">返回首页</button>
      </div>
    </section>
  </div>

  <div class="detail-modal" :class="{ visible: showDeckComposition }" @click.self="showDeckComposition = false">
    <section v-if="showDeckComposition" class="deck-composition-dialog panel">
      <button class="detail-close alt" type="button" @click="showDeckComposition = false">关闭</button>
      <div class="deck-composition-head">
        <span>对局牌堆</span>
        <strong>卡牌构成</strong>
        <small>共 {{ deckCompositionTotal }} 张</small>
        <div class="deck-composition-summary">
          <span class="deck-composition-tag type-character">角色 {{ deckCompositionStats.characters }}</span>
          <span class="deck-composition-tag type-skill">技能 {{ deckCompositionStats.skills }}</span>
          <span class="deck-composition-tag rarity-common">普通 {{ deckCompositionStats.common }}</span>
          <span class="deck-composition-tag rarity-rare">稀有 {{ deckCompositionStats.rare }}</span>
        </div>
      </div>
      <div class="deck-composition-list">
        <div v-if="!deckComposition.length" class="deck-composition-empty">暂无卡牌</div>
        <div
          v-for="item in deckComposition"
          :key="item.cardId"
          class="deck-composition-row"
          :class="[deckTypeTagClass(item.definition.type), deckRarityTagClass(item.definition.rarity)]"
        >
          <div>
            <strong>{{ item.definition.name }}</strong>
            <span class="deck-composition-tags">
              <span class="deck-composition-tag" :class="deckTypeTagClass(item.definition.type)">
                {{ describeType(item.definition.type) }}
              </span>
              <span class="deck-composition-tag" :class="deckRarityTagClass(item.definition.rarity)">
                {{ describeRarity(item.definition.rarity) }}
              </span>
            </span>
          </div>
          <em>x{{ item.count }}</em>
        </div>
      </div>
    </section>
  </div>

  <div class="detail-modal" :class="{ visible: !!detailCard }" @click.self="detailCard = null">
    <div v-if="detailCard" class="detail-card">
      <button class="detail-close alt" type="button" @click="detailCard = null">关闭</button>
      <div class="card-surface">
        <span class="card-cost-badge">{{ detailActionCost }}</span>
        <img v-if="detailRarityFrame" class="card-rarity-frame" :src="detailRarityFrame" alt="">
        <div class="card-figure" :class="{ 'no-image': !detailImage }" :data-mark="detailMark">
          <img v-if="detailImage" :src="detailImage" :alt="detailDefinition.name" @error="handleImageError($event, detailCard?.cardId || detailCard?.id)">
        </div>
        <div class="card-overlay"></div>
        <div class="card-body">
          <div class="card-top">
            <div class="card-name-block">
              <div class="card-name">{{ detailDefinition.name }}</div>
            </div>
          </div>
          <div class="stat-badges">
            <template v-if="detailDefinition.type === 'CHARACTER'">
              <span class="stat">攻击 {{ describeAttack(detailDefinition, detailCard?.formIndex || 0) }}</span>
              <span class="stat">体力 {{ detailCurrentHealth }}</span>
              <span class="stat">行动点 {{ detailActionCost }}</span>
              <span class="stat">稀有度 {{ describeRarity(detailDefinition.rarity) }}</span>
            </template>
            <template v-else>
              <span class="stat">作用范围 {{ describeSkillRange(detailDefinition.skillRange) }}</span>
              <span class="stat">效果分类 {{ describeEffectCategory(detailDefinition.effectCategory) }}</span>
              <span class="stat">行动点 {{ detailActionCost }}</span>
              <span class="stat">稀有度 {{ describeRarity(detailDefinition.rarity) }}</span>
            </template>
          </div>
          <div class="card-text">{{ detailDefinition.description || "暂无描述" }}</div>
          <div class="card-foot">
            <span>{{ detailDefinition.type === "SKILL" ? "技能牌详情" : "角色牌详情" }}</span>
            <span>{{ describeEffectType(detailDefinition.effectType) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppTopbar from "../components/AppTopbar.vue";
import { actionCostOf, api, buildInviteLink, cardImage, describeAttack, describeEffect, describeEffectCategory, describeEffectType, describeRarity, describeSkillRange, describeType, getInviteBlockedReason, isMissingRoomError, swapCardImageToFallback } from "../lib/game";
import { apiUrl } from "../lib/api-url";
import { assetRoot, copyText, publicBaseUrl, wsGamePath, wsRoot } from "../lib/runtime-config";
import { clearSession, ensureSessionPlayerName, loadSession, saveSession } from "../lib/session";
import { showToast } from "../lib/toast";

const router = useRouter();
const roomCode = ref("");
const selfPlayerId = ref("");
const playerToken = ref("");
const playerName = ref("");
const assetBaseUrl = ref("");
const match = ref(null);
const cardsMap = ref({});
const selectedHandId = ref("");
const discardSelectionIds = ref([]);
const discardMode = ref(false);
const selectedAttackerId = ref("");
const pendingSkillTarget = ref(null);
const pendingSkillDiscard = ref(null);
const sacrificeMode = ref(false);
const pollTimer = ref(null);
const socket = ref(null);
const socketConnected = ref(false);
const detailCard = ref(null);
const showDeckComposition = ref(false);
const logsRef = ref(null);
const handRowRef = ref(null);
const handRowWidth = ref(0);
const highlightedLogKey = ref("");
const summonedCardIds = ref([]);
const attackingCardIds = ref([]);
const damagedCardIds = ref([]);
const preventedSkillCardIds = ref([]);
const preventedAttackCardIds = ref([]);
const revivedCardIds = ref([]);
const damagedPlayerIds = ref([]);
const drawnHandIds = ref([]);
const drawPulse = ref(false);
const leavingBattle = ref(false);
const resultHandled = ref(false);
const showResultModal = ref(false);
const boardSlotMemory = ref({});
const activeAttackVectors = ref({});
const effectTimers = new Set();
let handRowResizeObserver = null;
let sessionPersistTimer = null;

const PHASE_TEXT = {
  DRAW: "抽牌阶段",
  ACTION: "行动阶段",
  FINISHED: "对局结束"
};

const selfPlayer = computed(() => match.value?.players?.find(player => player.playerId === selfPlayerId.value) || null);
const opponentPlayer = computed(() => match.value?.players?.find(player => player.playerId !== selfPlayerId.value) || null);
const isMyTurn = computed(() => !!match.value && match.value.currentPlayerId === selfPlayerId.value);
const selfPlayerLabel = computed(() => describeBattlePlayer(selfPlayer.value, "等待玩家"));
const opponentPlayerLabel = computed(() => describeBattlePlayer(opponentPlayer.value, "等待玩家"));

const connectionInfo = computed(() => {
  return selfPlayerId.value ? `已连接 ${publicBaseUrl()} / 身份 ${selfPlayerId.value}` : "未加入房间";
});

const turnInfo = computed(() => {
  if (!match.value) {
    return "等待开始";
  }
  return match.value.ready ? `第 ${match.value.roundNumber || 1} 局 · 第 ${match.value.turn} 回合` : "等待第二位玩家";
});
const matchScoreText = computed(() => {
  if (!match.value) {
    return "0 : 0";
  }
  return `${match.value.p1Wins || 0} : ${match.value.p2Wins || 0}`;
});

const phaseText = computed(() => describePhase(match.value?.phase));

const emptyStateText = computed(() => {
  if (roomCode.value) {
    return `正在恢复房间 ${roomCode.value}，如果长时间没有响应，请回首页重新加入。`;
  }
  return "还没有对局会话。请先回到首页创建或加入房间。";
});

const canSacrifice = computed(() => {
  return !!match.value && match.value.ready && isMyTurn.value && match.value.phase === "ACTION" && !!selfPlayer.value?.board?.length && hasActionPoints(1);
});

const canSortHand = computed(() => {
  return !!match.value && match.value.ready && !!selfPlayer.value && (selfPlayer.value.hand?.length || 0) > 1;
});

const discardOverflow = computed(() => Math.max(0, (selfPlayer.value?.hand?.length || 0) - 6));
const selfActionPoints = computed(() => selfPlayer.value?.actionPoints ?? 0);

const canConfirmSelectedHand = computed(() => {
  const my = selfPlayer.value;
  if (discardMode.value || !match.value || !match.value.ready || !my || !selectedHandId.value || !isMyTurn.value || match.value.phase !== "ACTION") {
    return false;
  }
  const selectedCard = my.hand.find(card => card.instanceId === selectedHandId.value);
  if (!selectedCard) {
    return false;
  }
  const definition = cardDef(selectedCard.cardId);
  if (!hasActionPoints(cardActionCost(selectedCard.cardId))) {
    return false;
  }
  if (definition.type === "CHARACTER") {
    return my.board.length < 3 && my.summonsThisTurn < 1;
  }
  return definition.type === "SKILL";
});

const enemySlots = computed(() => {
  return playerBoardSlots(opponentPlayer.value);
});

const selfSlots = computed(() => {
  return playerBoardSlots(selfPlayer.value);
});

const opponentEffects = computed(() => toEffectBadges(opponentPlayer.value));
const selfEffects = computed(() => toEffectBadges(selfPlayer.value));
const battleLogs = computed(() => match.value?.logs || []);
const MAX_RENDERED_LOGS = 30;
const renderedBattleLogs = computed(() => {
  const startIndex = Math.max(0, battleLogs.value.length - MAX_RENDERED_LOGS);
  return battleLogs.value.slice(startIndex).map((log, index) => {
    const realIndex = startIndex + index;
    return {
      key: logKey(log, realIndex),
      number: String(realIndex + 1).padStart(2, "0"),
      parts: logParts(log)
    };
  });
});
const handCards = computed(() => selfPlayer.value?.hand || []);
const selfSummonRemaining = computed(() => Math.max(0, 1 - (selfPlayer.value?.summonsThisTurn || 0)));
const handRowStyle = computed(() => {
  const cardCount = handCards.value.length;
  if (cardCount <= 0 || handRowWidth.value <= 0) {
    return {
      "--hand-row-justify": "flex-start",
      "--hand-card-overlap": "0px",
      "--hand-hover-spread": "0px",
      "--hand-row-padding-x": "14px",
      "--hand-card-gap": "0px"
    };
  }

  const cardWidth = handRowWidth.value <= 720 ? 160 : 154;
  const minimumGap = 4;
  const maxOverlap = Math.round(cardWidth * 0.58);
  const compressedSidePadding = 28;
  const availableWidth = Math.max(cardWidth, handRowWidth.value);
  const evenGap = (availableWidth - cardWidth * cardCount) / (cardCount + 1);
  const fitsWithoutOverlap = evenGap >= minimumGap;
  const compressedGap = cardCount > 1
    ? (availableWidth - compressedSidePadding - cardWidth * cardCount) / (cardCount - 1)
    : availableWidth - compressedSidePadding - cardWidth;
  const overlap = fitsWithoutOverlap ? 0 : Math.min(maxOverlap, Math.max(0, Math.ceil(-compressedGap)));
  const hoverSpread = fitsWithoutOverlap ? 0 : Math.min(overlap, Math.max(20, Math.round(cardWidth * 0.16)));
  const gap = fitsWithoutOverlap ? Math.floor(evenGap) : 0;

  return {
    "--hand-row-justify": "flex-start",
    "--hand-card-overlap": `${overlap}px`,
    "--hand-hover-spread": `${hoverSpread}px`,
    "--hand-row-padding-x": fitsWithoutOverlap ? `${gap}px` : "14px",
    "--hand-card-gap": fitsWithoutOverlap ? `${gap}px` : "0px"
  };
});
const topDrawPileName = computed(() => {
  const topCard = match.value?.drawPile?.[0];
  return topCard ? cardDef(topCard.cardId).name : "空";
});
const deckComposition = computed(() => {
  const counts = new Map();
  const collectList = list => {
    (list || []).forEach(card => {
      const cardId = card?.cardId || card?.id;
      if (!cardId) {
        return;
      }
      counts.set(cardId, (counts.get(cardId) || 0) + 1);
    });
  };

  collectList(match.value?.drawPile);
  collectList(match.value?.discardPile);
  (match.value?.players || []).forEach(player => {
    collectList(player.hand);
    collectList(player.board);
  });

  return Array.from(counts.entries())
    .map(([cardId, count]) => ({
      cardId,
      count,
      definition: cardDef(cardId)
    }))
    .sort((left, right) => {
      const leftType = left.definition.type === "CHARACTER" ? 0 : 1;
      const rightType = right.definition.type === "CHARACTER" ? 0 : 1;
      if (leftType !== rightType) {
        return leftType - rightType;
      }
      const leftRarity = left.definition.rarity === "RARE" ? 0 : 1;
      const rightRarity = right.definition.rarity === "RARE" ? 0 : 1;
      if (leftRarity !== rightRarity) {
        return leftRarity - rightRarity;
      }
      return String(left.definition.name || left.cardId).localeCompare(String(right.definition.name || right.cardId), "zh-Hans-CN");
    });
});
const deckCompositionTotal = computed(() => deckComposition.value.reduce((total, item) => total + item.count, 0));
const deckCompositionStats = computed(() => {
  return deckComposition.value.reduce(
    (stats, item) => {
      if (item.definition.type === "CHARACTER") {
        stats.characters += item.count;
      } else if (item.definition.type === "SKILL") {
        stats.skills += item.count;
      }
      if (item.definition.rarity === "RARE") {
        stats.rare += item.count;
      } else {
        stats.common += item.count;
      }
      return stats;
    },
    { characters: 0, skills: 0, common: 0, rare: 0 }
  );
});
const inviteBlockedReason = computed(() => {
  if (!roomCode.value) {
    return "当前没有可分享的房间";
  }
  return getInviteBlockedReason(match.value);
});
const canCopyInviteLink = computed(() => !!roomCode.value && !inviteBlockedReason.value);
const winnerText = computed(() => {
  if (!match.value?.winnerId) {
    return "对局结束";
  }
  if (match.value.winnerId === selfPlayerId.value) {
    return "你获胜了";
  }
  return "你失败了";
});
const resultPlayers = computed(() => {
  return (match.value?.players || []).map(player => ({
    player,
    name: describeBattlePlayer(player, "等待玩家"),
    isSelf: player.playerId === selfPlayerId.value,
    isWinner: player.playerId === match.value?.winnerId,
    damageDealt: player.damageDealt || 0,
    damageTaken: player.damageTaken || 0,
    turnsTaken: player.turnsTaken || 0,
    hp: player.hp || 0
  })).sort((left, right) => {
    if (left.isSelf !== right.isSelf) {
      return left.isSelf ? -1 : 1;
    }
    return left.player.playerId.localeCompare(right.player.playerId);
  });
});

const detailDefinition = computed(() => {
  if (!detailCard.value) {
    return { type: "UNKNOWN", name: "", description: "" };
  }
  return cardDef(detailCard.value.cardId || detailCard.value.id);
});

const detailImage = computed(() => {
  if (!detailCard.value) {
    return "";
  }
  return cardImage(detailCard.value.cardId || detailCard.value.id, cardsMap.value, assetBaseUrl.value);
});

const detailMark = computed(() => (detailDefinition.value.type === "SKILL" ? "技" : "角"));
const detailCurrentHealth = computed(() => {
  const maxHealth = currentFormHealth(detailDefinition.value, detailCard.value?.formIndex || 0);
  return clampHealth(detailCard.value?.currentHealth, maxHealth);
});
const detailActionCost = computed(() => cardActionCost(detailCard.value?.cardId || detailCard.value?.id));
const detailRarityFrame = computed(() => cardRarityFrame(detailCard.value?.cardId || detailCard.value?.id));

function cardDef(cardId) {
  return cardsMap.value[cardId] || { id: cardId, name: cardId, description: "", type: "UNKNOWN" };
}

function playerBoardSlots(player) {
  const rememberedSlots = boardSlotMemory.value[player?.playerId] || {};
  const cardsBySlot = new Map((player?.board || []).map((card, index) => [
    card.boardSlot || rememberedSlots[card.instanceId] || index + 1,
    card
  ]));
  return Array.from({ length: 3 }, (_, index) => ({
    slot: index + 1,
    card: cardsBySlot.get(index + 1) || null
  }));
}

function synchronizeBoardSlotMemory(nextMatch) {
  if (!nextMatch) {
    boardSlotMemory.value = {};
    return;
  }
  const nextMemory = {};
  (nextMatch.players || []).forEach(player => {
    const previousPlayerMemory = boardSlotMemory.value[player.playerId] || {};
    const usedSlots = new Set();
    const playerMemory = {};

    (player.board || []).forEach(card => {
      const rememberedSlot = card.boardSlot || previousPlayerMemory[card.instanceId];
      if (rememberedSlot >= 1 && rememberedSlot <= 3 && !usedSlots.has(rememberedSlot)) {
        playerMemory[card.instanceId] = rememberedSlot;
        usedSlots.add(rememberedSlot);
      }
    });

    (player.board || []).forEach((card, index) => {
      if (playerMemory[card.instanceId]) {
        return;
      }
      const preferredSlot = index + 1;
      if (preferredSlot <= 3 && !usedSlots.has(preferredSlot)) {
        playerMemory[card.instanceId] = preferredSlot;
        usedSlots.add(preferredSlot);
        return;
      }
      const openSlot = [1, 2, 3].find(slot => !usedSlots.has(slot));
      if (openSlot) {
        playerMemory[card.instanceId] = openSlot;
        usedSlots.add(openSlot);
      }
    });

    nextMemory[player.playerId] = playerMemory;
  });
  boardSlotMemory.value = nextMemory;
}

function cardActionCost(cardId) {
  return actionCostOf(cardDef(cardId));
}

function skillRequiredDiscardCount(definition) {
  return Math.max(0, Number(definition?.requiredHandDiscardCount) || 0);
}

function hasActionPoints(cost) {
  return selfActionPoints.value >= cost;
}

function cardRarityFrame(cardId) {
  return "";
}

function cardImageFor(cardId) {
  return cardImage(cardId, cardsMap.value, assetBaseUrl.value);
}

function cardMark(cardId) {
  return cardDef(cardId).type === "SKILL" ? "技" : "角";
}

function deckTypeTagClass(type) {
  return type === "CHARACTER" ? "type-character" : type === "SKILL" ? "type-skill" : "";
}

function deckRarityTagClass(rarity) {
  return rarity === "RARE" ? "rarity-rare" : "rarity-common";
}

function describePhase(phase) {
  return PHASE_TEXT[phase] || phase || "未开始";
}

function toEffectBadges(player) {
  return (player?.statusEffects || []).map((effect, index) => ({
    key: `${effect.type}-${effect.remainingTurns ?? "none"}-${index}`,
    label: describeEffect(effect),
    shortLabel: describeShortEffect(effect),
    category: effect.category === "debuff" ? "debuff" : "buff"
  }));
}

function boardEffectBadges(card) {
  return (card?.statusEffects || []).map((effect, index) => ({
    key: `${effect.type}-${effect.remainingTurns ?? "none"}-${index}`,
    label: describeEffect(effect),
    shortLabel: describeShortEffect(effect),
    category: effect.category === "debuff" ? "debuff" : "buff"
  }));
}

function describeShortEffect(effect) {
  const valueSuffix = effect.value ? `+${effect.value}` : "";
  const stackSuffix = effect.stacks > 1 ? `x${effect.stacks}` : "";
  const turnSuffix = effect.remainingTurns != null ? `·${effect.remainingTurns}` : "";
  switch (effect.type) {
    case "ATTACK_UP":
      return `攻${valueSuffix}${stackSuffix}${turnSuffix}`;
    case "MAX_HP_UP":
      return `体${valueSuffix}${stackSuffix}${turnSuffix}`;
    case "TURN_HEAL":
      return `回${valueSuffix}${stackSuffix}${turnSuffix}`;
    case "PREVENT_NEXT_ACTION":
      return `御${stackSuffix}${turnSuffix}`;
    case "REVIVE_ON_DEATH":
      return `复${stackSuffix}${turnSuffix}`;
    default:
      return `${effect.category === "debuff" ? "弱" : "强"}${stackSuffix}${turnSuffix}`;
  }
}

function sumCardEffect(card, type) {
  return (card?.statusEffects || [])
    .filter(effect => effect.type === type)
    .reduce((total, effect) => total + (Number(effect.value) || 0) * Math.max(1, Number(effect.stacks) || 1), 0);
}

function currentFormAttack(definition, formIndex = 0) {
  if (formIndex > 0 && definition.exclusive?.secondaryAttack != null) {
    return Number(definition.exclusive.secondaryAttack) || 0;
  }
  return Number(definition.attack) || 0;
}

function currentFormHealth(definition, formIndex = 0) {
  if (formIndex > 0 && definition.exclusive?.secondaryHealth != null) {
    return Number(definition.exclusive.secondaryHealth) || 0;
  }
  return Number(definition.health) || 0;
}

function clampHealth(value, fallbackMax = 0) {
  const maxHealth = Math.max(0, Number(fallbackMax) || 0);
  const rawHealth = value == null ? maxHealth : Number(value);
  const currentHealth = Number.isFinite(rawHealth) ? rawHealth : maxHealth;
  return Math.min(maxHealth, Math.max(0, currentHealth));
}

function boardAttack(card) {
  const definition = cardDef(card.cardId);
  const base = currentFormAttack(definition, card.formIndex || 0);
  return base + sumCardEffect(card, "ATTACK_UP");
}

function boardHealth(card) {
  const definition = cardDef(card.cardId);
  const maxHealth = currentFormHealth(definition, card.formIndex || 0) + sumCardEffect(card, "MAX_HP_UP");
  const currentHealth = clampHealth(card.currentHealth, maxHealth);
  return `${currentHealth}/${maxHealth}`;
}

function boardCardStyle(instance) {
  const vector = activeAttackVectors.value[instance.instanceId];
  if (!vector) {
    return {};
  }
  return {
    "--attack-x": `${vector.x}px`,
    "--attack-y": `${vector.y}px`
  };
}

function logKey(log, index) {
  return `${index}-${log}`;
}

function normalizePunctuation(value) {
  return String(value ?? "")
    .replace(/，/g, ", ")
    .replace(/。/g, ".")
    .replace(/；/g, "; ")
    .replace(/：/g, ": ")
    .replace(/！/g, "!")
    .replace(/？/g, "?")
    .replace(/（/g, "(")
    .replace(/）/g, ")")
    .replace(/\s+([,.;:!?])/g, "$1")
    .replace(/([,.;:!?])(?!\s|$)/g, "$1 ");
}

function logHighlightTokens() {
  const tokens = [];
  const seen = new Set();
  const add = (text, className) => {
    const normalized = String(text || "").trim();
    if (!normalized || seen.has(normalized)) {
      return;
    }
    seen.add(normalized);
    tokens.push({ text: normalized, className });
  };
  add(selfPlayer.value?.name, "log-mark-self");
  add("我方", "log-mark-self");
  add("己方", "log-mark-self");
  add(opponentPlayer.value?.name, "log-mark-enemy");
  add("对方", "log-mark-enemy");
  add("敌方", "log-mark-enemy");
  add("对手", "log-mark-enemy");
  return tokens.sort((left, right) => right.text.length - left.text.length);
}

function logParts(log) {
  const text = normalizePunctuation(log);
  const tokens = logHighlightTokens();
  if (!tokens.length || !text) {
    return [{ text, className: "" }];
  }
  const parts = [];
  let index = 0;
  while (index < text.length) {
    const token = tokens.find(item => text.startsWith(item.text, index));
    if (token) {
      parts.push({ text: token.text, className: token.className });
      index += token.text.length;
      continue;
    }
    const nextIndex = index + 1;
    const lastPart = parts[parts.length - 1];
    if (lastPart && !lastPart.className) {
      lastPart.text += text.slice(index, nextIndex);
    } else {
      parts.push({ text: text.slice(index, nextIndex), className: "" });
    }
    index = nextIndex;
  }
  return parts;
}

function queueEffectReset(callback, delay = 900) {
  const timer = window.setTimeout(() => {
    effectTimers.delete(timer);
    callback();
  }, delay);
  effectTimers.add(timer);
}

function flashIds(targetRef, ids, delay = 900) {
  const validIds = ids.filter(Boolean);
  if (!validIds.length) {
    return;
  }
  targetRef.value = Array.from(new Set([...targetRef.value, ...validIds]));
  queueEffectReset(() => {
    targetRef.value = targetRef.value.filter(id => !validIds.includes(id));
  }, delay);
}

function pulseDraw(delay = 900) {
  drawPulse.value = true;
  queueEffectReset(() => {
    drawPulse.value = false;
  }, delay);
}

function elementCenter(element) {
  const rect = element?.getBoundingClientRect();
  if (!rect) {
    return null;
  }
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2
  };
}

function cssEscape(value) {
  if (window.CSS?.escape) {
    return window.CSS.escape(value);
  }
  return String(value).replace(/["\\]/g, "\\$&");
}

function animateAttack(attackerInstanceId, targetSelector, delay = 420) {
  const attackerElement = document.querySelector(`[data-board-card-id="${cssEscape(attackerInstanceId)}"]`);
  const targetElement = document.querySelector(targetSelector);
  const attackerCenter = elementCenter(attackerElement);
  const targetCenter = elementCenter(targetElement);
  if (attackerCenter && targetCenter) {
    activeAttackVectors.value = {
      ...activeAttackVectors.value,
      [attackerInstanceId]: {
        x: Math.round(targetCenter.x - attackerCenter.x),
        y: Math.round(targetCenter.y - attackerCenter.y)
      }
    };
  }
  flashIds(attackingCardIds, [attackerInstanceId], delay);
  queueEffectReset(() => {
    const nextVectors = { ...activeAttackVectors.value };
    delete nextVectors[attackerInstanceId];
    activeAttackVectors.value = nextVectors;
  }, delay);
  return new Promise(resolve => window.setTimeout(resolve, Math.min(delay, 280)));
}

function animateCharacterAttack(attackerInstanceId, defenderInstanceId) {
  return animateAttack(attackerInstanceId, `[data-board-card-id="${cssEscape(defenderInstanceId)}"]`);
}

function animatePlayerAttack(attackerInstanceId, targetPlayerId) {
  return animateAttack(attackerInstanceId, `[data-player-id="${cssEscape(targetPlayerId)}"]`);
}

function scrollLogsToBottom() {
  nextTick(() => {
    if (logsRef.value) {
      logsRef.value.scrollTop = logsRef.value.scrollHeight;
    }
  });
}

function updateHandRowWidth() {
  handRowWidth.value = handRowRef.value?.clientWidth || 0;
}

function isLogsNearBottom() {
  if (!logsRef.value) {
    return true;
  }
  const distance = logsRef.value.scrollHeight - logsRef.value.scrollTop - logsRef.value.clientHeight;
  return distance < 24;
}

function mapInstances(list = []) {
  return new Map(list.map(card => [card.instanceId, card]));
}

function playerDisplayName(player) {
  return player?.playerToken === "BOT" ? "瓦库" : (player?.name || "");
}

function findPlayerByLogName(matchState, name) {
  const normalizedName = String(name || "").trim();
  if (!normalizedName) {
    return null;
  }
  return (matchState.players || []).find(player => playerDisplayName(player) === normalizedName || player.name === normalizedName) || null;
}

function protectedPlayerNameFromLog(log) {
  const markerIndex = String(log || "").indexOf(" 抵御了 ");
  if (markerIndex < 0) {
    return "";
  }
  const beforeMarker = String(log).slice(0, markerIndex).trim();
  return beforeMarker.split(/[，,]/).pop().trim();
}

function attackedCardNameFromLogs(logs) {
  const attackLog = [...logs].reverse().find(log => String(log || "").includes("攻击了") && !String(log || "").includes("直接攻击了玩家"));
  if (!attackLog) {
    return "";
  }
  const matchResult = String(attackLog).match(/攻击了\s*([^,.。]+)/);
  return matchResult?.[1]?.trim() || "";
}

function boardIdsForPlayer(player, cardName = "") {
  if (!player) {
    return [];
  }
  const normalizedCardName = String(cardName || "").trim();
  const cards = normalizedCardName
    ? (player.board || []).filter(card => cardDef(card.cardId).name === normalizedCardName)
    : (player.board || []);
  const targetCards = cards.length ? cards : (player.board || []);
  return targetCards.map(card => card.instanceId);
}

function boardIdsForCardName(matchState, cardName) {
  const normalizedCardName = String(cardName || "").trim();
  if (!normalizedCardName) {
    return [];
  }
  return (matchState.players || [])
    .flatMap(player => player.board || [])
    .filter(card => cardDef(card.cardId).name === normalizedCardName)
    .map(card => card.instanceId);
}

function revivedCardNameFromLog(log) {
  const marker = " 死亡后回复到 ";
  const markerIndex = String(log || "").indexOf(marker);
  if (markerIndex < 0) {
    return "";
  }
  return String(log).slice(0, markerIndex).trim();
}

function processLogTriggeredAnimations(nextMatch, newLogs) {
  if (!newLogs.length) {
    return;
  }

  newLogs.forEach(log => {
    const text = String(log || "");
    if (text.includes("抵御了")) {
      const protectedPlayer = findPlayerByLogName(nextMatch, protectedPlayerNameFromLog(text));
      if (text.includes(" 的攻击")) {
        flashIds(preventedAttackCardIds, boardIdsForPlayer(protectedPlayer, attackedCardNameFromLogs(newLogs)), 1150);
      } else {
        flashIds(preventedSkillCardIds, boardIdsForPlayer(protectedPlayer), 1150);
      }
    }

    if (text.includes("死亡后回复到")) {
      flashIds(revivedCardIds, boardIdsForCardName(nextMatch, revivedCardNameFromLog(text)), 1300);
    }
  });
}

function processMatchAnimations(nextMatch, previousMatch) {
  if (!nextMatch) {
    return;
  }
  if (!previousMatch) {
    scrollLogsToBottom();
    return;
  }
  const shouldAutoScrollLogs = isLogsNearBottom();

  const previousPlayers = previousMatch.players || [];
  const nextPlayers = nextMatch.players || [];
  const previousDrawPileSize = previousMatch.drawPile?.length || 0;
  const nextDrawPileSize = nextMatch.drawPile?.length || 0;
  if (nextDrawPileSize < previousDrawPileSize) {
    pulseDraw(1000);
  }

  nextPlayers.forEach(nextPlayer => {
    const previousPlayer = previousPlayers.find(player => player.playerId === nextPlayer.playerId);
    if (!previousPlayer) {
      return;
    }

    if ((nextPlayer.hp || 0) < (previousPlayer.hp || 0)) {
      flashIds(damagedPlayerIds, [nextPlayer.playerId], 780);
    }

    const previousBoard = mapInstances(previousPlayer.board || []);
    const nextBoard = mapInstances(nextPlayer.board || []);
    const summonedIds = Array.from(nextBoard.keys()).filter(instanceId => !previousBoard.has(instanceId));
    const damagedIds = Array.from(nextBoard.entries())
      .filter(([instanceId, card]) => {
        const previousCard = previousBoard.get(instanceId);
        if (!previousCard) {
          return false;
        }
        const nextDefinition = cardDef(card.cardId);
        const previousDefinition = cardDef(previousCard.cardId);
        const nextHealth = clampHealth(card.currentHealth, currentFormHealth(nextDefinition, card.formIndex || 0));
        const previousHealth = clampHealth(previousCard.currentHealth, currentFormHealth(previousDefinition, previousCard.formIndex || 0));
        return nextHealth < previousHealth;
      })
      .map(([instanceId]) => instanceId);

    flashIds(summonedCardIds, summonedIds, 1100);
    flashIds(damagedCardIds, damagedIds, 780);

    if (nextPlayer.playerId === selfPlayerId.value) {
      const previousHandIds = new Set((previousPlayer.hand || []).map(card => card.instanceId));
      const newHandIds = (nextPlayer.hand || [])
        .map(card => card.instanceId)
        .filter(instanceId => !previousHandIds.has(instanceId));
      if (newHandIds.length) {
        flashIds(drawnHandIds, newHandIds, 1100);
        pulseDraw(1000);
      }
    }
  });

  if ((nextMatch.logs?.length || 0) > (previousMatch.logs?.length || 0)) {
    const lastIndex = nextMatch.logs.length - 1;
    const latestKey = logKey(nextMatch.logs[lastIndex], lastIndex);
    const newLogs = nextMatch.logs.slice(previousMatch.logs?.length || 0);
    processLogTriggeredAnimations(nextMatch, newLogs);
    highlightedLogKey.value = latestKey;
    queueEffectReset(() => {
      if (highlightedLogKey.value === latestKey) {
        highlightedLogKey.value = "";
      }
    }, 1600);
  }

  if (shouldAutoScrollLogs) {
    scrollLogsToBottom();
  }
}

function persistSession() {
  if (sessionPersistTimer) {
    window.clearTimeout(sessionPersistTimer);
    sessionPersistTimer = null;
  }
  saveSession({
    baseUrl: publicBaseUrl(),
    roomCode: roomCode.value,
    selfPlayerId: selfPlayerId.value,
    playerToken: playerToken.value,
    playerName: playerName.value
  });
}

function schedulePersistSession(delay = 240) {
  if (sessionPersistTimer) {
    window.clearTimeout(sessionPersistTimer);
  }
  sessionPersistTimer = window.setTimeout(() => {
    sessionPersistTimer = null;
    persistSession();
  }, delay);
}

function resetLocalBattleState() {
  const preservedPlayerName = playerName.value;
  roomCode.value = "";
  selfPlayerId.value = "";
  playerToken.value = "";
  playerName.value = preservedPlayerName;
  match.value = null;
  selectedHandId.value = "";
  discardSelectionIds.value = [];
  discardMode.value = false;
  selectedAttackerId.value = "";
  pendingSkillTarget.value = null;
  sacrificeMode.value = false;
  detailCard.value = null;
  showDeckComposition.value = false;
  highlightedLogKey.value = "";
  summonedCardIds.value = [];
  attackingCardIds.value = [];
  damagedCardIds.value = [];
  preventedSkillCardIds.value = [];
  preventedAttackCardIds.value = [];
  revivedCardIds.value = [];
  damagedPlayerIds.value = [];
  drawnHandIds.value = [];
  drawPulse.value = false;
  resultHandled.value = false;
  showResultModal.value = false;
  boardSlotMemory.value = {};
  activeAttackVectors.value = {};
}

function stopRealtime() {
  stopPolling();
  if (socket.value) {
    socket.value.onopen = null;
    socket.value.onmessage = null;
    socket.value.onclose = null;
    socket.value.onerror = null;
    socket.value.close();
    socket.value = null;
  }
  socketConnected.value = false;
}

function persistNameOnlySession() {
  saveSession({
    baseUrl: publicBaseUrl(),
    roomCode: "",
    selfPlayerId: "",
    playerToken: "",
    playerName: playerName.value
  });
}

function clearInvalidBattleSession() {
  stopRealtime();
  resetLocalBattleState();
  if (playerName.value) {
    persistNameOnlySession();
  } else {
    clearSession();
  }
}

async function leaveBattle({ remote = true, silent = false } = {}) {
  if (leavingBattle.value) {
    return;
  }
  leavingBattle.value = true;
  try {
    if (remote && roomCode.value && playerToken.value) {
      await api(`/game/rooms/${roomCode.value}/leave`, {
        method: "POST",
        body: JSON.stringify({ playerToken: playerToken.value })
      });
    }
  } catch (error) {
    if (!silent) {
      showToast(error.message, "error");
    }
    return;
  } finally {
    stopRealtime();
    resetLocalBattleState();
    if (playerName.value) {
      persistNameOnlySession();
    } else {
      clearSession();
    }
    leavingBattle.value = false;
  }

  if (!silent) {
    showToast("已退出对局", "success");
  }
  router.push("/");
}

function canTargetPlayer(player) {
  if (!player) {
    return false;
  }
  if (pendingSkillTarget.value && !pendingSkillDiscard.value) {
    return true;
  }
  return !!selectedAttackerId.value && player.playerId !== selfPlayerId.value && player.board.length === 0;
}

function playerAvatarClass(player) {
  return {
    targetable: canTargetPlayer(player),
    "fx-hit": player ? damagedPlayerIds.value.includes(player.playerId) : false
  };
}

function playerAvatarImage(player) {
  if (!player) {
    return "";
  }
  if (player.playerToken === "BOT") {
    return uiAssetImage("bot.webp");
  }
  return uiAssetImage(player.playerId === "P1" ? "avatar1.webp" : "avatar2.webp");
}

function playerToneClass(player) {
  return player?.playerId === "P1" ? "tone-p1" : "tone-p2";
}

function playerWins(player) {
  if (!player) {
    return 0;
  }
  return player.playerId === "P1" ? (match.value?.p1Wins || 0) : (match.value?.p2Wins || 0);
}

function playerHpPercent(player) {
  const hp = Math.max(0, Math.min(100, Number(player?.hp) || 0));
  return `${hp}%`;
}

function uiAssetImage(fileName) {
  return apiUrl(`/assets/ui/${fileName}`);
}

function boardCardClass(instance, isSelfBoard) {
  const canAttack = isSelfBoard && isMyTurn.value && match.value?.phase === "ACTION" && !instance.sleeping;
  const canSacrificeTarget = isSelfBoard && sacrificeMode.value && isMyTurn.value && match.value?.phase === "ACTION";
  const targetable = (!isSelfBoard && !!selectedAttackerId.value && isMyTurn.value && match.value?.phase === "ACTION")
    || !!pendingSkillTarget.value;
  const definition = cardDef(instance.cardId);
  return {
    "card-character": definition.type === "CHARACTER",
    "card-skill": definition.type === "SKILL",
    selectable: canAttack || canSacrificeTarget,
    "attacker-selected": selectedAttackerId.value === instance.instanceId,
    "attack-target": targetable,
    "sacrifice-target": canSacrificeTarget,
    "fx-summoned": summonedCardIds.value.includes(instance.instanceId),
    "fx-attacking": attackingCardIds.value.includes(instance.instanceId),
    "fx-hit": damagedCardIds.value.includes(instance.instanceId),
    "fx-prevent-skill": preventedSkillCardIds.value.includes(instance.instanceId),
    "fx-prevent-attack": preventedAttackCardIds.value.includes(instance.instanceId),
    "fx-revived": revivedCardIds.value.includes(instance.instanceId)
  };
}

function handCardClass(instance) {
  const definition = cardDef(instance.cardId);
  return {
    "card-character": definition.type === "CHARACTER",
    "card-skill": definition.type === "SKILL",
    selected: !discardMode.value && !pendingSkillDiscard.value && selectedHandId.value === instance.instanceId,
    "discard-selected": discardSelectionIds.value.includes(instance.instanceId)
      || (pendingSkillDiscard.value?.selectedIds || []).includes(instance.instanceId),
    "discard-target": canSelectSkillDiscard(instance),
    "fx-draw-arrival": drawnHandIds.value.includes(instance.instanceId)
  };
}

function handCardStyle(index) {
  return {
    "--hand-card-z": String(index + 1)
  };
}

function describeBattlePlayer(player, fallback) {
  if (!player) {
    return fallback;
  }
  return player.playerToken === "BOT" ? "瓦库" : (player.name || fallback);
}

function handleImageError(event, cardId) {
  if (!swapCardImageToFallback(event, cardId, cardsMap.value, assetBaseUrl.value)) {
    event.target.remove();
  }
}

function handleAvatarImageError(event) {
  event.target.remove();
}

async function copyInviteLink() {
  if (!roomCode.value) {
    showToast("当前没有可分享的房间", "error");
    return;
  }
  if (inviteBlockedReason.value) {
    showToast(inviteBlockedReason.value, "error");
    return;
  }
  try {
    await copyText(buildInviteLink(roomCode.value, publicBaseUrl()));
    showToast("邀请链接已复制", "success");
  } catch {
    showToast("复制失败", "error");
  }
}

async function refreshRoom() {
  if (!roomCode.value) {
    return;
  }
  try {
    match.value = await api(`/game/rooms/${roomCode.value}`);
  } catch (error) {
    if (isMissingRoomError(error)) {
      clearInvalidBattleSession();
      showToast("房间不存在，已清理本地会话。", "error");
      return;
    }
    throw error;
  }
}

function startPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
  }
  if (socketConnected.value) {
    return;
  }
  pollTimer.value = setInterval(async () => {
    if (socketConnected.value) {
      clearInterval(pollTimer.value);
      pollTimer.value = null;
      return;
    }
    try {
      await refreshRoom();
    } catch {
    }
  }, 5000);
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

function connectSocket() {
  if (!roomCode.value) {
    return;
  }
  if (socket.value) {
    socket.value.close();
  }
  socket.value = new WebSocket(`${wsRoot()}${wsGamePath()}?roomCode=${encodeURIComponent(roomCode.value)}`);
  socket.value.onopen = () => {
    socketConnected.value = true;
    stopPolling();
  };
  socket.value.onmessage = event => {
    try {
      match.value = JSON.parse(event.data);
      persistSession();
    } catch {
    }
  };
  socket.value.onclose = () => {
    socketConnected.value = false;
    startPolling();
  };
  socket.value.onerror = () => {
    socketConnected.value = false;
    startPolling();
  };
}

async function restoreMatchSession() {
  const saved = loadSession();
  if (!saved?.roomCode || !saved?.playerToken) {
    return;
  }
  roomCode.value = saved.roomCode;
  selfPlayerId.value = saved.selfPlayerId || "";
  playerToken.value = saved.playerToken;
  playerName.value = saved.playerName || ensureSessionPlayerName(saved);
  try {
    const session = await api(`/game/rooms/${roomCode.value}/session/${playerToken.value}`);
    match.value = session.match;
    selfPlayerId.value = session.playerId;
    resultHandled.value = false;
    connectSocket();
    startPolling();
    persistSession();
    showToast("已恢复上次会话", "success");
  } catch (error) {
    if (isMissingRoomError(error)) {
      clearInvalidBattleSession();
      showToast("房间不存在，已清理本地会话。", "error");
    } else {
      showToast(error.message, "error");
    }
  }
}

async function confirmSelectedHand() {
  if (!canConfirmSelectedHand.value || !selfPlayer.value) {
    return;
  }
  sacrificeMode.value = false;
  discardMode.value = false;
  discardSelectionIds.value = [];
  pendingSkillDiscard.value = null;
  const selectedCard = selfPlayer.value.hand.find(card => card.instanceId === selectedHandId.value);
  if (!selectedCard) {
    return;
  }
  const definition = cardDef(selectedCard.cardId);
  if (definition.type === "SKILL") {
    await playSkill(selectedCard.instanceId);
  } else {
    await summonCard(selectedCard.instanceId);
  }
}

function handleHandCardClick(card) {
  if (pendingSkillDiscard.value) {
    selectSkillDiscard(card);
    return;
  }
  if (discardMode.value) {
    toggleDiscardSelection(card.instanceId);
    return;
  }
  if (selectedHandId.value === card.instanceId) {
    selectedHandId.value = "";
    pendingSkillTarget.value = null;
    return;
  }
  selectedHandId.value = card.instanceId;
  pendingSkillTarget.value = null;
}

function canSelectSkillDiscard(card) {
  return !!pendingSkillDiscard.value && card.instanceId !== pendingSkillDiscard.value.instanceId;
}

async function selectSkillDiscard(card) {
  if (!canSelectSkillDiscard(card)) {
    showToast("请选择另一张手牌弃置。", "error");
    return;
  }
  const pending = pendingSkillDiscard.value;
  const selectedIds = pending.selectedIds || [];
  const requiredCount = Math.max(1, pending.requiredCount || 1);
  const nextSelectedIds = selectedIds.includes(card.instanceId)
    ? selectedIds.filter(id => id !== card.instanceId)
    : [...selectedIds, card.instanceId].slice(0, requiredCount);
  if (nextSelectedIds.length < requiredCount) {
    pendingSkillDiscard.value = { ...pending, selectedIds: nextSelectedIds };
    return;
  }
  pendingSkillDiscard.value = null;
  await playSkill(pending.instanceId, pending.targetPlayerId, pending.targetInstanceId, nextSelectedIds.slice(0, requiredCount));
}

function toggleDiscardSelection(instanceId) {
  if (discardSelectionIds.value.includes(instanceId)) {
    discardSelectionIds.value = discardSelectionIds.value.filter(id => id !== instanceId);
    return;
  }
  if (discardSelectionIds.value.length >= discardOverflow.value) {
    showToast(`只需要选择 ${discardOverflow.value} 张手牌弃置。`, "info");
    return;
  }
  discardSelectionIds.value = [...discardSelectionIds.value, instanceId];
}

async function summonCard(instanceId) {
  try {
    match.value = await api(`/game/matches/${match.value.matchId}/summon`, {
      method: "POST",
      body: JSON.stringify({ playerId: selfPlayerId.value, handInstanceId: instanceId })
    });
    selectedHandId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    persistSession();
    showToast("角色已召唤", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function playSkill(instanceId, targetPlayerId = null, targetInstanceId = null, discardInstanceIds = []) {
  try {
    const skillCard = selfPlayer.value?.hand.find(card => card.instanceId === instanceId);
    const definition = skillCard ? cardDef(skillCard.cardId) : null;
    if (definition?.skillRange === "SINGLE" && !targetPlayerId) {
      pendingSkillTarget.value = { instanceId };
      showToast("请选择一个场上角色作为技能目标。", "info");
      return;
    }
    if (definition?.skillRange === "SINGLE" && targetPlayerId && !targetInstanceId) {
      showToast("这个技能只能选择场上角色，不能选择玩家。", "error");
      return;
    }
    const requiredDiscardCount = skillRequiredDiscardCount(definition);
    if (requiredDiscardCount > 0 && (discardInstanceIds?.length || 0) < requiredDiscardCount) {
      pendingSkillDiscard.value = { instanceId, targetPlayerId, targetInstanceId, requiredCount: requiredDiscardCount, selectedIds: [] };
      pendingSkillTarget.value = null;
      showToast(`请选择 ${requiredDiscardCount} 张手牌作为技能弃置。`, "info");
      return;
    }
    match.value = await api(`/game/matches/${match.value.matchId}/play-skill`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        handInstanceId: instanceId,
        targetPlayerId,
        targetInstanceId,
        discardInstanceIds
      })
    });
    pendingSkillTarget.value = null;
    selectedHandId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    pendingSkillDiscard.value = null;
    persistSession();
    showToast("技能已使用", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function attackCharacter(attackerInstanceId, defenderInstanceId) {
  try {
    const attackAnimation = animateCharacterAttack(attackerInstanceId, defenderInstanceId);
    const nextMatch = await api(`/game/matches/${match.value.matchId}/attack-character`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        attackerInstanceId,
        defenderInstanceId
      })
    });
    await attackAnimation;
    match.value = nextMatch;
    selectedAttackerId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    pendingSkillDiscard.value = null;
    persistSession();
    showToast("攻击已结算", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function attackPlayer() {
  if (!selectedAttackerId.value) {
    showToast("请先选择一个己方可攻击角色。", "error");
    return;
  }
  if (opponentPlayer.value?.board?.length) {
    showToast("对方场上还有角色，不能直接攻击玩家。", "error");
    return;
  }
  try {
    const attackerInstanceId = selectedAttackerId.value;
    const attackAnimation = animatePlayerAttack(attackerInstanceId, opponentPlayer.value.playerId);
    const nextMatch = await api(`/game/matches/${match.value.matchId}/attack-player`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        attackerInstanceId
      })
    });
    await attackAnimation;
    match.value = nextMatch;
    selectedAttackerId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    persistSession();
    showToast("已对玩家造成伤害。", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

function toggleSacrificeMode() {
  if (!canSacrifice.value) {
    return;
  }
  sacrificeMode.value = !sacrificeMode.value;
  selectedAttackerId.value = "";
  selectedHandId.value = "";
  discardMode.value = false;
  discardSelectionIds.value = [];
  pendingSkillDiscard.value = null;
  pendingSkillTarget.value = null;
  showToast(sacrificeMode.value ? "请选择己方召唤区一名角色献祭。" : "已取消献祭。", "info");
}

async function sacrificeCard(instanceId) {
  try {
    match.value = await api(`/game/matches/${match.value.matchId}/sacrifice`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        targetInstanceId: instanceId
      })
    });
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    persistSession();
    showToast("献祭已结算", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function sortHand() {
  if (!canSortHand.value) {
    return;
  }
  try {
    match.value = await api(`/game/matches/${match.value.matchId}/sort-hand?playerId=${selfPlayerId.value}`, {
      method: "POST"
    });
    selectedHandId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    persistSession();
    showToast("手牌已排序", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function endTurn() {
  if (!match.value) {
    return;
  }
  if (discardOverflow.value > 0) {
    if (!discardMode.value) {
      discardMode.value = true;
      discardSelectionIds.value = [];
      selectedHandId.value = "";
      selectedAttackerId.value = "";
      sacrificeMode.value = false;
      pendingSkillTarget.value = null;
      pendingSkillDiscard.value = null;
      showToast(`手牌超过上限，请选择 ${discardOverflow.value} 张手牌弃置。`, "info");
      return;
    }
    if (discardSelectionIds.value.length !== discardOverflow.value) {
      showToast(`还需要选择 ${discardOverflow.value - discardSelectionIds.value.length} 张手牌。`, "error");
      return;
    }
  }
  try {
    match.value = await api(`/game/matches/${match.value.matchId}/end-turn`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        discardInstanceIds: discardSelectionIds.value
      })
    });
    selectedAttackerId.value = "";
    sacrificeMode.value = false;
    discardMode.value = false;
    discardSelectionIds.value = [];
    persistSession();
    showToast("回合已结束", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function handleLeaveBattle() {
  await leaveBattle();
}

async function handleBoardCardClick(instance, ownerId, isSelfBoard) {
  if (!match.value || !match.value.ready || !isMyTurn.value || match.value.phase !== "ACTION") {
    return;
  }
  if (pendingSkillTarget.value) {
    await playSkill(pendingSkillTarget.value.instanceId, ownerId, instance.instanceId);
    return;
  }
  if (sacrificeMode.value) {
    if (!isSelfBoard) {
      showToast("只能献祭己方召唤区角色。", "error");
      return;
    }
    await sacrificeCard(instance.instanceId);
    return;
  }
  if (isSelfBoard) {
    if (instance.sleeping) {
      showToast("这个角色本回合不能攻击。", "error");
      return;
    }
    selectedAttackerId.value = instance.instanceId;
    showToast("已选中攻击者，再点敌方角色攻击。", "info");
    return;
  }
  if (!selectedAttackerId.value) {
    showToast("请先选择己方一个可攻击角色。", "error");
    return;
  }
  await attackCharacter(selectedAttackerId.value, instance.instanceId);
}

async function handlePlayerTarget(player) {
  if (!player) {
    return;
  }
  if (pendingSkillTarget.value) {
    await playSkill(pendingSkillTarget.value.instanceId, player.playerId, null);
    return;
  }
  if (selectedAttackerId.value && player.playerId !== selfPlayerId.value && player.board.length === 0) {
    await attackPlayer();
  }
}

function openDetail(instance) {
  detailCard.value = instance;
}

function onKeydown(event) {
  if (event.key === "Escape") {
    if (showDeckComposition.value) {
      showDeckComposition.value = false;
      return;
    }
    if (detailCard.value) {
      detailCard.value = null;
      return;
    }
    if (pendingSkillTarget.value) {
      pendingSkillTarget.value = null;
      return;
    }
    if (pendingSkillDiscard.value) {
      pendingSkillDiscard.value = null;
      return;
    }
    if (sacrificeMode.value) {
      sacrificeMode.value = false;
    }
  }
}

watch(match, (value, oldValue) => {
  if (!value) {
    return;
  }
  synchronizeBoardSlotMemory(value);
  schedulePersistSession();
  processMatchAnimations(value, oldValue);
  if (value.phase === "FINISHED" && !resultHandled.value) {
    resultHandled.value = true;
    const isWinner = value.winnerId === selfPlayerId.value;
    showToast(isWinner ? "对局结束，你获胜了。" : "对局结束，你失败了。", isWinner ? "success" : "info");
    showResultModal.value = true;
  }
});

watch(() => handCards.value.length, () => {
  nextTick(updateHandRowWidth);
});

onMounted(async () => {
  document.addEventListener("keydown", onKeydown);
  if (window.ResizeObserver) {
    handRowResizeObserver = new ResizeObserver(updateHandRowWidth);
    if (handRowRef.value) {
      handRowResizeObserver.observe(handRowRef.value);
    }
  } else {
    window.addEventListener("resize", updateHandRowWidth);
  }
  nextTick(updateHandRowWidth);
  try {
    const config = await api("/game/config");
    assetBaseUrl.value = (config.assetBaseUrl || assetRoot()).trim();
    const cards = await api("/game/cards");
    cardsMap.value = Object.fromEntries(cards.map(card => [card.id, card]));
    await restoreMatchSession();
  } catch (error) {
    showToast(error.message, "error");
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
  if (handRowResizeObserver) {
    handRowResizeObserver.disconnect();
    handRowResizeObserver = null;
  } else {
    window.removeEventListener("resize", updateHandRowWidth);
  }
  stopRealtime();
  if (sessionPersistTimer) {
    window.clearTimeout(sessionPersistTimer);
    sessionPersistTimer = null;
  }
  effectTimers.forEach(timer => window.clearTimeout(timer));
  effectTimers.clear();
});
</script>
