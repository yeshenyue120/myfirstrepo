<template>
  <div class="step-content">
    <h2 class="step-title">希望多久达成目标</h2>

    <div class="picker-row">
      <el-input-number
        v-model="modelValue.targetDays"
        :min="minDays || 7"
        :max="730"
        :step="7"
        size="large"
        class="days-input"
      />
      <span class="unit-label">天</span>
    </div>

    <!-- 天数提示行 -->
    <div class="days-hint">
      <span class="hint-text" v-if="recommendedDays > 0 && !atMin">
        推荐 <strong>{{ recommendedDays }}</strong> 天（温和减重）
      </span>
      <span class="hint-warn" v-if="atMin">
        ⚡ 已是最快健康速度
      </span>
    </div>

    <div class="calc-info">
      <div class="calc-row">
        <span class="label">约</span>
        <span class="val">{{ weeks }}</span>
        <span class="label">周</span>
      </div>
      <div class="calc-row" v-if="weeklyDelta">
        <span class="label">预计每周{{ delta > 0 ? '减' : '增' }}</span>
        <span class="val">{{ weeklyDelta }}</span>
        <span class="label">kg</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'

const modelValue = defineModel()

const delta = computed(() => modelValue.value.weightKg - modelValue.value.targetWeightKg)
const weeks = computed(() => (modelValue.value.targetDays / 7).toFixed(1))
const weeklyDelta = computed(() => {
  const d = Math.abs(delta.value)
  if (!d || !modelValue.value.targetDays) return null
  return (d / (modelValue.value.targetDays / 7)).toFixed(2)
})

// ---- 最早达标天数（25% TDEE 最大安全缺口）----
const minDays = computed(() => {
  const d = modelValue.value
  const w = d.weightKg, h = d.heightCm, g = d.gender, a = d.age, tw = d.targetWeightKg
  if (!w || !h || !g || !a || !tw) return 0

  let bmr
  if (g === 'MALE') {
    bmr = 10 * w + 6.25 * h - 5 * a + 5
  } else {
    bmr = 10 * w + 6.25 * h - 5 * a - 161
  }

  const tdee = bmr * 1.2
  const maxDeficit = tdee * 0.25
  if (maxDeficit <= 0) return 0

  const deltaKg = Math.abs(w - tw)
  return Math.ceil((deltaKg * 7700) / maxDeficit)
})

// ---- 推荐天数（15% TDEE 温和缺口）----
const recommendedDays = computed(() => {
  const d = modelValue.value
  const w = d.weightKg, h = d.heightCm, g = d.gender, a = d.age, tw = d.targetWeightKg
  if (!w || !h || !g || !a || !tw) return 0

  let bmr
  if (g === 'MALE') {
    bmr = 10 * w + 6.25 * h - 5 * a + 5
  } else {
    bmr = 10 * w + 6.25 * h - 5 * a - 161
  }

  const tdee = bmr * 1.2
  const deficit = tdee * 0.15
  const deltaKg = Math.abs(w - tw)
  return Math.max(Math.round((deltaKg * 7700) / deficit), 14)
})

const atMin = computed(() => minDays.value > 0 && modelValue.value.targetDays <= minDays.value)

// 如果当前天数 < 最早达标天数，自动修正
watch(minDays, (min) => {
  if (min > 0 && modelValue.value.targetDays < min) {
    modelValue.value.targetDays = min
  }
})
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 20px; }

.picker-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 10px;
}
.days-input { width: 170px; }
.unit-label { font-size: 20px; color: #064e3b; font-weight: 600; }

.days-hint {
  height: 22px; line-height: 22px; margin-bottom: 16px;
}
.hint-text {
  font-size: 13px; color: #9ca3af;
}
.hint-text strong { color: #6b7280; font-weight: 600; }
.hint-warn {
  font-size: 13px; color: #d97706; font-weight: 500;
}

.calc-info {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
}
.calc-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.calc-row .val {
  color: #064e3b;
  font-weight: 700;
  font-size: 18px;
  min-width: 48px;
  text-align: center;
}
.label { color: #9ca3af; font-size: 15px; }
</style>
