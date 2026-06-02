<template>
  <main class="app-shell home-shell">
    <AppTopbar extra-class="home-topbar" subtitle="Campus King"/>

    <section class="home-hero panel">
      <div class="home-hero-copy">
        <p class="eyebrow">Campus King</p>
        <h1>我常常追忆过去...</h1>
        <p class="hero-copy">
          名字暂时保存在当前浏览器。点击“进入对局”后再选择创建房间或加入房间，
          卡牌资料页也可以随时打开查看。
        </p>
        <div class="home-hero-actions">
          <button type="button" @click="openBattleEntry">进入对局</button>
          <RouterLink class="button-link alt" to="/cards">卡牌大全</RouterLink>

        </div>
      </div>

      <aside class="profile-panel">
        <div class="profile-head">
          <span class="profile-label">当前玩家</span>
          <strong>{{ displayPlayerName }}</strong>
        </div>
        <label for="playerName">名字</label>
        <input id="playerName" v-model.trim="playerNameInput" autocomplete="nickname" maxlength="18" placeholder="输入你的名字">
        <p class="profile-help">后续会改为数据库保存；现在先保存在浏览器会话里。</p>
        <div class="profile-meta">
          <span>{{ session.roomCode ? `当前房间 ${session.roomCode}` : "当前未加入房间" }}</span>
          <div class="profile-meta-actions">
            <button class="alt" type="button" @click="copyRoomCode">复制房间码</button>
            <button class="alt" type="button" :disabled="!canCopyInviteLink" :title="inviteBlockedReason" @click="copyInviteLink">复制邀请链接</button>
          </div>
        </div>
      </aside>
    </section>

    <section class="home-overview">
      <article class="overview-card panel">
        <p class="eyebrow">Resume</p>
        <h2>进入对局</h2>
        <p>如果浏览器里已有房间会话，可以直接回到对局页恢复身份。</p>
        <RouterLink class="text-link" to="/battle">继续上次对局</RouterLink>
      </article>
      <article class="overview-card panel">
        <p class="eyebrow">Cards</p>
        <h2>卡牌大全</h2>
        <p>查看角色牌和技能牌详情，支持双击放大高清图。</p>
        <RouterLink class="text-link" to="/cards">打开卡牌资料</RouterLink>
      </article>
      <article class="home-session panel">
        <div>
          <span class="session-label">当前会话</span>
          <strong>{{ session.roomCode ? `房间 ${session.roomCode}` : "未进入房间" }}</strong>
        </div>
        <div>
          <span class="session-label">当前身份</span>
          <strong>{{ session.selfPlayerId ? `座位 ${session.selfPlayerId}` : "尚未分配" }}</strong>
        </div>
        <div>
          <span class="session-label">本地保存</span>
          <strong>浏览器临时会话</strong>
        </div>
      </article>
    </section>
  </main>

  <div class="entry-modal" :class="{ visible: entryMode !== '' }" @click.self="closeEntryModal">
    <section v-if="entryMode" class="entry-dialog panel">
      <button class="entry-close alt" type="button" @click="closeEntryModal">关闭</button>

      <template v-if="entryMode === 'menu'">
        <p class="eyebrow">Battle Entry</p>
        <h2>进入对局</h2>
        <p class="entry-copy">选择创建新房间，或者输入房间码加入现有对局。</p>
        <div class="entry-grid">
          <button class="entry-action" type="button" @click="entryMode = 'create'">
            <span>创建房间</span>
            <small>可直接创建 PVP 或人机房</small>
          </button>
          <button class="entry-action alt-action" type="button" @click="entryMode = 'join'">
            <span>加入房间</span>
            <small>输入已有房间码并进入</small>
          </button>
        </div>
      </template>

      <form v-else-if="entryMode === 'create'" class="entry-form" @submit.prevent="createRoom">
        <p class="eyebrow">Create</p>
        <h2>创建房间</h2>
        <p class="entry-copy">创建后会自动以房主身份进入对局。</p>
        <div class="play-type-options" role="radiogroup" aria-label="玩法选择">
          <button
            v-for="option in playTypeOptions"
            :key="option.value"
            class="play-type-option"
            :class="{ active: playType === option.value }"
            type="button"
            :disabled="option.disabled"
            @click="playType = option.value"
          >
            <span>{{ option.label }}</span>
            <small>{{ option.description }}</small>
          </button>
        </div>
        <label class="checkbox-row">
          <input v-model="botMode" type="checkbox">
          <span>创建人机模式房间</span>
        </label>
        <div class="entry-form-actions">
          <button type="submit">创建并进入</button>
          <button class="alt" type="button" @click="entryMode = 'menu'">返回</button>
        </div>
      </form>

      <form v-else class="entry-form" @submit.prevent="joinRoomFromForm">
        <p class="eyebrow">Join</p>
        <h2>加入房间</h2>
        <p class="entry-copy">输入房主提供的房间码后进入对局。</p>
        <label for="roomCodeInput">房间码</label>
        <input id="roomCodeInput" v-model.trim="roomCodeInput" autocomplete="off" placeholder="例如 BC2485">
        <div class="entry-form-actions">
          <button type="submit">加入并进入</button>
          <button class="alt" type="button" @click="entryMode = 'menu'">返回</button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRouter } from "vue-router";
import AppTopbar from "../components/AppTopbar.vue";
import { api, buildInviteLink, getInviteBlockedReason, isMissingRoomError } from "../lib/game";
import { copyText, publicBaseUrl } from "../lib/runtime-config";
import { clearSession, ensureSessionPlayerName, generateClientToken, loadSession, saveSession } from "../lib/session";
import { showToast } from "../lib/toast";

const router = useRouter();
const session = ref({
  baseUrl: publicBaseUrl(),
  roomCode: "",
  selfPlayerId: "",
  playerToken: "",
  playerName: ""
});
const currentMatch = ref(null);
const playerNameInput = ref("");
const roomCodeInput = ref("");
const botMode = ref(false);
const playType = ref("SINGLE_SIDE");
const playTypeOptions = [
  { value: "SINGLE_SIDE", label: "单面玩法", description: "当前基础玩法" },
  { value: "DOUBLE_SIDE", label: "双面玩法", description: "暂未开放", disabled: true }
];
const entryMode = ref("");

const displayPlayerName = computed(() => playerNameInput.value || session.value.playerName || ensureSessionPlayerName(session.value));
const topbarStatus = computed(() => `${displayPlayerName.value}`);
const connectionInfo = computed(() => {
  if (!session.value.roomCode) {
    return "未加入房间";
  }
  return `已有会话：${session.value.roomCode} / ${session.value.selfPlayerId || "未知身份"}`;
});
const inviteBlockedReason = computed(() => {
  if (!session.value.roomCode) {
    return "当前没有可分享的房间";
  }
  return getInviteBlockedReason(currentMatch.value);
});
const canCopyInviteLink = computed(() => !!session.value.roomCode && !inviteBlockedReason.value);

watch(playerNameInput, value => {
  session.value = {
    ...session.value,
    playerName: value || ensureSessionPlayerName(session.value)
  };
  saveSession(session.value);
});

function persist() {
  saveSession(session.value);
}

function ensurePlayerName() {
  const name = playerNameInput.value.trim() || session.value.playerName || ensureSessionPlayerName(session.value);
  if (!playerNameInput.value.trim()) {
    playerNameInput.value = name;
  }
  session.value = {
    ...session.value,
    playerName: name
  };
  saveSession(session.value);
  return name;
}

function openBattleEntry() {
  if (!ensurePlayerName()) {
    return;
  }
  entryMode.value = "menu";
}

function closeEntryModal() {
  entryMode.value = "";
}

async function createRoom() {
  try {
    const playerName = ensurePlayerName();
    if (!playerName) {
      return;
    }
    const playerToken = generateClientToken();
    const match = await api("/api/rooms", {
      method: "POST",
      body: JSON.stringify({
        hostName: playerName,
        playerToken,
        botMode: botMode.value,
        playType: playType.value
      })
    });
    session.value = {
      baseUrl: publicBaseUrl(),
      roomCode: match.roomCode,
      selfPlayerId: "P1",
      playerToken,
      playerName
    };
    currentMatch.value = match;
    persist();
    closeEntryModal();
    router.push("/battle");
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function joinRoom(roomCodeOverride = "", options = {}) {
  const { closeModalOnSuccess = true, successMessage = "" } = options;
  try {
    const playerName = ensurePlayerName();
    if (!playerName) {
      return false;
    }
    const roomCodeSource = typeof roomCodeOverride === "string" ? roomCodeOverride : roomCodeInput.value;
    const roomCode = String(roomCodeSource || "").trim().toUpperCase();
    if (!roomCode) {
      showToast("请输入房间码", "error");
      return false;
    }
    const playerToken = generateClientToken();
    const match = await api(`/api/rooms/${roomCode}/join`, {
      method: "POST",
      body: JSON.stringify({ playerName, playerToken })
    });
    session.value = {
      baseUrl: publicBaseUrl(),
      roomCode,
      selfPlayerId: "P2",
      playerToken,
      playerName
    };
    currentMatch.value = match;
    persist();
    if (closeModalOnSuccess) {
      closeEntryModal();
    }
    if (successMessage) {
      showToast(successMessage, "success");
    }
    router.push("/battle");
    return true;
  } catch (error) {
    showToast(error.message, "error");
    return false;
  }
}

async function joinRoomFromForm(event) {
  const formValue = event?.currentTarget?.elements?.roomCodeInput?.value || "";
  if (formValue) {
    roomCodeInput.value = formValue;
  }
  return joinRoom(formValue);
}

async function copyRoomCode() {
  if (!session.value.roomCode) {
    showToast("当前没有房间码", "error");
    return;
  }
  try {
    await copyText(session.value.roomCode);
    showToast("房间码已复制", "success");
  } catch {
    showToast("复制失败", "error");
  }
}

async function copyInviteLink() {
  if (!session.value.roomCode) {
    showToast("当前没有可分享的房间", "error");
    return;
  }
  if (inviteBlockedReason.value) {
    showToast(inviteBlockedReason.value, "error");
    return;
  }
  try {
    await copyText(buildInviteLink(session.value.roomCode, session.value.baseUrl || publicBaseUrl()));
    showToast("邀请链接已复制", "success");
  } catch {
    showToast("复制失败", "error");
  }
}

function readInviteRoomCode() {
  const searchRoomCode = new URLSearchParams(window.location.search).get("roomID");
  if (searchRoomCode) {
    return searchRoomCode.trim().toUpperCase();
  }
  const hash = window.location.hash || "";
  const hashQueryIndex = hash.indexOf("?");
  if (hashQueryIndex < 0) {
    return "";
  }
  const hashRoomCode = new URLSearchParams(hash.slice(hashQueryIndex + 1)).get("roomID");
  return (hashRoomCode || "").trim().toUpperCase();
}

function clearInviteRoomCode() {
  const currentUrl = new URL(window.location.href);
  let updated = false;
  if (currentUrl.searchParams.has("roomID")) {
    currentUrl.searchParams.delete("roomID");
    updated = true;
  }
  if (currentUrl.hash.includes("?")) {
    const [hashPath, hashQuery = ""] = currentUrl.hash.slice(1).split("?");
    const params = new URLSearchParams(hashQuery);
    if (params.has("roomID")) {
      params.delete("roomID");
      const nextHashQuery = params.toString();
      currentUrl.hash = nextHashQuery ? `#${hashPath}?${nextHashQuery}` : `#${hashPath}`;
      updated = true;
    }
  }
  if (!updated) {
    return;
  }
  const nextUrl = `${currentUrl.origin}${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`;
  window.history.replaceState({}, "", nextUrl);
}

async function leavePreviousRoomIfNeeded(nextRoomCode) {
  const saved = loadSession();
  if (!saved?.roomCode || !saved?.playerToken || saved.roomCode === nextRoomCode) {
    return;
  }
  try {
    await api(`/api/rooms/${saved.roomCode}/leave`, {
      method: "POST",
      body: JSON.stringify({ playerToken: saved.playerToken })
    });
  } catch {
  }
}

async function handleInviteJoin() {
  const inviteRoomCode = readInviteRoomCode();
  if (!inviteRoomCode) {
    return;
  }

  roomCodeInput.value = inviteRoomCode;
  clearInviteRoomCode();

  const saved = loadSession();
  if (saved?.roomCode === inviteRoomCode && saved?.playerToken) {
    try {
      await api(`/api/rooms/${inviteRoomCode}/session/${saved.playerToken}`);
      showToast("检测到邀请链接，正在恢复房间", "success");
      router.replace("/battle");
    } catch (error) {
      if (isMissingRoomError(error)) {
        clearSession();
        session.value = {
          baseUrl: publicBaseUrl(),
          roomCode: "",
          selfPlayerId: "",
          playerToken: "",
          playerName: ensureSessionPlayerName(saved)
        };
        playerNameInput.value = session.value.playerName;
        roomCodeInput.value = inviteRoomCode;
        showToast("邀请房间不存在，请确认房间码。", "error");
      } else {
        showToast(error.message, "error");
      }
    }
    return;
  }

  await leavePreviousRoomIfNeeded(inviteRoomCode);
  const joined = await joinRoom(inviteRoomCode, {
    closeModalOnSuccess: false,
    successMessage: `已通过邀请链接加入房间 ${inviteRoomCode}`
  });

  if (!joined) {
    entryMode.value = "join";
  }
}

onMounted(async () => {
  const saved = loadSession();
  if (saved) {
    session.value = {
      baseUrl: saved.baseUrl || publicBaseUrl(),
      roomCode: saved.roomCode || "",
      selfPlayerId: saved.selfPlayerId || "",
      playerToken: saved.playerToken || "",
      playerName: saved.playerName || ensureSessionPlayerName(saved)
    };
    playerNameInput.value = session.value.playerName;
    roomCodeInput.value = session.value.roomCode;
    if (session.value.roomCode) {
      try {
        currentMatch.value = await api(`/api/rooms/${session.value.roomCode}`);
      } catch (error) {
        if (isMissingRoomError(error)) {
          clearSession();
          session.value = {
            baseUrl: publicBaseUrl(),
            roomCode: "",
            selfPlayerId: "",
            playerToken: "",
            playerName: playerNameInput.value
          };
          roomCodeInput.value = "";
        }
      }
    }
  }
  await handleInviteJoin();
});
</script>
