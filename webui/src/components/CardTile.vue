<template>
  <article class="catalog-card panel" @dblclick="$emit('preview', card)">
    <div class="catalog-art" :class="{ 'no-image': imageFailed || !image }">
      <img v-if="image && !imageFailed" :src="image" :alt="card.name" @error="imageFailed = true">
    </div>
    <div class="catalog-info">
      <div class="catalog-head">
        <div>
          <span class="catalog-type">{{ describeType(card.type) }}</span>
          <h2>{{ card.name }}</h2>
        </div>
        <div class="catalog-id">{{ card.id }}</div>
      </div>
      <p>{{ card.description || "暂无描述" }}</p>
      <div class="catalog-stats">
        <template v-if="card.type === 'CHARACTER'">
          <span>攻击 {{ describeAttack(card) }}</span>
          <span>体力 {{ card.secondaryHealth != null ? `${card.health || 0}/${card.secondaryHealth}` : (card.health || 0) }}</span>
        </template>
        <template v-else>
          <span>范围 {{ describeSkillRange(card.skillRange) }}</span>
          <span>效果 {{ describeEffectType(card.effectType) }}</span>
          <span>分类 {{ describeEffectCategory(card.effectCategory) }}</span>
        </template>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from "vue";
import { cardImage, describeAttack, describeEffectCategory, describeEffectType, describeSkillRange, describeType } from "../lib/game";

const props = defineProps({
  card: {
    type: Object,
    required: true
  },
  assetBaseUrl: {
    type: String,
    default: ""
  }
});

defineEmits(["preview"]);

const imageFailed = ref(false);
const image = computed(() => cardImage(props.card, {}, props.assetBaseUrl));
</script>
