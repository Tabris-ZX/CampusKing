<template>
  <main class="app-shell admin-shell">
    <AppTopbar subtitle="管理员面板" />

    <section class="admin-hero panel">
      <div>
        <p class="eyebrow">Control Room</p>
        <h1>管理后台</h1>
        <p class="hero-copy">当前为前端占位版，已加上本地密码校验，用于公告编写、卡牌数值微调和用户管理占位展示。</p>
      </div>
      <div class="admin-hero-actions">
        <button type="button" @click="saveAnnouncement">保存公告</button>
        <button class="alt" type="button" @click="saveTuning">保存卡牌调整</button>
        <button class="alt" type="button" @click="logoutAdmin">退出管理</button>
      </div>
    </section>

    <section class="admin-grid">
      <section class="panel admin-section">
        <div class="admin-section-head">
          <div>
            <p class="eyebrow">Announcement</p>
            <h2>公告编写</h2>
          </div>
          <span class="admin-badge">Markdown</span>
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

    <section class="admin-grid">
      <section class="panel admin-section">
        <div class="admin-section-head">
          <div>
            <p class="eyebrow">Players</p>
            <h2>用户管理</h2>
          </div>
          <span class="admin-badge">数据库未连接</span>
        </div>
        <div class="admin-user-placeholder">
          <article v-for="user in placeholderUsers" :key="user.id" class="admin-user-card">
            <div>
              <strong>{{ user.name }}</strong>
              <p>{{ user.email }}</p>
            </div>
            <span :class="['admin-user-state', user.status]">{{ user.statusText }}</span>
          </article>
        </div>
      </section>

      <section class="panel admin-section">
        <div class="admin-section-head">
          <div>
            <p class="eyebrow">Balance</p>
            <h2>卡牌数值微调</h2>
          </div>
          <span class="admin-badge">本地占位</span>
        </div>
        <div class="admin-card-table">
          <article v-for="card in cards" :key="card.id" class="admin-card-row">
            <div class="admin-card-meta">
              <strong>{{ card.name }}</strong>
              <span>{{ card.id }}</span>
            </div>
            <div class="admin-card-fields">
              <label>
                攻击
                <input v-model.number="tuning[card.id].attack" type="number">
              </label>
              <label>
                体力
                <input v-model.number="tuning[card.id].health" type="number">
              </label>
              <label v-if="card.secondaryAttack != null">
                二段攻击
                <input v-model.number="tuning[card.id].secondaryAttack" type="number">
              </label>
              <label v-if="card.secondaryHealth != null">
                二段体力
                <input v-model.number="tuning[card.id].secondaryHealth" type="number">
              </label>
            </div>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppTopbar from "../components/AppTopbar.vue";
import { clearAdminAuthentication, loadAnnouncementMarkdown, loadCardTuning, renderMarkdown, saveAnnouncementMarkdown, saveCardTuning } from "../lib/admin";
import { api } from "../lib/game";
import { showToast } from "../lib/toast";

const router = useRouter();
const cards = ref([]);
const announcementMarkdown = ref(loadAnnouncementMarkdown());
const tuning = ref({});

const announcementHtml = computed(() => renderMarkdown(announcementMarkdown.value));

const placeholderUsers = [
  { id: "u1", name: "用户-1024", email: "placeholder1@campus.king", status: "online", statusText: "在线" },
  { id: "u2", name: "用户-2048", email: "placeholder2@campus.king", status: "idle", statusText: "空闲" },
  { id: "u3", name: "管理员-0001", email: "admin@campus.king", status: "admin", statusText: "管理员" }
];

function buildTuning(cardsList) {
  const stored = loadCardTuning();
  const next = {};
  for (const card of cardsList) {
    next[card.id] = {
      attack: stored[card.id]?.attack ?? card.attack ?? 0,
      health: stored[card.id]?.health ?? card.health ?? 0,
      secondaryAttack: stored[card.id]?.secondaryAttack ?? card.secondaryAttack ?? null,
      secondaryHealth: stored[card.id]?.secondaryHealth ?? card.secondaryHealth ?? null
    };
  }
  tuning.value = next;
}

function saveAnnouncement() {
  saveAnnouncementMarkdown(announcementMarkdown.value);
  showToast("公告已保存", "success");
}

function saveTuning() {
  saveCardTuning(tuning.value);
  showToast("卡牌调整已保存到本地占位存储", "success");
}

function logoutAdmin() {
  clearAdminAuthentication();
  showToast("已退出管理员模式", "success");
  router.push("/");
}

onMounted(async () => {
  try {
    cards.value = await api("/api/cards");
    buildTuning(cards.value);
  } catch (error) {
    showToast(error.message, "error");
  }
});
</script>
