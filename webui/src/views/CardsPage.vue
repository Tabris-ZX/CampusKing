<template>
  <main class="app-shell cards-shell">
    <AppTopbar subtitle="所有卡牌信息陈列" />

    <section class="cards-hero panel">
      <div>
        <p class="eyebrow">Collection</p>
        <h1>卡牌大全</h1>
        <p class="hero-copy">显示当前可用的角色牌和技能牌，双击卡牌可查看高清大图</p>
      </div>
      <div class="cards-filter">
        <button
          v-for="item in filters"
          :key="item.value"
          class="alt"
          :class="{ active: filter === item.value }"
          type="button"
          @click="filter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="cards-summary">
      <div class="summary-chip panel">
        <span class="summary-label">总卡牌</span>
        <strong>{{ filteredCards.length }}</strong>
      </div>
      <div class="summary-chip panel">
        <span class="summary-label">当前筛选</span>
        <strong>{{ filterLabel }}</strong>
      </div>
      <div class="summary-chip panel">
        <span class="summary-label">浏览提示</span>
        <strong>双击卡牌查看高清图</strong>
      </div>
    </section>

    <section class="cards-density-note">
      <span>缩略图已压缩展示，双击即可查看完整信息与高清图。</span>
    </section>

    <section class="card-gallery">
      <div v-if="loading" class="board-empty">正在加载卡牌...</div>
      <div v-else-if="!filteredCards.length" class="board-empty">没有匹配的卡牌。</div>
      <CardTile
        v-for="card in filteredCards"
        :key="card.id"
        :card="card"
        :asset-base-url="assetBaseUrl"
        @preview="previewCard = $event"
      />
    </section>
  </main>

  <div
    class="card-preview-modal"
    :class="{ visible: !!previewCard }"
    :aria-hidden="previewCard ? 'false' : 'true'"
    @click.self="previewCard = null"
  >
    <div v-if="previewCard" class="card-preview-dialog panel">
      <button class="card-preview-close alt" type="button" @click="previewCard = null">关闭</button>
      <div class="card-preview-media" :class="{ 'no-image': previewImageFailed || !previewImage }">
        <img v-if="previewImage && !previewImageFailed" :src="previewImage" :alt="previewCard.name" @error="previewImageFailed = true">
      </div>
      <div class="card-preview-info">
        <div class="card-preview-head">
          <span class="catalog-type">{{ describeType(previewCard.type) }}</span>
          <span class="catalog-id">{{ previewCard.id }}</span>
        </div>
        <div class="card-preview-title">
          <h2>{{ previewCard.name || "未命名卡牌" }}</h2>
          <div class="catalog-stats card-preview-stats">
            <template v-if="previewCard.type === 'CHARACTER'">
              <span>攻击 {{ describeAttack(previewCard) }}</span>
              <span>体力 {{ previewCard.secondaryHealth != null ? `${previewCard.health || 0}/${previewCard.secondaryHealth}` : (previewCard.health || 0) }}</span>
            </template>
            <template v-else>
              <span>范围 {{ describeSkillRange(previewCard.skillRange) }}</span>
              <span>效果 {{ describeEffectType(previewCard.effectType) }}</span>
              <span>分类 {{ describeEffectCategory(previewCard.effectCategory) }}</span>
            </template>
          </div>
        </div>
        <div class="card-preview-body">
          <p>{{ previewCard.description || "暂无描述" }}</p>
          <div class="card-preview-meta">
            <div class="preview-meta-block">
              <span class="summary-label">卡牌类型</span>
              <strong>{{ describeType(previewCard.type) }}</strong>
            </div>
            <div class="preview-meta-block">
              <span class="summary-label">效果标签</span>
              <strong>{{ describeEffectType(previewCard.effectType) }}</strong>
            </div>
            <div v-if="previewCard.type === 'SKILL'" class="preview-meta-block">
              <span class="summary-label">效果分类</span>
              <strong>{{ describeEffectCategory(previewCard.effectCategory) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import AppTopbar from "../components/AppTopbar.vue";
import CardTile from "../components/CardTile.vue";
import { FILTER_TEXT } from "../lib/constants";
import { api, cardImage, describeAttack, describeEffectCategory, describeEffectType, describeSkillRange, describeType } from "../lib/game";
import { showToast } from "../lib/toast";

const cards = ref([]);
const filter = ref("ALL");
const loading = ref(true);
const assetBaseUrl = ref("");
const previewCard = ref(null);
const previewImageFailed = ref(false);

const filters = [
  { value: "ALL", label: "全部" },
  { value: "CHARACTER", label: "角色牌" },
  { value: "SKILL", label: "技能牌" }
];

const filteredCards = computed(() => {
  return filter.value === "ALL" ? cards.value : cards.value.filter(card => card.type === filter.value);
});

const filterLabel = computed(() => FILTER_TEXT[filter.value] || filter.value);
const previewImage = computed(() => {
  if (!previewCard.value) {
    return "";
  }
  return cardImage(previewCard.value, {}, assetBaseUrl.value);
});

watch(previewCard, () => {
  previewImageFailed.value = false;
});

function onKeydown(event) {
  if (event.key === "Escape" && previewCard.value) {
    previewCard.value = null;
  }
}

onMounted(async () => {
  document.addEventListener("keydown", onKeydown);
  try {
    const config = await api("/api/config");
    assetBaseUrl.value = (config.assetBaseUrl || "").trim();
    cards.value = await api("/api/cards");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
});
</script>
