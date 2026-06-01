<template>
  <main class="app-shell admin-shell">
    <AppTopbar subtitle="管理员面板" />

    <section class="admin-hero panel">
      <div>
        <p class="eyebrow">Control Room</p>
        <h1>管理后台</h1>
        <p class="hero-copy">当前用于公告编写和管理员入口占位，密码校验仍是前端本地方案，后续如需正式鉴权建议切到后端。</p>
      </div>
      <div class="admin-hero-actions">
        <button type="button" @click="saveAnnouncement">保存公告</button>
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
  </main>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import AppTopbar from "../components/AppTopbar.vue";
import { clearAdminAuthentication, loadAnnouncementMarkdown, renderMarkdown, saveAnnouncementMarkdown } from "../lib/admin";
import { showToast } from "../lib/toast";

const router = useRouter();
const announcementMarkdown = ref(loadAnnouncementMarkdown());

const announcementHtml = computed(() => renderMarkdown(announcementMarkdown.value));

function saveAnnouncement() {
  saveAnnouncementMarkdown(announcementMarkdown.value);
  showToast("公告已保存", "success");
}

function logoutAdmin() {
  clearAdminAuthentication();
  showToast("已退出管理员模式", "success");
  router.push("/");
}
</script>
