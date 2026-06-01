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
                <span class="battle-headline-label">对局状态</span>
                <strong>{{ battleHeadline }}</strong>
                <span class="battle-headline-subtext">{{ battleSubtext }}</span>
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
              <div class="effect-summary">
                <div class="compact-effects">
                  <span>对方状态</span>
                  <div class="effect-list">
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
                </div>
                <div class="compact-effects">
                  <span>我方状态</span>
                  <div class="effect-list">
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
                </div>
              </div>
            </div>

            <div class="battle-main-grid">
              <div class="battle-lanes">
                <div class="battle-player-row">
                <div
                  class="player-avatar"
                  :class="playerAvatarClass(opponentPlayer)"
                  @click="handlePlayerTarget(opponentPlayer)"
                >
                    <div>{{ opponentPlayerLabel }}</div>
                    <span>{{ opponentPlayer ? `${opponentPlayer.hp} / 100` : "" }}</span>
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
                        @click="handleBoardCardClick(slot.card, opponentPlayer.playerId, false)"
                        @dblclick="openDetail(slot.card)"
                      >
                        <div class="card-surface">
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
                              <div>
                                <div class="card-kind">{{ describeType(cardDef(slot.card.cardId).type) }}</div>
                                <div class="card-name">{{ cardDef(slot.card.cardId).name }}</div>
                              </div>
                            </div>
                            <div class="mini-stats">
                              <span>攻 {{ describeAttack(cardDef(slot.card.cardId), slot.card.formIndex || 0) }}</span>
                              <span>体 {{ slot.card.currentHealth || cardDef(slot.card.cardId).health || 0 }}</span>
                            </div>
                          </div>
                        </div>
                      </article>
                    </div>
                  </div>
                </div>

                <div class="battle-player-row">
                <div
                  class="player-avatar"
                  :class="playerAvatarClass(selfPlayer)"
                  @click="handlePlayerTarget(selfPlayer)"
                >
                    <div>{{ selfPlayerLabel }}</div>
                    <span>{{ selfPlayer ? `${selfPlayer.hp} / 100` : "" }}</span>
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
                        @click="handleBoardCardClick(slot.card, selfPlayer.playerId, true)"
                        @dblclick="openDetail(slot.card)"
                      >
                        <div class="card-surface">
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
                              <div>
                                <div class="card-kind">{{ describeType(cardDef(slot.card.cardId).type) }}</div>
                                <div class="card-name">{{ cardDef(slot.card.cardId).name }}</div>
                              </div>
                            </div>
                            <div class="mini-stats">
                              <span>攻 {{ describeAttack(cardDef(slot.card.cardId), slot.card.formIndex || 0) }}</span>
                              <span>体 {{ slot.card.currentHealth || cardDef(slot.card.cardId).health || 0 }}</span>
                            </div>
                          </div>
                        </div>
                      </article>
                    </div>
                  </div>
                </div>
              </div>

              <aside class="battle-right-rail">
                <div class="pile-box">墓地<br>{{ match.discardPile?.length || 0 }}</div>
                <div class="pile-box draw-pile-box" :class="{ 'fx-draw-source': drawPulse }">抽牌堆<br>{{ match.drawPile?.length || 0 }}</div>

                <section class="battle-log-panel">
                  <div class="zone-title">日志</div>
                  <div ref="logsRef" class="logs">
                    <div v-if="!battleLogs.length" class="log-entry empty">暂无日志</div>
                    <div
                      v-for="(log, index) in battleLogs"
                      :key="logKey(log, index)"
                      class="log-entry"
                      :class="{ latest: highlightedLogKey === logKey(log, index) }"
                    >
                      <span class="log-index">{{ String(index + 1).padStart(2, "0") }}</span>
                      <span class="log-text">{{ log }}</span>
                    </div>
                  </div>
                </section>
              </aside>
            </div>

            <div class="battle-bottom-row">
              <div class="hand-zone" :class="{ 'fx-draw-receive': drawPulse }">
                <div class="zone-title">手牌区</div>
                <div class="hand-row">
                  <div v-if="!selfPlayer?.hand?.length" class="board-empty">暂无手牌</div>
                  <article
                    v-for="card in selfPlayer?.hand || []"
                    :key="card.instanceId"
                    class="card selectable"
                    :class="handCardClass(card)"
                    @click="selectedHandId = card.instanceId"
                    @dblclick="openDetail(card)"
                  >
                    <div class="card-surface">
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
                          <div>
                            <div class="card-kind">{{ describeType(cardDef(card.cardId).type) }}</div>
                            <div class="card-name">{{ cardDef(card.cardId).name }}</div>
                          </div>
                        </div>
                        <div class="mini-stats">
                          <template v-if="cardDef(card.cardId).type === 'CHARACTER'">
                            <span>攻 {{ describeAttack(cardDef(card.cardId)) }}</span>
                            <span>体 {{ cardDef(card.cardId).secondaryHealth != null ? `${cardDef(card.cardId).health || 0}/${cardDef(card.cardId).secondaryHealth}` : (cardDef(card.cardId).health || 0) }}</span>
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
                  <button :disabled="!canConfirmSelectedHand" @click="confirmSelectedHand">确定</button>
                  <button :disabled="!canDrawCards" @click="drawPhase">抽牌</button>
                  <button class="alt" @click="endTurn">结束回合</button>
                  <button class="alt danger-button" :disabled="leavingBattle" @click="handleLeaveBattle">退出对局</button>
                </div>
              </div>
            </div>
          </section>
        </section>
      </div>
    </section>
  </main>

  <div class="detail-modal" :class="{ visible: !!detailCard }" @click.self="detailCard = null">
    <div v-if="detailCard" class="detail-card">
      <button class="detail-close alt" type="button" @click="detailCard = null">关闭</button>
      <div class="card-surface">
        <div class="card-figure" :class="{ 'no-image': !detailImage }" :data-mark="detailMark">
          <img v-if="detailImage" :src="detailImage" :alt="detailDefinition.name" @error="handleImageError($event, detailCard?.cardId || detailCard?.id)">
        </div>
        <div class="card-overlay"></div>
        <div class="card-body">
          <div class="card-top">
            <div>
              <div class="card-kind">{{ describeType(detailDefinition.type) }}</div>
              <div class="card-name">{{ detailDefinition.name }}</div>
            </div>
          </div>
          <div class="stat-badges">
            <template v-if="detailDefinition.type === 'CHARACTER'">
              <span class="stat">攻击 {{ describeAttack(detailDefinition, detailCard?.formIndex || 0) }}</span>
              <span class="stat">体力 {{ detailCurrentHealth }}</span>
            </template>
            <template v-else>
              <span class="stat">作用范围 {{ describeSkillRange(detailDefinition.skillRange) }}</span>
              <span class="stat">效果分类 {{ describeEffectCategory(detailDefinition.effectCategory) }}</span>
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
import { api, buildInviteLink, cardImage, describeAttack, describeEffect, describeEffectCategory, describeEffectType, describeSkillRange, describeType, getInviteBlockedReason, isMissingRoomError, swapCardImageToFallback } from "../lib/game";
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
const selectedAttackerId = ref("");
const pendingSkillTarget = ref(null);
const pollTimer = ref(null);
const socket = ref(null);
const detailCard = ref(null);
const logsRef = ref(null);
const highlightedLogKey = ref("");
const summonedCardIds = ref([]);
const attackingCardIds = ref([]);
const damagedCardIds = ref([]);
const damagedPlayerIds = ref([]);
const drawnHandIds = ref([]);
const drawPulse = ref(false);
const leavingBattle = ref(false);
const resultHandled = ref(false);
const effectTimers = new Set();

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
  return match.value.ready ? `第 ${match.value.turn} 回合` : "等待第二位玩家";
});

const phaseText = computed(() => describePhase(match.value?.phase));

const phaseInfo = computed(() => {
  if (!match.value) {
    return "尚未就绪";
  }
  return `阶段 ${phaseText.value} | 当前行动方 ${match.value.currentPlayerId} | ${isMyTurn.value ? "轮到你" : "等待对方"}`;
});

const emptyStateText = computed(() => {
  if (roomCode.value) {
    return `正在恢复房间 ${roomCode.value}，如果长时间没有响应，请回首页重新加入。`;
  }
  return "还没有对局会话。请先回到首页创建或加入房间。";
});

const canDrawCards = computed(() => {
  return !!match.value && match.value.ready && isMyTurn.value && match.value.phase === "DRAW";
});

const canConfirmSelectedHand = computed(() => {
  const my = selfPlayer.value;
  if (!match.value || !match.value.ready || !my || !selectedHandId.value || !isMyTurn.value || match.value.phase !== "ACTION") {
    return false;
  }
  const selectedCard = my.hand.find(card => card.instanceId === selectedHandId.value);
  if (!selectedCard) {
    return false;
  }
  const definition = cardDef(selectedCard.cardId);
  if (definition.type === "CHARACTER") {
    return my.board.length < 3 && my.summonsThisTurn < 1;
  }
  return definition.type === "SKILL";
});

const modeTitle = computed(() => {
  if (pendingSkillTarget.value) {
    return "当前模式：选择技能目标";
  }
  if (selectedAttackerId.value) {
    return "当前模式：攻击目标选择";
  }
  if (selectedHandId.value) {
    return "当前模式：手牌已选择";
  }
  return "当前模式：空闲";
});

const modeText = computed(() => {
  const my = selfPlayer.value;
  if (!match.value || !my) {
    return "没有正在进行的操作。";
  }
  if (pendingSkillTarget.value) {
    const selectedCard = my.hand.find(card => card.instanceId === pendingSkillTarget.value.instanceId);
    const definition = selectedCard ? cardDef(selectedCard.cardId) : null;
    return definition
      ? `请为 ${definition.name} 选择一个角色或玩家目标。`
      : "请为当前技能选择一个角色或玩家目标。";
  }
  if (selectedAttackerId.value) {
    const attacker = my.board.find(card => card.instanceId === selectedAttackerId.value);
    const definition = attacker ? cardDef(attacker.cardId) : null;
    return definition ? `已选择攻击者 ${definition.name}。` : "已选择攻击者。";
  }
  if (selectedHandId.value) {
    const selectedCard = my.hand.find(card => card.instanceId === selectedHandId.value);
    const definition = selectedCard ? cardDef(selectedCard.cardId) : null;
    return definition ? `已选择 ${definition.name}。点击“确定”使用。` : "已选择一张手牌。";
  }
  return isMyTurn.value ? "可选择手牌出牌，或选择场上可攻击角色。" : "当前不是你的回合，等待对方操作。";
});

const enemySlots = computed(() => {
  return Array.from({ length: 3 }, (_, index) => ({
    slot: index + 1,
    card: opponentPlayer.value?.board?.[index] || null
  }));
});

const selfSlots = computed(() => {
  return Array.from({ length: 3 }, (_, index) => ({
    slot: index + 1,
    card: selfPlayer.value?.board?.[index] || null
  }));
});

const opponentEffects = computed(() => toEffectBadges(opponentPlayer.value));
const selfEffects = computed(() => toEffectBadges(selfPlayer.value));
const battleLogs = computed(() => match.value?.logs || []);
const inviteBlockedReason = computed(() => {
  if (!roomCode.value) {
    return "当前没有可分享的房间";
  }
  return getInviteBlockedReason(match.value);
});
const canCopyInviteLink = computed(() => !!roomCode.value && !inviteBlockedReason.value);
const battleHeadline = computed(() => {
  if (!match.value) {
    return "等待对局开始";
  }
  if (match.value.phase === "FINISHED") {
    return winnerText.value;
  }
  return `${phaseText.value} · ${turnInfo.value}`;
});
const battleSubtext = computed(() => {
  if (!match.value) {
    return "房间建立后，等待对局数据同步。";
  }
  if (match.value.phase === "FINISHED") {
    return "对局已结束，即将退出房间。";
  }
  return `房间 ${roomCode.value || "------"} · ${phaseInfo.value} · ${modeTitle.value}。${modeText.value}`;
});
const winnerText = computed(() => {
  if (!match.value?.winnerId) {
    return "对局结束";
  }
  if (match.value.winnerId === selfPlayerId.value) {
    return "你获胜了";
  }
  return "你失败了";
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
const detailCurrentHealth = computed(() => detailCard.value?.currentHealth || detailDefinition.value.health || 0);

function cardDef(cardId) {
  return cardsMap.value[cardId] || { id: cardId, name: cardId, description: "", type: "UNKNOWN" };
}

function cardImageFor(cardId) {
  return cardImage(cardId, cardsMap.value, assetBaseUrl.value);
}

function cardMark(cardId) {
  return cardDef(cardId).type === "SKILL" ? "技" : "角";
}

function describePhase(phase) {
  return PHASE_TEXT[phase] || phase || "未开始";
}

function toEffectBadges(player) {
  return (player?.statusEffects || []).map((effect, index) => ({
    key: `${effect.type}-${effect.remainingTurns ?? "none"}-${index}`,
    label: describeEffect(effect),
    category: effect.category === "debuff" ? "debuff" : "buff"
  }));
}

function logKey(log, index) {
  return `${index}-${log}`;
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

function scrollLogsToBottom() {
  nextTick(() => {
    if (logsRef.value) {
      logsRef.value.scrollTop = logsRef.value.scrollHeight;
    }
  });
}

function mapInstances(list = []) {
  return new Map(list.map(card => [card.instanceId, card]));
}

function processMatchAnimations(nextMatch, previousMatch) {
  if (!nextMatch) {
    return;
  }
  if (!previousMatch) {
    scrollLogsToBottom();
    return;
  }

  const previousPlayers = previousMatch.players || [];
  const nextPlayers = nextMatch.players || [];

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
        const nextHealth = card.currentHealth ?? card.health ?? 0;
        const previousHealth = previousCard.currentHealth ?? previousCard.health ?? 0;
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
        pulseDraw(900);
      }
    }
  });

  if ((nextMatch.logs?.length || 0) > (previousMatch.logs?.length || 0)) {
    const lastIndex = nextMatch.logs.length - 1;
    const latestKey = logKey(nextMatch.logs[lastIndex], lastIndex);
    highlightedLogKey.value = latestKey;
    queueEffectReset(() => {
      if (highlightedLogKey.value === latestKey) {
        highlightedLogKey.value = "";
      }
    }, 1600);
  }

  scrollLogsToBottom();
}

function persistSession() {
  saveSession({
    baseUrl: publicBaseUrl(),
    roomCode: roomCode.value,
    selfPlayerId: selfPlayerId.value,
    playerToken: playerToken.value,
    playerName: playerName.value
  });
}

function resetLocalBattleState() {
  const preservedPlayerName = playerName.value;
  roomCode.value = "";
  selfPlayerId.value = "";
  playerToken.value = "";
  playerName.value = preservedPlayerName;
  match.value = null;
  selectedHandId.value = "";
  selectedAttackerId.value = "";
  pendingSkillTarget.value = null;
  detailCard.value = null;
  highlightedLogKey.value = "";
  summonedCardIds.value = [];
  attackingCardIds.value = [];
  damagedCardIds.value = [];
  damagedPlayerIds.value = [];
  drawnHandIds.value = [];
  drawPulse.value = false;
  resultHandled.value = false;
}

function stopRealtime() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
  if (socket.value) {
    socket.value.close();
    socket.value = null;
  }
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
      await api(`/api/rooms/${roomCode.value}/leave`, {
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
  if (pendingSkillTarget.value) {
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

function boardCardClass(instance, isSelfBoard) {
  const canAttack = isSelfBoard && isMyTurn.value && match.value?.phase === "ACTION" && !instance.sleeping;
  const targetable = (!isSelfBoard && !!selectedAttackerId.value && isMyTurn.value && match.value?.phase === "ACTION")
    || !!pendingSkillTarget.value;
  return {
    selectable: canAttack,
    "attacker-selected": selectedAttackerId.value === instance.instanceId,
    "attack-target": targetable,
    "fx-summoned": summonedCardIds.value.includes(instance.instanceId),
    "fx-attacking": attackingCardIds.value.includes(instance.instanceId),
    "fx-hit": damagedCardIds.value.includes(instance.instanceId)
  };
}

function handCardClass(instance) {
  return {
    selected: selectedHandId.value === instance.instanceId,
    "fx-draw-arrival": drawnHandIds.value.includes(instance.instanceId)
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
    match.value = await api(`/api/rooms/${roomCode.value}`);
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
  pollTimer.value = setInterval(async () => {
    try {
      await refreshRoom();
    } catch {
    }
  }, 1800);
}

function connectSocket() {
  if (!roomCode.value) {
    return;
  }
  if (socket.value) {
    socket.value.close();
  }
  socket.value = new WebSocket(`${wsRoot()}${wsGamePath()}?roomCode=${encodeURIComponent(roomCode.value)}`);
  socket.value.onmessage = event => {
    try {
      match.value = JSON.parse(event.data);
      persistSession();
    } catch {
    }
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
    const session = await api(`/api/rooms/${roomCode.value}/session/${playerToken.value}`);
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

async function summonCard(instanceId) {
  try {
    match.value = await api(`/api/matches/${match.value.matchId}/summon`, {
      method: "POST",
      body: JSON.stringify({ playerId: selfPlayerId.value, handInstanceId: instanceId })
    });
    selectedHandId.value = "";
    persistSession();
    showToast("角色已召唤", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function playSkill(instanceId, targetPlayerId = null, targetInstanceId = null) {
  try {
    const skillCard = selfPlayer.value?.hand.find(card => card.instanceId === instanceId);
    const definition = skillCard ? cardDef(skillCard.cardId) : null;
    if (definition?.skillRange === "SINGLE" && !targetPlayerId) {
      pendingSkillTarget.value = { instanceId };
      showToast("请选择一个角色或玩家作为技能目标。", "info");
      return;
    }
    match.value = await api(`/api/matches/${match.value.matchId}/play-skill`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        handInstanceId: instanceId,
        targetPlayerId,
        targetInstanceId
      })
    });
    pendingSkillTarget.value = null;
    selectedHandId.value = "";
    persistSession();
    showToast("技能已使用", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function attackCharacter(attackerInstanceId, defenderInstanceId) {
  try {
    flashIds(attackingCardIds, [attackerInstanceId], 520);
    match.value = await api(`/api/matches/${match.value.matchId}/attack-character`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        attackerInstanceId,
        defenderInstanceId
      })
    });
    selectedAttackerId.value = "";
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
    flashIds(attackingCardIds, [selectedAttackerId.value], 520);
    match.value = await api(`/api/matches/${match.value.matchId}/attack-player`, {
      method: "POST",
      body: JSON.stringify({
        playerId: selfPlayerId.value,
        attackerInstanceId: selectedAttackerId.value
      })
    });
    selectedAttackerId.value = "";
    persistSession();
    showToast("已对玩家造成伤害。", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function drawPhase() {
  if (!canDrawCards.value) {
    return;
  }
  try {
    match.value = await api(`/api/matches/${match.value.matchId}/draw?playerId=${selfPlayerId.value}`, {
      method: "POST"
    });
    persistSession();
    showToast("已完成抽牌阶段", "success");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function endTurn() {
  if (!match.value) {
    return;
  }
  try {
    match.value = await api(`/api/matches/${match.value.matchId}/end-turn?playerId=${selfPlayerId.value}`, {
      method: "POST"
    });
    selectedAttackerId.value = "";
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
    if (detailCard.value) {
      detailCard.value = null;
      return;
    }
    if (pendingSkillTarget.value) {
      pendingSkillTarget.value = null;
    }
  }
}

watch(match, (value, oldValue) => {
  if (!value) {
    return;
  }
  persistSession();
  processMatchAnimations(value, oldValue);
  if (value.phase === "FINISHED" && !resultHandled.value) {
    resultHandled.value = true;
    const isWinner = value.winnerId === selfPlayerId.value;
    showToast(isWinner ? "对局结束，你获胜了。" : "对局结束，你失败了。", isWinner ? "success" : "info");
    window.setTimeout(() => {
      leaveBattle({ remote: false, silent: true });
    }, 1800);
  }
});

onMounted(async () => {
  document.addEventListener("keydown", onKeydown);
  try {
    const config = await api("/api/config");
    assetBaseUrl.value = (config.assetBaseUrl || assetRoot()).trim();
    const cards = await api("/api/cards");
    cardsMap.value = Object.fromEntries(cards.map(card => [card.id, card]));
    await restoreMatchSession();
  } catch (error) {
    showToast(error.message, "error");
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
  }
  if (socket.value) {
    socket.value.close();
  }
  effectTimers.forEach(timer => window.clearTimeout(timer));
  effectTimers.clear();
});
</script>
