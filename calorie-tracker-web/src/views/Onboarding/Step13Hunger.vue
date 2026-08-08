<template>
  <div class="step-content">
    <h2 class="step-title">是否容易感到饥饿</h2>
    <div class="option-list">
      <div v-for="o in options" :key="o.value"
        class="option-row" :class="{ selected: modelValue.hungerLevel === o.value }"
        @click="modelValue.hungerLevel = o.value">
        <span>{{ o.label }}</span>
        <span v-if="modelValue.hungerLevel === o.value" class="check">✓</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()
const options = [
  { value: 'RARE', label: '很少饿' },
  { value: 'SOMETIMES', label: '偶尔饿' },
  { value: 'OFTEN', label: '经常饿' }
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