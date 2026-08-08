<template>
  <div class="step-content">
    <h2 class="step-title">确认你的信息</h2>
    <div class="info-card">
      <div class="info-row"><span class="label">性别</span><span>{{ modelValue.gender === 'MALE' ? '男' : '女' }}</span></div>
      <div class="info-row"><span class="label">身高</span><span>{{ modelValue.heightCm }} cm</span></div>
      <div class="info-row"><span class="label">体重</span><span>{{ modelValue.weightKg }} kg</span></div>
      <div class="info-row"><span class="label">年龄</span><span>{{ modelValue.age }} 岁</span></div>
      <div class="info-row"><span class="label">目标体重</span><span>{{ modelValue.targetWeightKg }} kg</span></div>
      <div class="info-row"><span class="label">目标天数</span><span>{{ modelValue.targetDays }} 天</span></div>
    </div>
  </div>
</template>

<script setup>
const modelValue = defineModel()
</script>

<style scoped>
.step-content { text-align: center; padding: 20px; }
.step-title { font-size: 24px; font-weight: 700; color: #064e3b; margin-bottom: 24px; }
.info-card {
  background: #f0fdf4; border-radius: 16px; padding: 24px; text-align: left;
}
.info-row {
  display: flex; justify-content: space-between; padding: 10px 0;
  border-bottom: 1px solid #dcfce7; font-size: 15px;
}
.info-row:last-child { border: none; }
.label { color: #9ca3af; }
</style>
