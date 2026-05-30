<template>
  <header class="topbar" :class="extraClass">
    <div class="brand">
      <img class="brand-title-image" :src="`${assetPrefix}/images/ui/title.png`" alt="校园王">
      <div class="title sr-only">校园王</div>
      <div class="subtitle">{{ subtitle }}</div>
    </div>
    <div class="topbar-right">
      <nav class="page-nav">
        <RouterLink class="nav-link" :class="{ active: route.name === 'home' }" to="/">首页</RouterLink>
        <RouterLink class="nav-link" :class="{ active: route.name === 'cards' }" to="/cards">卡牌大全</RouterLink>
        <RouterLink class="nav-link" :class="{ active: route.name === 'battle' }" to="/battle">对局</RouterLink>
        <span v-if="displayUser" class="nav-link nav-user">{{ displayUser }}</span>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { loadSession } from "../lib/session";

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
const assetPrefix = computed(() => window.location.origin);
const sessionPlayerName = computed(() => loadSession()?.playerName || "");
const displayUser = computed(() => props.username || sessionPlayerName.value || props.status || "");
</script>
