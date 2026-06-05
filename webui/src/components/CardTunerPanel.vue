<template>
  <section class="tuner-layout">
    <section class="tuner-stage panel">
      <div class="tuner-toolbar">
        <label>
          预览卡牌
          <select v-model="selectedCardId">
            <option v-for="card in cards" :key="card.id" :value="card.id">
              {{ card.name }} · {{ describeType(card.type) }} · {{ describeRarity(card.rarity) }}
            </option>
          </select>
        </label>
        <label>
          预览宽度
          <input v-model.number="settings.cardWidth" type="number" min="120" max="520" step="10">
        </label>
        <button class="alt" type="button" @click="resetSettings">重置</button>
      </div>

      <div class="tuner-card-wrap">
        <article class="tuner-card" :style="previewStyle">
          <div class="tuner-card-surface">
            <div class="tuner-card-art" :class="{ 'no-image': !previewImage }" :data-mark="selectedCard.type === 'SKILL' ? '技' : '角'">
              <img v-if="previewImage" :src="previewImage" :alt="selectedCard.name" @error="imageFailed = true">
            </div>
            <div class="tuner-card-overlay"></div>
            <img v-if="rarityFrame" class="tuner-rarity-frame" :src="rarityFrame" alt="">
            <span class="tuner-cost-badge">{{ cardActionCost }}</span>
            <div class="tuner-name-band">
              <div class="tuner-card-name">{{ selectedCard.name }}</div>
            </div>
            <div class="tuner-stats">
              <template v-if="selectedCard.type === 'CHARACTER'">
                <span>攻 {{ describeAttack(selectedCard) }}</span>
                <span>体 {{ selectedCard.exclusive?.secondaryHealth != null ? `${selectedCard.health || 0}/${selectedCard.exclusive.secondaryHealth}` : (selectedCard.health || 0) }}</span>
              </template>
              <template v-else>
                <span>{{ describeSkillRange(selectedCard.skillRange) }}</span>
              </template>
            </div>
          </div>
        </article>
      </div>
    </section>

    <aside class="tuner-controls panel">
      <div class="tuner-controls-head">
        <div>
          <p class="eyebrow">Tuner</p>
          <h1>位置参数</h1>
        </div>
        <button type="button" @click="copySettings">复制参数</button>
      </div>

      <div class="tuner-control-grid">
        <TunerControl v-model="settings.frameScale" label="边框缩放" unit="%" :min="100" :max="150" :step="1" />
        <TunerControl v-model="settings.artInset" label="图片内缩" unit="%" :min="-8" :max="12" :step="0.5" />
        <TunerControl v-model="settings.overlayHeight" label="下半区高度" unit="%" :min="20" :max="55" :step="0.5" />

        <TunerControl v-model="settings.costTop" label="费用上边距" unit="px" :min="-40" :max="20" :step="1" />
        <TunerControl v-model="settings.costLeft" label="费用左边距" unit="px" :min="-40" :max="20" :step="1" />
        <TunerControl v-model="settings.costSize" label="费用尺寸" unit="px" :min="28" :max="90" :step="1" />
        <TunerControl v-model="settings.costRotate" label="费用旋转" unit="deg" :min="-24" :max="24" :step="1" />
        <TunerControl v-model="settings.costFont" label="费用字号" unit="px" :min="12" :max="36" :step="1" />

        <TunerControl v-model="settings.nameTop" label="名字上边距" unit="%" :min="-5" :max="30" :step="0.1" />
        <TunerControl v-model="settings.nameLeft" label="名字左边距" unit="%" :min="0" :max="40" :step="0.1" />
        <TunerControl v-model="settings.nameRight" label="名字右边距" unit="%" :min="0" :max="40" :step="0.1" />
        <TunerControl v-model="settings.nameHeight" label="名字高度" unit="%" :min="3" :max="28" :step="0.1" />
        <TunerControl v-model="settings.nameFont" label="名字字号" unit="px" :min="8" :max="42" :step="1" />

        <TunerControl v-model="settings.statsLeft" label="属性左边距" unit="px" :min="-20" :max="80" :step="1" />
        <TunerControl v-model="settings.statsRight" label="属性右边距" unit="px" :min="-20" :max="80" :step="1" />
        <TunerControl v-model="settings.statsBottom" label="属性下边距" unit="px" :min="-20" :max="120" :step="1" />
        <TunerControl v-model="settings.statsFont" label="属性字号" unit="px" :min="8" :max="32" :step="1" />
      </div>

      <pre class="tuner-output">{{ output }}</pre>
    </aside>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from "vue";
import { actionCostOf, api, cardImage, cardTexture, describeAttack, describeRarity, describeSkillRange, describeType } from "../lib/game";
import { assetRoot } from "../lib/runtime-config";
import { showToast } from "../lib/toast";

const STORAGE_KEY = "campusking-card-tuner";

const defaultSettings = {
  cardWidth: 300,
  frameScale: 128,
  artInset: 0,
  overlayHeight: 34,
  costTop: -15,
  costLeft: -15,
  costSize: 52,
  costRotate: -9,
  costFont: 16,
  nameTop: 4.4,
  nameLeft: 14,
  nameRight: 14,
  nameHeight: 10.8,
  nameFont: 16,
  statsLeft: 10,
  statsRight: 10,
  statsBottom: 10,
  statsFont: 11
};

const fallbackCards = [
  {
    id: "dragon",
    name: "预览角色",
    type: "CHARACTER",
    rarity: "RARE",
    actionCost: 1,
    attack: 3,
    health: 8
  },
  {
    id: "foresight",
    name: "预见时间",
    type: "SKILL",
    rarity: "RARE",
    actionCost: 0,
    skillRange: "SELF"
  }
];

const savedSettings = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
const settings = reactive({ ...defaultSettings, ...(savedSettings || {}) });
const cards = ref(fallbackCards);
const selectedCardId = ref(savedSettings?.selectedCardId || "");
const imageFailed = ref(false);

const TunerControl = defineComponent({
  props: {
    modelValue: { type: Number, required: true },
    label: { type: String, required: true },
    unit: { type: String, default: "" },
    min: { type: Number, required: true },
    max: { type: Number, required: true },
    step: { type: Number, default: 1 }
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    const update = event => emit("update:modelValue", Number(event.target.value));
    return () => h("label", { class: "tuner-control" }, [
      h("span", props.label),
      h("input", {
        type: "range",
        min: props.min,
        max: props.max,
        step: props.step,
        value: props.modelValue,
        onInput: update
      }),
      h("input", {
        type: "number",
        min: props.min,
        max: props.max,
        step: props.step,
        value: props.modelValue,
        onInput: update
      }),
      h("small", props.unit)
    ]);
  }
});

const selectedCard = computed(() => cards.value.find(card => card.id === selectedCardId.value) || cards.value[0] || fallbackCards[0]);
const previewImage = computed(() => imageFailed.value ? "" : cardImage(selectedCard.value, {}, assetRoot()));
const rarityFrame = computed(() => {
  return cardTexture(selectedCard.value.type === "SKILL" ? "race-skills" : "race-characters");
});
const cardActionCost = computed(() => actionCostOf(selectedCard.value));

const previewStyle = computed(() => ({
  "--tuner-card-width": `${settings.cardWidth}px`,
  "--tuner-frame-scale": `${settings.frameScale}%`,
  "--tuner-art-inset": `${settings.artInset}%`,
  "--tuner-overlay-height": `${settings.overlayHeight}%`,
  "--tuner-cost-top": `${settings.costTop}px`,
  "--tuner-cost-left": `${settings.costLeft}px`,
  "--tuner-cost-size": `${settings.costSize}px`,
  "--tuner-cost-rotate": `${settings.costRotate}deg`,
  "--tuner-cost-font": `${settings.costFont}px`,
  "--tuner-name-top": `${settings.nameTop}%`,
  "--tuner-name-left": `${settings.nameLeft}%`,
  "--tuner-name-right": `${settings.nameRight}%`,
  "--tuner-name-height": `${settings.nameHeight}%`,
  "--tuner-name-font": `${settings.nameFont}px`,
  "--tuner-stats-left": `${settings.statsLeft}px`,
  "--tuner-stats-right": `${settings.statsRight}px`,
  "--tuner-stats-bottom": `${settings.statsBottom}px`,
  "--tuner-stats-font": `${settings.statsFont}px`
}));

const output = computed(() => JSON.stringify({ ...settings, selectedCardId: selectedCardId.value }, null, 2));

watch([settings, selectedCardId], () => {
  localStorage.setItem(STORAGE_KEY, output.value);
}, { deep: true });

watch(selectedCardId, () => {
  imageFailed.value = false;
});

onMounted(async () => {
  try {
    const loadedCards = await api("/game/cards");
    cards.value = loadedCards;
    if (!selectedCardId.value || !loadedCards.some(card => card.id === selectedCardId.value)) {
      selectedCardId.value = loadedCards.find(card => card.rarity === "RARE")?.id || loadedCards[0]?.id || fallbackCards[0].id;
    }
  } catch {
    selectedCardId.value = selectedCardId.value || fallbackCards[0].id;
  }
});

function resetSettings() {
  Object.assign(settings, defaultSettings);
}

async function copySettings() {
  await navigator.clipboard.writeText(output.value);
  showToast("参数已复制", "success");
}
</script>
