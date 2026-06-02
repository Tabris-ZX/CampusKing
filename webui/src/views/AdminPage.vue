<template>
  <main class="app-shell admin-shell">
    <AppTopbar subtitle="管理员面板" />

    <section class="admin-hero panel">
      <div>
        <p class="eyebrow">Control Room</p>
        <h1>管理后台</h1>
        <p class="hero-copy">公告读取 resources/notices，卡牌参数保存到 cardRegistry.json。</p>
      </div>
      <div class="admin-hero-actions">
        <button
          v-for="section in adminSections"
          :key="section.id"
          type="button"
          :class="{ active: activeSection === section.id }"
          @click="activeSection = section.id"
        >
          {{ section.label }}
        </button>
        <button class="alt" type="button" @click="logoutAdmin">退出管理</button>
      </div>
    </section>

    <section v-if="activeSection === 'notice'" class="admin-grid">
      <section class="panel admin-section">
        <div class="admin-section-head">
          <div>
            <p class="eyebrow">Announcement</p>
            <h2>新增公告</h2>
          </div>
          <button type="button" :disabled="savingNotice" @click="saveAnnouncement">
            {{ savingNotice ? "保存中..." : "保存公告" }}
          </button>
        </div>
        <textarea v-model="announcementMarkdown" class="admin-textarea" spellcheck="false"></textarea>
      </section>

      <section class="panel admin-section">
        <div class="admin-section-head">
          <div>
            <p class="eyebrow">Preview</p>
            <h2>公告预览</h2>
          </div>
        </div>
        <div class="markdown-body admin-preview" v-html="announcementHtml"></div>
      </section>
    </section>

    <section v-else-if="activeSection === 'cards'" class="panel admin-section">
      <div class="admin-section-head">
        <div>
          <p class="eyebrow">Cards</p>
          <h2>卡牌参数</h2>
        </div>
        <span class="admin-badge">{{ cards.length }} 张</span>
      </div>

      <div v-if="loadingCards" class="admin-empty">正在加载卡牌...</div>
      <div v-else-if="!cards.length" class="admin-empty">暂无卡牌配置</div>
      <div v-else class="admin-card-editor">
        <aside class="admin-card-list">
          <button
            v-for="card in cards"
            :key="card.id"
            class="admin-card-tab"
            :class="{ active: selectedCardId === card.id }"
            type="button"
            @click="selectedCardId = card.id"
          >
            <strong>{{ card.name }}</strong>
            <span>{{ card.id }} · {{ describeType(card.type) }} · {{ describeRarity(card.rarity) }}</span>
          </button>
        </aside>

        <form v-if="selectedCard" class="admin-card-form" @submit.prevent="saveCards">
          <div class="admin-card-form-head">
            <div>
              <p class="eyebrow">{{ selectedCard.id }}</p>
              <h3>{{ selectedCard.name }}</h3>
            </div>
            <span class="admin-badge">{{ describeType(selectedCard.type) }}</span>
          </div>

          <div class="admin-card-fields">
            <label>
              名称
              <input v-model.trim="selectedCard.name">
            </label>
            <label>
              费用
              <input v-model.number="selectedCard.actionCost" type="number" min="0">
            </label>
            <label>
              稀有度
              <select v-model="selectedCard.rarity">
                <option value="COMMON">普通</option>
                <option value="RARE">稀有</option>
              </select>
            </label>
            <label>
              类型
              <input :value="describeType(selectedCard.type)" disabled>
            </label>
          </div>

          <label class="admin-wide-field">
            描述
            <textarea v-model="selectedCard.description" class="admin-small-textarea" spellcheck="false"></textarea>
          </label>

          <div v-if="selectedCard.type === 'CHARACTER'" class="admin-card-fields">
            <label>
              攻击
              <input v-model.number="selectedCard.attack" type="number" min="0">
            </label>
            <label>
              体力
              <input v-model.number="selectedCard.health" type="number" min="0">
            </label>
            <label>
              第二形态攻击
              <input v-model.number="selectedCard.exclusive.secondaryAttack" type="number" min="0" placeholder="可空">
            </label>
            <label>
              第二形态体力
              <input v-model.number="selectedCard.exclusive.secondaryHealth" type="number" min="0" placeholder="可空">
            </label>
          </div>

          <div v-else class="admin-card-fields">
            <label>
              效果类型
              <select v-model="selectedCard.effectType">
                <option value="NONE">无</option>
                <option value="DAMAGE_ALL_ENEMIES">伤害</option>
                <option value="GLOBAL_BUFF">增益</option>
                <option value="PREVENT_ACTION">抵御</option>
                <option value="DRAW_AND_MODIFY_UNIT">抽牌与单位改动</option>
                <option value="REVIVE_ALLY">复活</option>
                <option value="DISCARD_AND_DRAW">弃牌换牌</option>
                <option value="DISCARD_ENEMY_HAND">弃置手牌</option>
              </select>
            </label>
            <label>
              效果分类
              <select v-model="selectedCard.effectCategory">
                <option value="INSTANT">即时效果</option>
                <option value="DURATION">持续效果</option>
              </select>
            </label>
            <label>
              效果数值
              <input v-model.number="selectedCard.effectValue" type="number" min="0">
            </label>
            <label>
              持续回合
              <input v-model.number="selectedCard.effectDuration" type="number" min="0">
            </label>
            <label>
              作用范围
              <select v-model="selectedCard.skillRange">
                <option value="SINGLE">单体</option>
                <option value="SELF">自身</option>
                <option value="ENEMY">敌方</option>
                <option value="BOTH">双方</option>
              </select>
            </label>
          </div>

          <div class="admin-card-form-actions">
            <button type="submit" :disabled="savingCards || !cards.length">
              {{ savingCards ? "保存中..." : "保存卡牌参数" }}
            </button>
          </div>
        </form>
      </div>
    </section>

    <section v-else class="admin-tuner-section">
      <div class="admin-section-head">
        <div>
          <p class="eyebrow">Card Preview</p>
          <h2>卡面预览</h2>
        </div>
      </div>
      <CardTunerPanel />
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppTopbar from "../components/AppTopbar.vue";
import CardTunerPanel from "../components/CardTunerPanel.vue";
import { clearAdminAuthentication, renderMarkdown } from "../lib/admin";
import { api, describeRarity, describeType } from "../lib/game";
import { showToast } from "../lib/toast";

const router = useRouter();
const announcementMarkdown = ref("# 公告标题\n\n- 这里填写公告内容");
const savingNotice = ref(false);
const savingCards = ref(false);
const loadingCards = ref(false);
const cards = ref([]);
const selectedCardId = ref("");
const activeSection = ref("notice");
const adminSections = [
  { id: "notice", label: "公告" },
  { id: "cards", label: "卡牌参数" },
  { id: "preview", label: "卡面预览" }
];
const announcementHtml = computed(() => renderMarkdown(announcementMarkdown.value));
const selectedCard = computed(() => cards.value.find(card => card.id === selectedCardId.value));

onMounted(loadCards);

async function loadCards() {
  loadingCards.value = true;
  try {
    cards.value = (await api("/api/cards")).map(normalizeCard);
    selectedCardId.value = cards.value[0]?.id || "";
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    loadingCards.value = false;
  }
}

async function saveAnnouncement() {
  if (!announcementMarkdown.value.trim()) {
    showToast("公告内容不能为空", "error");
    return;
  }
  savingNotice.value = true;
  try {
    const notice = await api("/api/notices", {
      method: "POST",
      body: JSON.stringify({ markdown: announcementMarkdown.value })
    });
    announcementMarkdown.value = "# 公告标题\n\n- 这里填写公告内容";
    showToast(`公告已保存：${notice.name}`, "success");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    savingNotice.value = false;
  }
}

async function saveCards() {
  savingCards.value = true;
  try {
    cards.value = (await api("/api/cards", {
      method: "POST",
      body: JSON.stringify({ cards: cards.value.map(normalizeCard) })
    })).map(normalizeCard);
    if (!cards.value.some(card => card.id === selectedCardId.value)) {
      selectedCardId.value = cards.value[0]?.id || "";
    }
    showToast("卡牌参数已保存", "success");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    savingCards.value = false;
  }
}

function normalizeCard(card) {
  const normalized = {
    ...card,
    exclusive: { ...(card.exclusive || {}) },
    actionCost: numberOrZero(card.actionCost),
    attack: card.type === "CHARACTER" ? numberOrZero(card.attack) : undefined,
    health: card.type === "CHARACTER" ? numberOrZero(card.health) : undefined,
    effectValue: card.type === "SKILL" ? numberOrZero(card.effectValue) : undefined,
    effectDuration: card.type === "SKILL" ? numberOrZero(card.effectDuration) : undefined
  };
  if (card.type === "CHARACTER") {
    normalized.exclusive.secondaryAttack = numberOrNull(normalized.exclusive.secondaryAttack);
    normalized.exclusive.secondaryHealth = numberOrNull(normalized.exclusive.secondaryHealth);
    if (normalized.exclusive.secondaryAttack == null) {
      delete normalized.exclusive.secondaryAttack;
    }
    if (normalized.exclusive.secondaryHealth == null) {
      delete normalized.exclusive.secondaryHealth;
    }
  }
  return normalized;
}

function numberOrZero(value) {
  const number = Number(value);
  return Number.isFinite(number) ? Math.max(0, number) : 0;
}

function numberOrNull(value) {
  if (value === "" || value == null) {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? Math.max(0, number) : null;
}

function logoutAdmin() {
  clearAdminAuthentication();
  showToast("已退出管理员模式", "success");
  router.push("/");
}
</script>
