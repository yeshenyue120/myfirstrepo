<template>
  <div class="step-content">
    <h2 class="step-title">是否使用过减重处方药</h2>
    <div class="option-cards">
      <div class="option-card" :class="{ selected: modelValue.usedWeightLossDrug === true }" @click="modelValue.usedWeightLossDrug = true">
        <div class="option-icon">💊</div><div class="option-label">是</div>
      </div>
      <div class="option-card" :class="{ selected: modelValue.usedWeightLossDrug === false }" @click="modelValue.usedWeightLossDrug = false">
        <div class="option-icon">✅</div><div class="option-label">否</div>
      </div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 32px; }
.option-cards { display: flex; gap: 20px; justify-content: center; }
.option-card {
  width: 130px; height: 130px; border-radius: 20px; border: 2px solid #e5e7eb;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.3s; background: #fff;
}
.option-card.selected { border-color: #10b981; background: #f0fdf4; transform: scale(1.05); box-shadow: 0 4px 20px rgba(16,185,129,0.2); }
.option-icon { font-size: 40px; margin-bottom: 8px; }
.option-label { font-size: 18px; font-weight: 600; color: #064e3b; }
</style>