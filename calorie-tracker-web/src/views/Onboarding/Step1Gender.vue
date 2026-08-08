<template>
  <div class="step-content">
    <h2 class="step-title">你的性别</h2>
    <div class="gender-cards">
      <div
        class="gender-card"
        :class="{ selected: modelValue.gender === 'MALE' }"
        @click="modelValue.gender = 'MALE'"
      >
        <div class="gender-icon">♂</div>
        <div class="gender-label">男</div>
      </div>
      <div
        class="gender-card"
        :class="{ selected: modelValue.gender === 'FEMALE' }"
        @click="modelValue.gender = 'FEMALE'"
      >
        <div class="gender-icon">♀</div>
        <div class="gender-label">女</div>
      </div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()
</script>

<style scoped>
.step-content {
  text-align: center; padding: 20px;
}
.step-title {
  font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 32px;
}
.gender-cards {
  display: flex; gap: 20px; justify-content: center;
}
.gender-card {
  width: 130px; height: 150px;
  border-radius: 20px; border: 2px solid #e5e7eb;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.3s ease;
  background: #fff;
}
.gender-card.selected {
  border-color: #10b981; background: #f0fdf4;
  box-shadow: 0 4px 20px rgba(16,185,129,0.2);
  transform: scale(1.05);
}
.gender-icon { font-size: 48px; margin-bottom: 8px; }
.gender-label { font-size: 18px; font-weight: 600; color: #064e3b; }
</style>