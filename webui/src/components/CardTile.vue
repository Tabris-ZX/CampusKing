<template>
  <article class="catalog-card panel" :class="cardTypeClass" @dblclick="$emit('preview', card)">
    <div class="catalog-art" :class="{ 'no-image': imageFailed || !image }">
      <img v-if="image && !imageFailed" :src="image" :alt="card.name" @error="handleImageError">
      <span class="card-cost-badge">{{ cardActionCost }}</span>
      <img v-if="rarityFrame" class="card-rarity-frame" :src="rarityFrame" alt="">
      <div class="catalog-card-caption">
        <h2>{{ card.name }}</h2>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from "vue";
import { actionCostOf, cardImage, swapCardImageToFallback } from "../lib/game";

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
const cardActionCost = computed(() => actionCostOf(props.card));
const rarityFrame = computed(() => {
  return "";
});
const cardTypeClass = computed(() => ({
  "catalog-card-character": props.card.type === "CHARACTER",
  "catalog-card-skill": props.card.type === "SKILL"
}));

function handleImageError(event) {
  if (!swapCardImageToFallback(event, props.card, {}, props.assetBaseUrl)) {
    imageFailed.value = true;
  }
}
</script>
