<template>
  <div class="step-content">
    <div class="intro-icon">📋</div>
    <h2 class="step-title">接下来几个问题</h2>
    <p class="intro-text">帮你评估最适合的减重方案</p>
  </div>
</template>

<script setup>
const modelValue = defineModel()
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.intro-icon { font-size: 64px; margin-bottom: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 12px; }
.intro-text { font-size: 16px; color: #6b7280; }
</style>