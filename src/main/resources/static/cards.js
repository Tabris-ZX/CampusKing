const state = {
    baseUrl: window.location.origin,
    assetBaseUrl: "",
    cards: [],
    filter: "ALL"
};

function toast(message, type = "info") {
    window.Toast.show(message, type);
}

async function api(path) {
    const response = await fetch(`${state.baseUrl}${path}`);
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "请求失败");
    }
    return data;
}

async function loadClientConfig() {
    try {
        const config = await api("/api/config");
        state.assetBaseUrl = (config.assetBaseUrl || "").trim();
    } catch {
        state.assetBaseUrl = "";
    }
}

function cardImage(card) {
    const folder = card.type === "SKILL" ? "skills" : "characters";
    const base = (state.assetBaseUrl || state.baseUrl || window.location.origin || "").replace(/\/$/, "");
    return `${base}/images/texture/${folder}/${card.id}.png`;
}

function describeType(type) {
    if (type === "CHARACTER") {
        return "角色牌";
    }
    if (type === "SKILL") {
        return "技能牌";
    }
    return type || "未知";
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
            return effectType || "无";
    }
}

function renderCard(card) {
    const isCharacter = card.type === "CHARACTER";
    const image = cardImage(card);
    return `
        <article class="catalog-card panel">
            <div class="catalog-art">
                <img src="${image}" alt="${card.name}" onerror="this.parentElement.classList.add('no-image'); this.remove();">
            </div>
            <div class="catalog-info">
                <div>
                    <span class="catalog-type">${describeType(card.type)}</span>
                    <h2>${card.name}</h2>
                    <p>${card.description || "暂无描述"}</p>
                </div>
                <div class="catalog-stats">
                    ${isCharacter
                        ? `<span>攻击 ${card.attack || 0}</span><span>体力 ${card.health || 0}</span>`
                        : `<span>范围 ${describeSkillRange(card.skillRange)}</span><span>效果 ${describeEffectType(card.effectType)}</span>`}
                </div>
                <div class="catalog-id">${card.id}</div>
            </div>
        </article>
    `;
}

function render() {
    const gallery = document.getElementById("cardGallery");
    const cards = state.filter === "ALL"
        ? state.cards
        : state.cards.filter(card => card.type === state.filter);
    gallery.innerHTML = cards.length
        ? cards.map(renderCard).join("")
        : `<div class="board-empty">没有匹配的卡牌。</div>`;
}

function bindFilters() {
    document.querySelectorAll("[data-filter]").forEach(button => {
        button.addEventListener("click", () => {
            state.filter = button.dataset.filter;
            document.querySelectorAll("[data-filter]").forEach(item => item.classList.toggle("active", item === button));
            render();
        });
    });
}

async function init() {
    try {
        bindFilters();
        await loadClientConfig();
        state.cards = await api("/api/cards");
        render();
    } catch (error) {
        toast(error.message, "error");
    }
}

init();
