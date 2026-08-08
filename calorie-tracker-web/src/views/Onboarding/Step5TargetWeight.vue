<template>
  <div class="step-content">
    <h2 class="step-title">你的目标体重</h2>

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
        :model-value="modelValue.targetWeightKg"
        @update:model-value="onWeightChange"
        :min="30"
        :max="200"
        :step="0.5"
        :show-tooltip="false"
      />
    </div>

    <div class="delta-info" v-if="delta !== 0">
      <span :class="delta > 0 ? 'lose' : 'gain'">
        {{ delta > 0 ? '减重' : '增重' }} {{ Math.abs(delta).toFixed(1) }}kg
      </span>
      <span class="percent">（{{ percent }}%）</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const modelValue = defineModel()

function onWeightChange(val) {
  modelValue.value.targetWeightKg = Math.round(val * 2) / 2
}

function adjust(delta) {
  const val = Math.round((modelValue.value.targetWeightKg + delta) * 2) / 2
  modelValue.value.targetWeightKg = Math.min(200, Math.max(30, val))
}

const displayWeight = computed(() => {
  const v = modelValue.value.targetWeightKg
  return v % 1 === 0 ? v + '.0' : String(v)
})

const delta = computed(() => {
  return modelValue.value.weightKg - modelValue.value.targetWeightKg
})

const percent = computed(() => {
  if (!modelValue.value.weightKg) return 0
  return Math.abs((delta.value / modelValue.value.weightKg) * 100).toFixed(1)
})
</script>

<style scoped>
.step-content {
  text-align: center;
  padding: 20px;
  overflow: hidden;
}
.step-title {
  font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 10px;
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

.delta-info {
  font-size: 18px;
  font-weight: 600;
  height: 28px;
  line-height: 28px;
  margin-top: 12px;
}
.lose { color: #f87171; }
.gain { color: #60a5fa; }
.percent { color: #9ca3af; font-size: 16px; }
</style>
