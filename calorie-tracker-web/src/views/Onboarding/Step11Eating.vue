<template>
  <div class="step-content">
    <h2 class="step-title">你的饮食习惯</h2>
    <div class="option-list">
      <div v-for="o in options" :key="o.value"
        class="option-row" :class="{ selected: modelValue.eatingHabit === o.value }"
        @click="modelValue.eatingHabit = o.value">
        <span>{{ o.label }}</span>
        <span v-if="modelValue.eatingHabit === o.value" class="check">✓</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()
const options = [
  { value: 'REGULAR', label: '规律就餐' },
  { value: 'ADJUSTABLE', label: '不规律，但可以调整' },
  { value: 'HARD', label: '很难调整' }
]
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 24px; }
.option-list { max-width: 320px; margin: 0 auto; }
.option-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; margin-bottom: 10px; border-radius: 14px;
  border: 2px solid #e5e7eb; cursor: pointer; transition: all 0.3s;
  background: #fff; font-size: 16px; color: #064e3b;
}
.option-row.selected { border-color: #10b981; background: #f0fdf4; }
.check { color: #10b981; font-weight: 700; font-size: 18px; }
</style>