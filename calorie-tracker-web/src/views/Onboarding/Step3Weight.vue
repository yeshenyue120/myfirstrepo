<template>
  <div class="step-content">
    <h2 class="step-title">你的体重</h2>

    <!-- 数值 + 微调按钮 -->
    <div class="value-row">
      <button class="step-btn" @click="adjust(-0.5)">−</button>
      <div class="value-display">
        <span class="value-num">{{ displayWeight }}</span>
        <span class="value-unit">kg</span>
      </div>
      <button class="step-btn" @click="adjust(0.5)">+</button>
    </div>

    <div class="slider-wrapper">
      <el-slider
        :model-value="modelValue.weightKg"
        @update:model-value="onWeightChange"
        :min="30"
        :max="200"
        :step="0.5"
        :show-tooltip="false"
      />
    </div>

    <!-- BMI 四色条 -->
    <div class="bmi-section" v-if="bmi > 0">
      <div class="bmi-value">BMI {{ bmi }}</div>
      <div class="bmi-bar-wrapper">
        <div class="bmi-bar">
          <div class="bmi-segment underweight" :class="{ active: bmi < 18.5 }">偏瘦</div>
          <div class="bmi-segment ideal" :class="{ active: bmi >= 18.5 && bmi < 24 }">理想</div>
          <div class="bmi-segment overweight" :class="{ active: bmi >= 24 && bmi < 28 }">偏胖</div>
          <div class="bmi-segment obese" :class="{ active: bmi >= 28 }">肥胖</div>
        </div>
        <div v-if="bmi >= 10 && bmi <= 40" class="bmi-indicator" :style="{ left: bmiPosition + '%' }">▼</div>
        <div class="bmi-edge-label left" v-if="bmi < 10">▼ &lt;10</div>
        <div class="bmi-edge-label right" v-if="bmi > 40">&gt;40 ▼</div>
      </div>
      <p class="bmi-hint">BMI = 体重(kg) / 身高(m)²</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const modelValue = defineModel()

function onWeightChange(val) {
  modelValue.value.weightKg = Math.round(val * 2) / 2
}

function adjust(delta) {
  const val = Math.round((modelValue.value.weightKg + delta) * 2) / 2
  modelValue.value.weightKg = Math.min(200, Math.max(30, val))
}

const displayWeight = computed(() => {
  const v = modelValue.value.weightKg
  return v % 1 === 0 ? v + '.0' : String(v)
})

const bmi = computed(() => {
  const h = modelValue.value.heightCm / 100
  if (!h || !modelValue.value.weightKg) return 0
  return Math.round(modelValue.value.weightKg / (h * h) * 10) / 10
})

const bmiPosition = computed(() => {
  const pos = ((bmi.value - 10) / 30) * 100
  return Math.min(Math.max(pos, 0), 100)
})
</script>

<style scoped>
.step-content {
  text-align: center;
  padding: 20px;
  overflow: hidden;
}
.step-title {
  font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 16px;
}

/* 数值行 */
.value-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 72px;
  margin-bottom: 24px;
}

.step-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #e5e7eb;
  background: #fff;
  font-size: 22px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  font-family: inherit;
  line-height: 1;
  padding: 0;
}
.step-btn:hover {
  border-color: #10b981;
  color: #10b981;
  background: #f0fdf4;
}
.step-btn:active {
  transform: scale(0.92);
  background: #d1fae5;
}

.value-display {
  display: flex;
  align-items: baseline;
}
.value-num {
  font-size: 56px;
  font-weight: 800;
  color: #10b981;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.value-unit {
  font-size: 22px;
  color: #9ca3af;
  font-weight: 500;
  margin-left: 6px;
}

/* 滑动条 */
.slider-wrapper :deep(.el-slider) {
  width: 100%;
}
.slider-wrapper :deep(.el-slider__runway) {
  width: 100%;
}

/* BMI 四色条 */
.bmi-section {
  width: 85%;
  max-width: 320px;
  margin: 24px auto 0;
  contain: layout style;
}
.bmi-value {
  font-size: 20px;
  font-weight: 700;
  color: #064e3b;
  margin-bottom: 8px;
  height: 28px;
  line-height: 28px;
}

.bmi-bar-wrapper {
  position: relative;
  padding-top: 22px;
}
.bmi-bar {
  display: flex;
  height: 28px;
  border-radius: 14px;
  overflow: hidden;
}
.bmi-segment {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: rgba(255,255,255,0.8);
  font-weight: 600;
  transition: font-size 0.2s, box-shadow 0.2s;
}
.bmi-segment.active {
  font-size: 12px;
  color: #fff;
  font-weight: 700;
  box-shadow: inset 0 0 12px rgba(255,255,255,0.4);
}
.underweight { background: #60a5fa; }
.ideal       { background: #34d399; }
.overweight  { background: #fbbf24; }
.obese       { background: #f87171; }

.bmi-indicator {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  font-size: 14px;
  color: #1f2937;
  transition: left 0.25s ease;
}

.bmi-edge-label {
  position: absolute;
  top: 0;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}
.bmi-edge-label.left  { left: 0; }
.bmi-edge-label.right { right: 0; }

.bmi-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 10px;
  height: 18px;
  line-height: 18px;
}
</style>
