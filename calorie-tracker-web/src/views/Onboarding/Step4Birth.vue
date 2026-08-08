<template>
  <div class="step-content">
    <h2 class="step-title">你的年龄</h2>

    <div class="age-input-wrap">
      <input
        v-model="modelValue.age"
        type="number"
        class="age-input"
        placeholder="18"
        min="10"
        max="100"
        inputmode="numeric"
      />
      <span class="age-unit">岁</span>
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

.age-input-wrap {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10px;
}

.age-input {
  width: 140px;
  border: none;
  border-bottom: 3px solid #d1fae5;
  background: transparent;
  font-size: 64px;
  font-weight: 800;
  color: #10b981;
  text-align: center;
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  outline: none;
  padding: 4px 0;
  transition: border-color 0.3s;
  caret-color: #10b981;
  line-height: 1.1;
}
.age-input:focus {
  border-bottom-color: #10b981;
}
/* 去掉数字输入框的 spinner */
.age-input::-webkit-inner-spin-button,
.age-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.age-input[type="number"] {
  appearance: textfield;
}
.age-input::placeholder {
  color: #a7f3d0;
  font-size: 56px;
}

.age-unit {
  font-size: 24px;
  color: #9ca3af;
  font-weight: 500;
}
</style>
