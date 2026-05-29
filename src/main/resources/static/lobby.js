const STORAGE_KEY = "campusking-session";

const state = {
    baseUrl: window.location.origin,
    roomCode: "",
    selfPlayerId: "",
    playerToken: "",
    playerName: ""
};

const playerNameInput = document.getElementById("playerName");
const joinPlayerNameInput = document.getElementById("joinPlayerName");
const roomCodeInput = document.getElementById("roomCodeInput");
const botModeInput = document.getElementById("botModeInput");

function toast(message, type = "info") {
    window.Toast.show(message, type);
}

function generateClientToken() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
        return window.crypto.randomUUID().replaceAll("-", "");
    }
    if (window.crypto && typeof window.crypto.getRandomValues === "function") {
        const buffer = new Uint8Array(16);
        window.crypto.getRandomValues(buffer);
        return Array.from(buffer, value => value.toString(16).padStart(2, "0")).join("");
    }
    return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`;
}

async function api(path, options = {}) {
    const response = await fetch(`${state.baseUrl}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "请求失败");
    }
    return data;
}

function saveSession() {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
        baseUrl: state.baseUrl,
        roomCode: state.roomCode,
        selfPlayerId: state.selfPlayerId,
        playerToken: state.playerToken,
        playerName: state.playerName
    }));
}

function restoreSession() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        if (!raw) {
            renderStatus();
            return;
        }
        const saved = JSON.parse(raw);
        state.baseUrl = saved.baseUrl || window.location.origin;
        state.roomCode = saved.roomCode || "";
        state.selfPlayerId = saved.selfPlayerId || "";
        state.playerToken = saved.playerToken || "";
        state.playerName = saved.playerName || "";
        if (state.playerName) {
            playerNameInput.value = state.playerName;
            joinPlayerNameInput.value = state.playerName;
        }
        if (state.roomCode) {
            roomCodeInput.value = state.roomCode;
        }
        renderStatus();
    } catch {
        sessionStorage.removeItem(STORAGE_KEY);
        renderStatus();
    }
}

function renderStatus() {
    const roomCodeText = document.getElementById("roomCodeText");
    const connectionInfo = document.getElementById("connectionInfo");
    if (roomCodeText) {
        roomCodeText.textContent = state.roomCode || "------";
    }
    if (connectionInfo) {
        connectionInfo.textContent = state.roomCode
            ? `已有会话：${state.roomCode} / ${state.selfPlayerId || "未知身份"}`
            : "未加入房间";
    }
}

async function createRoom(event) {
    event.preventDefault();
    try {
        state.playerName = playerNameInput.value.trim() || "玩家";
        state.playerToken = generateClientToken();
        const match = await api("/api/rooms", {
            method: "POST",
            body: JSON.stringify({
                hostName: state.playerName,
                playerToken: state.playerToken,
                botMode: !!botModeInput.checked
            })
        });
        state.roomCode = match.roomCode;
        state.selfPlayerId = "P1";
        saveSession();
        window.location.href = "battle.html";
    } catch (error) {
        toast(error.message, "error");
    }
}

async function joinRoom(event) {
    event.preventDefault();
    try {
        const roomCode = roomCodeInput.value.trim().toUpperCase();
        if (!roomCode) {
            toast("请输入房间码。", "error");
            return;
        }
        state.playerName = joinPlayerNameInput.value.trim() || playerNameInput.value.trim() || "玩家";
        state.playerToken = generateClientToken();
        await api(`/api/rooms/${roomCode}/join`, {
            method: "POST",
            body: JSON.stringify({
                playerName: state.playerName,
                playerToken: state.playerToken
            })
        });
        state.roomCode = roomCode;
        state.selfPlayerId = "P2";
        saveSession();
        window.location.href = "battle.html";
    } catch (error) {
        toast(error.message, "error");
    }
}

document.getElementById("createRoomForm").addEventListener("submit", createRoom);
document.getElementById("joinRoomForm").addEventListener("submit", joinRoom);
document.getElementById("copyRoomBtn").addEventListener("click", async () => {
    if (!state.roomCode) {
        toast("当前没有房间码。", "error");
        return;
    }
    try {
        await navigator.clipboard.writeText(state.roomCode);
        toast("房间码已复制。", "success");
    } catch {
        roomCodeInput.select();
        document.execCommand("copy");
        toast("房间码已复制。", "success");
    }
});

restoreSession();
