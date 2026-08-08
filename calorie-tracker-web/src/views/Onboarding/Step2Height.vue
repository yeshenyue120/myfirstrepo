<template>
  <div class="step-content">
    <h2 class="step-title">你的身高</h2>

    <!-- 数值 + 微调按钮 -->
    <div class="value-row">
      <button class="step-btn" @click="adjust(-1)">−</button>
      <div class="value-display">
        <span class="value-num">{{ modelValue.heightCm }}</span>
        <span class="value-unit">cm</span>
      </div>
      <button class="step-btn" @click="adjust(1)">+</button>
    </div>

    <div class="slider-wrapper">
      <el-slider
        v-model="modelValue.heightCm"
        :min="130"
        :max="220"
        :step="1"
        :show-tooltip="false"
      />
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()

function adjust(delta) {
  modelValue.value.heightCm = Math.min(220, Math.max(130, modelValue.value.heightCm + delta))
}
</script>

<style scoped>
.step-content {
  text-align: center;
  padding: 20px;
  overflow: hidden;
}
.step-title {
  font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 20px;
}

/* 数值行：按钮 + 数字 + 按钮 */
.value-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 16px;
  height: 72px;
  margin-bottom: 28px;
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
  align-self: center;
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
</style>
