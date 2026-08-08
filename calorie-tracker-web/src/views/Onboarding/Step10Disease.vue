<template>
  <div class="step-content">
    <h2 class="step-title">是否有以下疾病</h2>
    <p class="subtitle">可多选</p>
    <div class="disease-grid">
      <div v-for="d in diseases" :key="d.value"
        class="disease-card" :class="{ selected: modelValue.diseases.includes(d.value) }"
        @click="toggle(d.value)">
        <div class="disease-icon">{{ d.icon }}</div>
        <div class="disease-label">{{ d.label }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()

const diseases = [
  { value: '糖尿病',   label: '糖尿病',   icon: '🩸' },
  { value: '高血压',   label: '高血压',   icon: '❤️' },
  { value: '高血脂',   label: '高血脂',   icon: '🫀' },
  { value: '脂肪肝',   label: '脂肪肝',   icon: '🫁' },
  { value: '甲减',     label: '甲减',     icon: '🦋' },
  { value: '痛风',     label: '痛风',     icon: '🦵' },
  { value: '冠心病',   label: '冠心病',   icon: '💔' },
  { value: '无',       label: '都没有',   icon: '✅' }
]

function toggle(val) {
  const arr = modelValue.value.diseases || []
  if (val === '无') {
    modelValue.value.diseases = ['无']
    return
  }
  const filtered = arr.filter(d => d !== '无')
  const idx = filtered.indexOf(val)
  if (idx >= 0) filtered.splice(idx, 1)
  else filtered.push(val)
  modelValue.value.diseases = filtered.length ? filtered : ['无']
}
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 4px; }
.subtitle { font-size: 14px; color: #9ca3af; margin-bottom: 24px; }

.disease-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  max-width: 320px;
  margin: 0 auto;
}
.disease-card {
  padding: 14px 8px;
  border-radius: 14px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}
.disease-card.selected {
  border-color: #10b981;
  background: #f0fdf4;
}
.disease-icon { font-size: 24px; margin-bottom: 2px; }
.disease-label { font-size: 13px; font-weight: 600; color: #064e3b; }
</style>
