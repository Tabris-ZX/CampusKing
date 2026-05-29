const STORAGE_KEY = "campusking-session";

const state = {
    baseUrl: window.location.origin,
    assetBaseUrl: "",
    roomCode: "",
    match: null,
    selfPlayerId: "",
    playerToken: "",
    playerName: "",
    selectedHandId: "",
    selectedAttackerId: "",
    pendingSkillTarget: null,
    seenHandInstanceIds: [],
    seenBoardInstanceIds: [],
    pollTimer: null,
    socket: null,
    cards: {}
};

const playerNameInput = document.getElementById("playerName");
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

function saveSession() {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
        baseUrl: state.baseUrl,
        roomCode: state.roomCode,
        selfPlayerId: state.selfPlayerId,
        playerToken: state.playerToken,
        playerName: playerNameInput?.value?.trim() || state.playerName || ""
    }));
}

function restoreSession() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return;
        }
        const saved = JSON.parse(raw);
        if (saved.baseUrl) {
            state.baseUrl = saved.baseUrl;
        }
        if (saved.roomCode) {
            state.roomCode = saved.roomCode;
            if (roomCodeInput) {
                roomCodeInput.value = saved.roomCode;
            }
        }
        if (saved.selfPlayerId) {
            state.selfPlayerId = saved.selfPlayerId;
        }
        if (saved.playerToken) {
            state.playerToken = saved.playerToken;
        }
        if (saved.playerName) {
            state.playerName = saved.playerName;
            if (playerNameInput) {
                playerNameInput.value = saved.playerName;
            }
        }
    } catch {
        sessionStorage.removeItem(STORAGE_KEY);
    }
}

async function api(path, options = {}) {
    const base = state.baseUrl || window.location.origin;
    state.baseUrl = base;
    const response = await fetch(`${base}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "请求失败");
    }
    return data;
}

async function loadCards() {
    const list = await api("/api/cards");
    state.cards = Object.fromEntries(list.map(card => [card.id, card]));
}

async function loadClientConfig() {
    try {
        const config = await api("/api/config");
        state.assetBaseUrl = (config.assetBaseUrl || "").trim();
    } catch {
        state.assetBaseUrl = "";
    }
}

async function loadBattleTemplates() {
    const templateIds = ["battleTemplate", "playerRowTemplate", "emptySlotTemplate", "cardTemplate"];
    if (templateIds.every(id => document.getElementById(id))) {
        return;
    }
    templateIds.forEach(id => {
        const existing = document.getElementById(id);
        if (existing) {
            existing.remove();
        }
    });
    const response = await fetch("battle.html");
    if (!response.ok) {
        throw new Error("无法加载 battle.html");
    }
    const doc = new DOMParser().parseFromString(await response.text(), "text/html");
    const host = document.createElement("div");
    host.hidden = true;
    templateIds.forEach(id => {
        const template = doc.getElementById(id);
        if (!template) {
            throw new Error(`battle.html 缺少模板：${id}`);
        }
        host.appendChild(template.cloneNode(true));
    });
    document.body.appendChild(host);
}

function getCard(cardId) {
    return state.cards[cardId] || { id: cardId, name: cardId, description: "", type: "UNKNOWN" };
}

function selfPlayer() {
    return state.match?.players.find(player => player.playerId === state.selfPlayerId) || null;
}

function opponentPlayer() {
    return state.match?.players.find(player => player.playerId !== state.selfPlayerId) || null;
}

function isMyTurn() {
    return !!state.match && state.match.currentPlayerId === state.selfPlayerId;
}

function canConfirmSelectedHand() {
    const my = selfPlayer();
    if (!state.match || !state.match.ready || !my || !state.selectedHandId || !isMyTurn() || state.match.phase !== "ACTION") {
        return false;
    }
    const selectedCard = my.hand.find(card => card.instanceId === state.selectedHandId);
    if (!selectedCard) {
        return false;
    }
    const definition = getCard(selectedCard.cardId);
    if (definition.type === "CHARACTER") {
        return my.board.length < 3 && my.summonsThisTurn < 1;
    }
    return definition.type === "SKILL";
}

function cardImage(cardId) {
    const card = getCard(cardId);
    if (!card || !card.id) {
        return "";
    }
    const folder = card.type === "SKILL" ? "skills" : "characters";
    const base = (state.assetBaseUrl || state.baseUrl || window.location.origin || "").replace(/\/$/, "");
    return `${base}/images/texture/${folder}/${card.id}.png`;
}

function cardImageMarkup(image, alt) {
    if (!image) {
        return "";
    }
    return `<img src="${image}" alt="${alt}" onerror="this.remove()">`;
}

function render() {
    const match = state.match;
    setTextById("roomCodeText", state.roomCode || "------");
    setTextById("connectionInfo", state.selfPlayerId ? `已连接 ${state.baseUrl} / 身份 ${state.selfPlayerId}` : "未加入房间");

    if (!match) {
        setTextById("turnInfo", "等待开始");
        setTextById("phaseInfo", "尚未就绪");
        setTextById("actionHint", "先创建或加入房间。");
        setHtmlById("arena", state.roomCode
            ? `<div class="board-empty">正在恢复房间 ${state.roomCode}，如果长时间没有响应，请回大厅重新加入。</div>`
            : `<div class="board-empty">还没有对局会话。请先回到大厅创建或加入房间。</div>`);
        setTextById("logs", "");
        renderSelectionPanel();
        return;
    }

    setTextById("turnInfo", match.ready ? `第 ${match.turn} 回合` : "等待第二位玩家");
    setTextById("phaseInfo", `阶段 ${match.phase} | 当前行动方 ${match.currentPlayerId} | ${isMyTurn() ? "轮到你" : "等待对方"}`);
    setTextById("actionHint", !match.ready
        ? "房间已创建，等待另一位玩家加入。"
        : isMyTurn()
            ? "选中手牌后点确定出牌。点击己方角色后再点敌方角色攻击。"
            : "当前不是你的回合。");

    setHtmlById("arena", renderBattleLayout(match));
    setTextById("logs", (match.logs || []).join("\n"));
    syncBattleLogs(match);
    renderModePanel();
    renderSelectionPanel();
    bindInteractions();
    bindTurnActionButtons();
    rememberRenderedInstances();
    saveSession();
}

function syncBattleLogs(match) {
    const battleLogs = document.getElementById("battleLogs");
    if (battleLogs) {
        battleLogs.textContent = (match.logs || []).join("\n");
    }
}

function rememberRenderedInstances() {
    const my = selfPlayer();
    const enemy = opponentPlayer();
    state.seenHandInstanceIds = my ? my.hand.map(card => card.instanceId) : [];
    state.seenBoardInstanceIds = [
        ...(my ? my.board.map(card => card.instanceId) : []),
        ...(enemy ? enemy.board.map(card => card.instanceId) : [])
    ];
}

function renderModePanel() {
    const title = document.getElementById("modeTitle");
    const text = document.getElementById("modeText");
    const my = selfPlayer();

    if (!title || !text) {
        return;
    }

    if (!state.match || !my) {
        title.textContent = "当前模式：空闲";
        text.textContent = "没有正在进行的操作。";
        return;
    }

    if (state.pendingSkillTarget) {
        const selectedCard = my.hand.find(card => card.instanceId === state.pendingSkillTarget.instanceId);
        const definition = selectedCard ? getCard(selectedCard.cardId) : null;
        title.textContent = "当前模式：选择技能目标";
        text.textContent = definition
            ? `请为 ${definition.name} 选择一个角色或玩家目标。点击空白后可用“取消选择”退出。`
            : "请为当前技能选择一个角色或玩家目标。";
        return;
    }

    if (state.selectedAttackerId) {
        const attacker = my.board.find(card => card.instanceId === state.selectedAttackerId);
        const definition = attacker ? getCard(attacker.cardId) : null;
        title.textContent = "当前模式：攻击目标选择";
        text.textContent = definition
            ? `已选择攻击者 ${definition.name}。若敌方场上有角色，请点击敌方角色；若敌方场上为空，可点击敌方玩家头像直接攻击。`
            : "已选择攻击者。若敌方场上为空，可点击敌方玩家头像直接攻击。";
        return;
    }

    if (state.selectedHandId) {
        const selectedCard = my.hand.find(card => card.instanceId === state.selectedHandId);
        const definition = selectedCard ? getCard(selectedCard.cardId) : null;
        title.textContent = "当前模式：手牌已选择";
        text.textContent = definition
            ? `已选择 ${definition.name}。点击“确认出牌”使用。`
            : "已选择一张手牌。";
        return;
    }

    title.textContent = "当前模式：空闲";
    text.textContent = isMyTurn()
        ? "可选择手牌出牌，或选择场上可攻击角色。"
        : "当前不是你的回合，等待对方操作。";
}

function renderSelectionPanel() {
    const panel = document.getElementById("selectionPanel");
    const title = document.getElementById("selectionTitle");
    const meta = document.getElementById("selectionMeta");
    const confirmBtn = document.getElementById("confirmPlayBtn");
    const my = selfPlayer();

    if (!panel || !title || !meta || !confirmBtn) {
        return;
    }

    if (!my || !state.selectedHandId) {
        panel.classList.remove("visible");
        return;
    }

    const selectedCard = my.hand.find(card => card.instanceId === state.selectedHandId);
    if (!selectedCard) {
        panel.classList.remove("visible");
        return;
    }

    const definition = getCard(selectedCard.cardId);
    panel.classList.add("visible");
    title.textContent = `已选择 ${definition.name}`;
    meta.classList.toggle("targeting", !!state.pendingSkillTarget);
    meta.textContent = state.pendingSkillTarget
        ? "请选择一个角色或玩家作为技能目标。"
        : definition.type === "SKILL"
            ? "这是技能牌。点击“确认出牌”使用。"
            : "这是角色牌。点击“确认出牌”召唤。";
    confirmBtn.textContent = state.pendingSkillTarget
        ? "等待选择目标"
        : definition.type === "SKILL" ? "确认使用技能" : "确认召唤角色";
    confirmBtn.disabled = !!state.pendingSkillTarget;
}

function renderBattleLayout(match) {
    const enemy = opponentPlayer();
    const my = selfPlayer();
    const root = cloneTemplate("battleTemplate");
    setNodeText(root, '[data-battle="roomCode"]', state.roomCode || "------");
    setNodeText(root, '[data-battle="turn"]', match.ready ? match.turn : "等待");
    setNodeText(root, '[data-battle="phase"]', match.phase);
    setNodeHtml(root, '[data-battle="enemyEffects"]', renderCompactEffects(enemy));
    setNodeHtml(root, '[data-battle="selfEffects"]', renderCompactEffects(my));
    replaceNode(root, '[data-battle="enemyRow"]', renderBattlePlayerRow(enemy, false, match));
    replaceNode(root, '[data-battle="selfRow"]', renderBattlePlayerRow(my, true, match));

    const hand = root.querySelector('[data-battle="hand"]');
    if (my && my.hand.length) {
        my.hand.forEach(card => hand.appendChild(renderHandCardNode(card)));
    } else {
        hand.innerHTML = `<div class="board-empty">暂无手牌</div>`;
    }

    const confirm = root.querySelector("#handConfirmBtn");
    confirm.disabled = !canConfirmSelectedHand();
    confirm.classList.toggle("disabled", !canConfirmSelectedHand());
    setNodeText(root, "#battleLogs", (match.logs || []).join("\n"));
    return root.outerHTML;
}

function cloneTemplate(id) {
    const template = document.getElementById(id);
    if (!template) {
        throw new Error(`缺少页面模板：${id}`);
    }
    return template.content.firstElementChild.cloneNode(true);
}

function setNodeText(root, selector, value) {
    const node = root.querySelector(selector);
    if (node) {
        node.textContent = value;
    }
}

function setTextById(id, value) {
    const node = document.getElementById(id);
    if (node) {
        node.textContent = value;
    }
}

function setHtmlById(id, value) {
    const node = document.getElementById(id);
    if (node) {
        node.innerHTML = value;
    }
}

function setNodeHtml(root, selector, value) {
    const node = root.querySelector(selector);
    if (node) {
        node.innerHTML = value;
    }
}

function replaceNode(root, selector, replacement) {
    const node = root.querySelector(selector);
    if (node) {
        node.replaceWith(replacement);
    }
}

function bindClickById(id, handler) {
    const node = document.getElementById(id);
    if (node) {
        node.onclick = handler;
    }
}

function renderCompactEffects(player) {
    const badges = player ? renderEffectBadges(player) : [];
    return badges.length ? badges.join("") : `<span class="effect-badge">无效果</span>`;
}

function renderBattlePlayerRow(player, isSelf, match) {
    const row = cloneTemplate("playerRowTemplate");
    row.classList.add(isSelf ? "self" : "enemy");
    if (!player) {
        setNodeHtml(row, '[data-player="label"]', "等待<br>玩家");
        setNodeText(row, '[data-player="hp"]', "");
        setNodeHtml(row, '[data-player="summons"]', renderEmptySlots(isSelf));
        setNodeHtml(row, '[data-player="pile"]', isSelf ? "抽牌堆<br>0" : "墓地<br>0");
        setNodeHtml(row, '[data-player="effects"]', `<span class="effect-badge">无 buff / debuff</span>`);
        return row;
    }

    const canDirectAttackPlayer = !isSelf && !!state.selectedAttackerId && isMyTurn() && match.phase === "ACTION" && player.board.length === 0;
    const avatar = row.querySelector(".player-avatar");
    if (avatar) {
        avatar.classList.toggle("targetable", !!state.pendingSkillTarget || canDirectAttackPlayer);
        avatar.dataset.playerId = player.playerId;
    }
    setNodeHtml(row, '[data-player="label"]', `${isSelf ? "我方" : "对方"}<br>玩家`);
    setNodeText(row, '[data-player="hp"]', `${player.hp} / 100`);
    setNodeHtml(row, '[data-player="summons"]', renderBoardSlots(player, isSelf));
    setNodeHtml(row, '[data-player="pile"]', isSelf ? `抽牌堆<br>${state.match?.drawPile?.length || 0}` : `墓地<br>${state.match?.discardPile?.length || 0}`);
    setNodeHtml(row, '[data-player="effects"]', renderEffectBadges(player).join("") || `<span class="effect-badge">无 buff / debuff</span>`);
    return row;
}

function renderEmptySlots(isSelf) {
    return [1, 2, 3].map(index => `<div class="summon-slot empty">${isSelf ? "" : "对方"}召唤区${index}</div>`).join("");
}

function renderBoardSlots(player, isSelf) {
    const slots = [];
    for (let i = 0; i < 3; i++) {
        const card = player.board[i];
        if (!card) {
            slots.push(`<div class="summon-slot empty">${isSelf ? `召唤位 ${i + 1}` : `敌方位 ${i + 1}`}</div>`);
            continue;
        }
        slots.push(`<div class="summon-slot">${renderBoardCard(card, player, isSelf)}</div>`);
    }
    return slots.join("");
}

function renderBoardCard(instance, owner, isSelf) {
    return renderBoardCardNode(instance, owner, isSelf).outerHTML;
}

function renderBoardCardNode(instance, owner, isSelf) {
    const card = getCard(instance.cardId);
    const canAttack = isSelf && isMyTurn() && state.match.phase === "ACTION" && !instance.sleeping;
    const targetable = (!isSelf && !!state.selectedAttackerId && isMyTurn() && state.match.phase === "ACTION")
        || !!state.pendingSkillTarget;
    const isNew = !state.seenBoardInstanceIds.includes(instance.instanceId);
    const node = renderCardNode(card, instance, "board");
    node.classList.toggle("entering", isNew);
    node.classList.toggle("selectable", canAttack);
    node.classList.toggle("attack-target", targetable);
    node.classList.toggle("attacker-selected", state.selectedAttackerId === instance.instanceId);
    node.dataset.ownerId = owner.playerId;
    node.dataset.canAttack = String(canAttack);
    return node;
}

function renderHandCard(instance) {
    return renderHandCardNode(instance).outerHTML;
}

function renderHandCardNode(instance) {
    const card = getCard(instance.cardId);
    const isNew = !state.seenHandInstanceIds.includes(instance.instanceId);
    const node = renderCardNode(card, instance, "hand");
    node.classList.add("selectable");
    node.classList.toggle("selected", state.selectedHandId === instance.instanceId);
    node.classList.toggle("entering", isNew);
    return node;
}

function renderCardNode(card, instance, role) {
    const node = cloneTemplate("cardTemplate");
    const image = cardImage(card.id);
    node.dataset.cardRole = role;
    node.dataset.instanceId = instance.instanceId;
    node.dataset.cardId = card.id;
    node.dataset.cardType = card.type;
    setNodeText(node, ".card-kind", card.type === "SKILL" ? "技能牌" : "角色牌");
    setNodeText(node, ".card-name", card.name);
    const figure = node.querySelector(".card-figure");
    figure.classList.toggle("no-image", !image);
    figure.dataset.mark = card.type === "SKILL" ? "技" : "角";
    const img = figure.querySelector("img");
    if (image) {
        img.src = image;
        img.alt = card.name;
        img.onerror = () => img.remove();
    } else {
        img.remove();
    }
    const stats = node.querySelector(".mini-stats");
    if (card.type === "CHARACTER") {
        stats.innerHTML = `<span>攻 ${card.attack || 0}</span><span>体 ${instance.currentHealth || card.health || 0}</span>`;
    } else {
        stats.innerHTML = `<span>${describeSkillRange(card.skillRange)}</span>`;
    }
    return node;
}

function openDetailCard(card, instance = null) {
    const image = cardImage(card.id);
    const isSkill = card.type === "SKILL";
    const isCharacter = card.type === "CHARACTER";
    const currentHealth = instance && typeof instance.currentHealth === "number" ? instance.currentHealth : card.health || 0;
    const detailCard = document.getElementById("detailCard");
    const detailModal = document.getElementById("detailModal");
    if (!detailCard || !detailModal) {
        return;
    }
    detailCard.innerHTML = `
        <button class="detail-close alt" id="detailCloseBtn">关闭</button>
        <div class="card-surface">
            <div class="card-figure ${image ? "" : "no-image"}" data-mark="${isSkill ? "技" : "角"}">
                ${cardImageMarkup(image, card.name)}
            </div>
            <div class="card-overlay"></div>
            <div class="card-body">
                <div class="card-top">
                    <div>
                        <div class="card-kind">${isSkill ? "技能牌" : isCharacter ? "角色牌" : "卡牌数据未加载"}</div>
                        <div class="card-name">${card.name}</div>
                    </div>
                </div>
                <div class="stat-badges">
                    ${isCharacter
                        ? `<span class="stat">攻击 ${card.attack || 0}</span><span class="stat">体力 ${currentHealth}</span>`
                        : isSkill
                            ? `<span class="stat">作用范围 ${describeSkillRange(card.skillRange)}</span>`
                            : `<span class="stat">等待 /api/cards 数据</span>`}
                </div>
                <div class="card-text">${card.description}</div>
                <div class="card-foot">
                    <span>${isSkill ? "技能牌详情" : isCharacter ? "角色牌详情" : "定义未匹配"}</span>
                    <span>${describeEffectType(card.effectType)}</span>
                </div>
            </div>
        </div>
    `;
    detailModal.classList.add("visible");
    bindClickById("detailCloseBtn", closeDetailCard);
}

function closeDetailCard() {
    const detailModal = document.getElementById("detailModal");
    if (detailModal) {
        detailModal.classList.remove("visible");
    }
}

function renderEffectBadges(player) {
    const effects = [];
    (player.statusEffects || []).forEach(effect => {
        const label = describeEffect(effect);
        effects.push(
            `<span class="effect-badge ${effect.category === "debuff" ? "debuff" : "buff"}">${label}</span>`
        );
    });
    return effects;
}

function describeSkillRange(range) {
    switch (range) {
        case "SINGLE":
            return "单体";
        case "ALLY":
            return "我方";
        case "BOTH":
            return "双方";
        default:
            return "未定义";
    }
}

function describeEffectType(effectType) {
    switch (effectType) {
        case "GLOBAL_BUFF":
            return "增益";
        case "SHIELD":
            return "护盾";
        case "HEAL_BOTH":
            return "治疗";
        case "DAMAGE_ALL_ENEMIES":
            return "伤害";
        case "COUNTER_EFFECT":
            return "反制";
        default:
            return "";
    }
}

function describeEffect(effect) {
    const suffix = effect.remainingTurns != null ? ` · ${effect.remainingTurns} 回合` : "";
    const stackSuffix = effect.stacks > 1 ? ` x${effect.stacks}` : "";
    switch (effect.type) {
        case "ATTACK_UP":
            return `Buff：攻击 +${effect.value}${suffix}`;
        case "MAX_HP_UP":
            return `Buff：最大生命 +${effect.value}${suffix}`;
        case "TURN_HEAL":
            return `Buff：回合回血 +${effect.value}${suffix}`;
        case "SHIELD":
            return `Buff：护盾${stackSuffix}${suffix}`;
        case "BLOCK_DAMAGE":
            return `Buff：挡伤害${stackSuffix}${suffix}`;
        case "NEGATE_NEXT_SKILL":
            return `Debuff：对方技能无效${stackSuffix}${suffix}`;
        default:
            return `${effect.category === "debuff" ? "Debuff" : "Buff"}：${effect.type}${suffix}`;
    }
}

function bindInteractions() {
    document.querySelectorAll('[data-card-role="hand"]').forEach(node => {
        node.addEventListener("click", () => {
            state.selectedHandId = node.dataset.instanceId;
            render();
        });
        node.addEventListener("dblclick", () => {
            const card = getCard(node.dataset.cardId);
            const my = selfPlayer();
            const instance = my?.hand.find(item => item.instanceId === node.dataset.instanceId) || null;
            openDetailCard(card, instance);
        });
    });

    document.querySelectorAll('[data-card-role="board"]').forEach(node => {
        node.addEventListener("click", async () => {
            if (!state.match || !state.match.ready || !isMyTurn() || state.match.phase !== "ACTION") {
                return;
            }

            const ownerId = node.dataset.ownerId;
            const instanceId = node.dataset.instanceId;
            const canAttack = node.dataset.canAttack === "true";
            const owner = ownerId === state.selfPlayerId ? selfPlayer() : opponentPlayer();
            const instance = owner?.board.find(item => item.instanceId === instanceId) || null;
            const definition = instance ? getCard(instance.cardId) : null;

            if (state.pendingSkillTarget) {
                await playSkill(state.pendingSkillTarget.instanceId, ownerId, instanceId);
                return;
            }

            if (ownerId === state.selfPlayerId) {
                if (!canAttack) {
                    toast("这个角色本回合不能攻击。", "error");
                    return;
                }
                state.selectedAttackerId = instanceId;
                toast("已选中攻击者，再点敌方角色攻击。", "info");
                render();
                return;
            }

            if (!state.selectedAttackerId) {
                toast("请先选择己方一个可攻击角色。", "error");
                return;
            }
            await attackCharacter(state.selectedAttackerId, instanceId);
        });
        node.addEventListener("dblclick", () => {
            const ownerId = node.dataset.ownerId;
            const owner = ownerId === state.selfPlayerId ? selfPlayer() : opponentPlayer();
            const instance = owner?.board.find(item => item.instanceId === node.dataset.instanceId) || null;
            if (!instance) {
                return;
            }
            openDetailCard(getCard(instance.cardId), instance);
        });
    });

    document.querySelectorAll('[data-player-head="true"]').forEach(node => {
        node.addEventListener("click", async () => {
            if (!state.pendingSkillTarget) {
                const playerId = node.dataset.playerId;
                const targetPlayer = state.match?.players.find(player => player.playerId === playerId);
                if (
                    state.selectedAttackerId &&
                    playerId !== state.selfPlayerId &&
                    targetPlayer &&
                    targetPlayer.board.length === 0
                ) {
                    await attackPlayer();
                }
                return;
            }
            await playSkill(state.pendingSkillTarget.instanceId, node.dataset.playerId, null);
        });
    });
}

function bindTurnActionButtons() {
    const confirmBtn = document.getElementById("handConfirmBtn");
    if (confirmBtn) {
        confirmBtn.onclick = confirmSelectedHand;
    }
    const drawBtn = document.getElementById("handDrawBtn");
    if (drawBtn) {
        drawBtn.onclick = drawPhase;
    }
    const endTurnBtn = document.getElementById("handEndTurnBtn");
    if (endTurnBtn) {
        endTurnBtn.onclick = endTurn;
    }
}

async function summonCard(instanceId) {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/summon`, {
            method: "POST",
            body: JSON.stringify({ playerId: state.selfPlayerId, handInstanceId: instanceId })
        });
        state.selectedHandId = "";
        render();
        toast("角色已召唤。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function playSkill(instanceId, targetPlayerId = null, targetInstanceId = null) {
    try {
        const my = selfPlayer();
        const skillCard = my?.hand.find(card => card.instanceId === instanceId);
        const definition = skillCard ? getCard(skillCard.cardId) : null;
        if (definition?.skillRange === "SINGLE" && !targetPlayerId) {
            state.pendingSkillTarget = { instanceId };
            renderSelectionPanel();
            toast("请选择一个角色或玩家作为技能目标。", "info");
            return;
        }
        state.match = await api(`/api/matches/${state.match.matchId}/play-skill`, {
            method: "POST",
            body: JSON.stringify({
                playerId: state.selfPlayerId,
                handInstanceId: instanceId,
                targetPlayerId,
                targetInstanceId
            })
        });
        state.pendingSkillTarget = null;
        state.selectedHandId = "";
        render();
        toast("技能已使用。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function attackCharacter(attackerInstanceId, defenderInstanceId) {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/attack-character`, {
            method: "POST",
            body: JSON.stringify({
                playerId: state.selfPlayerId,
                attackerInstanceId,
                defenderInstanceId
            })
        });
        state.selectedAttackerId = "";
        render();
        toast("攻击已结算。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function attackPlayer() {
    if (!state.selectedAttackerId) {
        toast("请先点一个己方可攻击角色。", "error");
        return;
    }
    const enemy = opponentPlayer();
    if (enemy && enemy.board.length > 0) {
        toast("对方场上还有角色，不能直接攻击玩家。", "error");
        return;
    }
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/attack-player`, {
            method: "POST",
            body: JSON.stringify({
                playerId: state.selfPlayerId,
                attackerInstanceId: state.selectedAttackerId
            })
        });
        state.selectedAttackerId = "";
        render();
        toast("已对玩家造成伤害。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function confirmSelectedHand() {
    if (!canConfirmSelectedHand()) {
        return;
    }
    const my = selfPlayer();
    const selectedCard = my.hand.find(card => card.instanceId === state.selectedHandId);
    const definition = getCard(selectedCard.cardId);
    if (definition.type === "SKILL") {
        await playSkill(selectedCard.instanceId);
    } else {
        await summonCard(selectedCard.instanceId);
    }
}

async function drawPhase() {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/draw?playerId=${state.selfPlayerId}`, { method: "POST" });
        render();
        toast("宸插畬鎴愭娊鐗岄樁娈点€?", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function endTurn() {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/end-turn?playerId=${state.selfPlayerId}`, { method: "POST" });
        state.selectedAttackerId = "";
        render();
        toast("鍥炲悎宸茬粨鏉熴€?", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function createRoom() {
    try {
        state.playerToken = generateClientToken();
        const match = await api("/api/rooms", {
            method: "POST",
            body: JSON.stringify({
                hostName: playerNameInput?.value || state.playerName || "玩家",
                playerToken: state.playerToken,
                botMode: !!botModeInput?.checked
            })
        });
        state.match = match;
        state.roomCode = match.roomCode;
        state.selfPlayerId = "P1";
        state.playerName = playerNameInput?.value || state.playerName || "玩家";
        if (roomCodeInput) {
            roomCodeInput.value = match.roomCode;
        }
        startPolling();
        connectSocket();
        render();
        toast("房间创建成功。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function joinRoom() {
    try {
        state.playerToken = generateClientToken();
        const roomCode = roomCodeInput.value.trim().toUpperCase();
        // Join errors are passed through directly so the UI can distinguish "not found" and "full".
        const match = await api(`/api/rooms/${roomCode}/join`, {
            method: "POST",
            body: JSON.stringify({ playerName: playerNameInput?.value || state.playerName || "玩家", playerToken: state.playerToken })
        });
        state.match = match;
        state.roomCode = roomCode;
        state.selfPlayerId = "P2";
        state.playerName = playerNameInput?.value || state.playerName || "玩家";
        startPolling();
        connectSocket();
        render();
        toast("已加入房间。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
}

async function refreshRoom() {
    try {
        if (!state.roomCode) {
            const code = roomCodeInput.value.trim().toUpperCase();
            if (!code) {
                return;
            }
            state.roomCode = code;
        }
        state.match = await api(`/api/rooms/${state.roomCode}`);
        render();
    } catch (error) {
        toast(error.message, "error");
    }
}

function startPolling() {
    if (state.pollTimer) {
        clearInterval(state.pollTimer);
    }
    state.pollTimer = setInterval(refreshRoom, 1800);
}

function connectSocket() {
    if (!state.roomCode) {
        return;
    }
    if (state.socket) {
        state.socket.close();
    }
    // WebSocket is used for live table sync; polling stays as a fallback if the socket drops.
    const protocol = state.baseUrl.startsWith("https") ? "wss" : "ws";
    const wsBase = state.baseUrl.replace(/^https?/, protocol);
    state.socket = new WebSocket(`${wsBase}/ws/game?roomCode=${encodeURIComponent(state.roomCode)}`);
    state.socket.onmessage = event => {
        try {
            state.match = JSON.parse(event.data);
            render();
        } catch {
        }
    };
}

async function restoreMatchSession() {
    restoreSession();
    if (!state.roomCode || !state.playerToken) {
        render();
        return;
    }
    try {
        // Refresh recovery now uses playerToken instead of assuming the browser is still P1 or P2.
        const session = await api(`/api/rooms/${state.roomCode}/session/${state.playerToken}`);
        state.match = session.match;
        state.selfPlayerId = session.playerId;
        connectSocket();
        render();
        toast("已恢复上次会话。", "success");
        startPolling();
    } catch {
        sessionStorage.removeItem(STORAGE_KEY);
        state.roomCode = "";
        state.selfPlayerId = "";
        state.playerToken = "";
        state.match = null;
        render();
    }
}

bindClickById("createRoomBtn", createRoom);
bindClickById("joinRoomBtn", joinRoom);
bindClickById("refreshRoomBtn", refreshRoom);
bindClickById("drawBtn", async () => {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/draw?playerId=${state.selfPlayerId}`, { method: "POST" });
        render();
        toast("已完成抽牌阶段。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
});
bindClickById("endTurnBtn", async () => {
    try {
        state.match = await api(`/api/matches/${state.match.matchId}/end-turn?playerId=${state.selfPlayerId}`, { method: "POST" });
        state.selectedAttackerId = "";
        render();
        toast("回合已结束。", "success");
    } catch (error) {
        toast(error.message, "error");
    }
});
bindClickById("attackPlayerBtn", attackPlayer);
bindClickById("confirmPlayBtn", async () => {
    const my = selfPlayer();
    if (!my || !state.selectedHandId) {
        toast("请先选择一张手牌。", "error");
        return;
    }
    const selectedCard = my.hand.find(card => card.instanceId === state.selectedHandId);
    if (!selectedCard) {
        toast("当前选择的手牌已失效。", "error");
        state.selectedHandId = "";
        render();
        return;
    }
    const definition = getCard(selectedCard.cardId);
    if (definition.type === "SKILL") {
        await playSkill(selectedCard.instanceId);
    } else {
        await summonCard(selectedCard.instanceId);
    }
});
bindClickById("clearSelectionBtn", () => {
    state.selectedHandId = "";
    state.selectedAttackerId = "";
    state.pendingSkillTarget = null;
    render();
});
bindClickById("copyRoomBtn", async () => {
    if (!state.roomCode) {
        toast("当前没有房间码。", "error");
        return;
    }
    await navigator.clipboard.writeText(state.roomCode);
    toast("房间码已复制。", "success");
});
bindClickById("detailModal", event => {
    if (event.target.id === "detailModal") {
        closeDetailCard();
    }
});

loadBattleTemplates()
    .then(loadClientConfig)
    .then(loadCards)
    .then(restoreMatchSession)
    .catch(error => toast(error.message, "error"));
