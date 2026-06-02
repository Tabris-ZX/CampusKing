<template>
  <header class="topbar" :class="extraClass">
    <div class="topbar-nav-row">
      <nav class="page-nav">
        <RouterLink class="nav-link" :class="{ active: route.name === 'home' }" to="/">首页</RouterLink>
        <RouterLink class="nav-link" :class="{ active: route.name === 'cards' }" to="/cards">卡牌大全</RouterLink>
        <RouterLink class="nav-link" :class="{ active: route.name === 'battle' }" to="/battle">对局</RouterLink>
        <button class="nav-link nav-button" type="button" @click="noticeOpen = true">公告</button>
        <button class="nav-link nav-button" :class="{ active: docsOpen }" type="button" @click="docsOpen = true">文档</button>
        <button class="nav-link nav-button" :class="{ active: route.name === 'admin' }" type="button" @click="openAdminEntry">管理</button>
        <a class="nav-link" :href="repoUrl" target="_blank" rel="noreferrer">GitHub</a>
      </nav>
      <div class="topbar-tools">
        <span v-if="visibleStatus" class="status topbar-status">{{ visibleStatus }}</span>
        <span v-if="displayUser" class="nav-link nav-user">{{ displayUser }}</span>
      </div>
    </div>
    <div class="brand">
      <img class="brand-title-image" :src="titleImageUrl" alt="校园王">
      <div class="title sr-only">校园王</div>
      <div class="subtitle">{{ subtitle }}</div>
    </div>
  </header>

  <div class="topbar-modal" :class="{ visible: noticeOpen }" :aria-hidden="noticeOpen ? 'false' : 'true'" @click.self="noticeOpen = false">
    <section v-if="noticeOpen" class="topbar-dialog panel topbar-announcement-dialog">
      <div class="topbar-dialog-head">
        <div>
          <p class="eyebrow">Notice</p>
          <h2>更新公告</h2>
        </div>
        <button class="alt" type="button" @click="noticeOpen = false">关闭</button>
      </div>
      <div class="topbar-notice-body">
        <div v-if="noticeLoading" class="board-empty">正在加载公告...</div>
        <article v-else-if="noticeError" class="markdown-body" v-html="noticeErrorHtml"></article>
        <article v-else-if="!notices.length" class="markdown-body" v-html="emptyNoticeHtml"></article>
        <div v-else class="topbar-docs-layout">
          <aside class="topbar-doc-tabs" aria-label="公告列表">
            <button
              v-for="notice in notices"
              :key="notice.name"
              class="topbar-doc-tab"
              :class="{ active: selectedNoticeName === notice.name }"
              type="button"
              @click="selectedNoticeName = notice.name"
            >
              <span>{{ notice.title }}</span>
              <small>{{ notice.displayTime }}</small>
            </button>
          </aside>
          <article class="topbar-dialog-body markdown-body topbar-doc-content" v-html="selectedNoticeHtml"></article>
        </div>
      </div>
    </section>
  </div>

  <div class="topbar-modal" :class="{ visible: docsOpen }" :aria-hidden="docsOpen ? 'false' : 'true'" @click.self="docsOpen = false">
    <section v-if="docsOpen" class="topbar-dialog panel topbar-docs-dialog">
      <div class="topbar-dialog-head">
        <div>
          <p class="eyebrow">Documents</p>
          <h2>项目文档</h2>
        </div>
        <button class="alt" type="button" @click="docsOpen = false">关闭</button>
      </div>
      <div class="topbar-docs-layout">
        <aside class="topbar-doc-tabs" aria-label="文档分类">
          <button
            v-for="doc in docs"
            :key="doc.id"
            class="topbar-doc-tab"
            :class="{ active: selectedDocId === doc.id }"
            type="button"
            @click="selectedDocId = doc.id"
          >
            <span>{{ doc.label }}</span>
            <small>{{ doc.hint }}</small>
          </button>
        </aside>
        <article class="topbar-dialog-body markdown-body topbar-doc-content" v-html="selectedDocHtml"></article>
      </div>
    </section>
  </div>

  <div class="topbar-modal" :class="{ visible: adminAuthOpen }" :aria-hidden="adminAuthOpen ? 'false' : 'true'" @click.self="closeAdminAuth">
    <section v-if="adminAuthOpen" class="topbar-dialog panel">
      <div class="topbar-dialog-head">
        <div>
          <p class="eyebrow">Admin</p>
          <h2>管理员验证</h2>
        </div>
        <button class="alt" type="button" @click="closeAdminAuth">关闭</button>
      </div>
      <form class="topbar-password-form" @submit.prevent="submitAdminPassword">
        <label for="adminPasswordInput">管理员密码</label>
        <input id="adminPasswordInput" v-model="adminPassword" type="password" autocomplete="current-password" placeholder="输入管理员密码">
        <p class="topbar-helper">当前先使用本地前端密码校验，后续再接后端或数据库鉴权。</p>
        <div class="button-row">
          <button type="submit">进入管理台</button>
          <button class="alt" type="button" @click="closeAdminAuth">取消</button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import designMarkdown from "../../../docs/design.md?raw";
import howToAddMarkdown from "../../../docs/how-to-add.md?raw";
import ruleMarkdown from "../../../docs/rule.md?raw";
import { isAdminAuthenticated, markAdminAuthenticated, renderMarkdown, verifyAdminPassword } from "../lib/admin";
import { api } from "../lib/game";
import { assetUrl, githubUrl } from "../lib/runtime-config";
import { ensureSessionPlayerName, loadSession } from "../lib/session";
import { showToast } from "../lib/toast";

const props = defineProps({
  subtitle: {
    type: String,
    required: true
  },
  status: {
    type: String,
    default: ""
  },
  extraClass: {
    type: String,
    default: ""
  },
  username: {
    type: String,
    default: ""
  },
});

const route = useRoute();
const router = useRouter();
const titleImageUrl = computed(() => assetUrl("images/ui/title.png"));
const repoUrl = computed(() => githubUrl());
const noticeOpen = ref(false);
const docsOpen = ref(false);
const adminAuthOpen = ref(false);
const adminPassword = ref("");
const selectedDocId = ref("rule");
const notices = ref([]);
const noticeLoading = ref(false);
const noticeError = ref("");
const selectedNoticeName = ref("");
const emptyNoticeHtml = renderMarkdown("# 暂无公告\n\n请在 `resources/notices` 下添加 `yymmdd-hhmm.md` 公告文件。");
const noticeErrorHtml = computed(() => renderMarkdown(`# 公告加载失败\n\n${noticeError.value || "请稍后重试。"}`));
const docs = [
  {
    id: "rule",
    label: "规则",
    hint: "玩法与流程",
    markdown: ruleMarkdown
  },
  {
    id: "design",
    label: "设计",
    hint: "牌堆与机制",
    markdown: designMarkdown
  },
  {
    id: "how-to-add",
    label: "自制卡牌指南",
    hint: "新增卡牌步骤",
    markdown: howToAddMarkdown
  }
];
const selectedDoc = computed(() => docs.find(doc => doc.id === selectedDocId.value) || docs[0]);
const selectedDocHtml = computed(() => renderMarkdown(selectedDoc.value.markdown));
const selectedNotice = computed(() => notices.value.find(notice => notice.name === selectedNoticeName.value) || notices.value[0]);
const selectedNoticeHtml = computed(() => selectedNotice.value?.html || "");
const sessionPlayerName = computed(() => {
  const session = loadSession();
  return session?.playerName || ensureSessionPlayerName(session);
});
const displayUser = computed(() => props.username || sessionPlayerName.value || props.status || "");
const visibleStatus = computed(() => {
  const status = (props.status || "").trim();
  return status && status !== displayUser.value ? status : "";
});

watch(adminAuthOpen, open => {
  if (!open) {
    adminPassword.value = "";
  }
});

watch(noticeOpen, open => {
  if (open) {
    loadNotices();
  }
});

function onKeydown(event) {
  if (event.key !== "Escape") {
    return;
  }
  if (noticeOpen.value) {
    noticeOpen.value = false;
  }
  if (docsOpen.value) {
    docsOpen.value = false;
  }
  if (adminAuthOpen.value) {
    closeAdminAuth();
  }
}

function openAdminEntry() {
  if (isAdminAuthenticated()) {
    router.push("/admin");
    return;
  }
  adminAuthOpen.value = true;
}

function closeAdminAuth() {
  adminAuthOpen.value = false;
}

function submitAdminPassword() {
  if (!verifyAdminPassword(adminPassword.value)) {
    showToast("管理员密码错误", "error");
    return;
  }
  markAdminAuthenticated();
  adminAuthOpen.value = false;
  showToast("管理员验证通过", "success");
  router.push("/admin");
}

async function loadNotices() {
  noticeLoading.value = true;
  noticeError.value = "";
  try {
    const data = await api("/api/notices");
    notices.value = data.map(notice => ({
      ...notice,
      title: noticeTitle(notice.markdown, notice.name),
      html: renderMarkdown(notice.markdown || "")
    }));
    if (!notices.value.some(notice => notice.name === selectedNoticeName.value)) {
      selectedNoticeName.value = notices.value[0]?.name || "";
    }
  } catch (error) {
    noticeError.value = error.message;
  } finally {
    noticeLoading.value = false;
  }
}

function noticeTitle(markdown, fallback) {
  const firstHeading = (markdown || "").split(/\r?\n/).find(line => line.trim().startsWith("# "));
  const title = firstHeading ? firstHeading.trim().replace(/^#\s+/, "").trim() : "";
  return title || fallback || "未命名公告";
}

onMounted(() => {
  document.addEventListener("keydown", onKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
});
</script>
