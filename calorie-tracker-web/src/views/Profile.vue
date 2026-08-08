<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="avatar">{{ user?.username?.charAt(0) }}</div>
      <h2>{{ user?.username }}</h2>
      <p>{{ user?.email }}</p>
    </div>

    <!-- 数据总览 -->
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-label">每日目标</span>
        <span class="stat-value">{{ Math.round(user?.dailyCalorieTarget || 0) }}</span>
        <span class="stat-unit">千卡</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">当前体重</span>
        <span class="stat-value">{{ latestWeight !== null ? latestWeight : '--' }}</span>
        <span class="stat-unit">kg</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">目标体重</span>
        <span class="stat-value">{{ user?.targetWeightKg || '--' }}</span>
        <span class="stat-unit">kg</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">连续打卡</span>
        <span class="stat-value">{{ streak.currentStreak }}</span>
        <span class="stat-unit">天</span>
      </div>
    </div>

    <!-- 目标进度 -->
    <div v-if="latestWeight !== null" class="progress-card">
      <div class="progress-labels">
        <span>起始 {{ user?.weightKg }} kg</span>
        <span>目标 {{ user?.targetWeightKg }} kg</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: Math.round(gaugePercent * 100) + '%' }"></div>
      </div>
      <div class="progress-info">
        <span v-if="totalDelta !== 0" class="progress-delta" :class="totalDelta > 0 ? 'down' : 'up'">
          {{ totalDelta > 0 ? '已减' : '已增' }} {{ Math.abs(totalDelta) }} kg
        </span>
        <span class="progress-pct">{{ Math.round(gaugePercent * 100) }}%</span>
      </div>
      <div class="streak-line">最长连续 {{ streak.longestStreak }} 天 · 最近打卡 {{ streak.lastCheckInDate || '--' }}</div>
    </div>

    <!-- 设置：外观 -->
    <div class="settings-card">
      <div class="settings-title">外观</div>
      <div class="settings-row">
        <span class="settings-label">深色模式</span>
        <el-switch :model-value="isDark" @change="toggleDarkMode" />
      </div>
    </div>

    <!-- 设置：每日提醒 -->
    <div class="settings-card">
      <div class="settings-title">每日提醒</div>
      <div class="settings-row">
        <span class="settings-label">开启提醒</span>
        <el-switch v-model="reminders.enabled" @change="onReminderChange" />
      </div>
      <template v-if="reminders.enabled">
        <div class="settings-row">
          <span class="settings-label">💧 喝水提醒</span>
          <el-switch v-model="reminders.water.enabled" @change="onReminderChange" />
        </div>
        <div v-if="reminders.water.enabled" class="settings-sub">
          <span class="settings-sub-label">时间点（点击选择）</span>
          <div class="time-chips">
            <span
              v-for="t in ALL_WATER_TIMES"
              :key="t"
              class="time-chip"
              :class="{ on: reminders.water.times.includes(t) }"
              @click="toggleWaterTime(t)"
            >{{ t }}</span>
          </div>
        </div>
        <div class="settings-row">
          <span class="settings-label">📝 晚间记录提醒</span>
          <el-switch v-model="reminders.record.enabled" @change="onReminderChange" />
        </div>
        <div v-if="reminders.record.enabled" class="settings-sub">
          <span class="settings-sub-label">提醒时间</span>
          <el-time-select
            v-model="reminders.record.time"
            start="18:00"
            step="00:30"
            end="23:59"
            placeholder="选择时间"
            style="width: 120px;"
            @change="onReminderChange"
          />
        </div>
        <div class="settings-hint">⚠️ 需浏览器通知权限，请允许本站通知</div>
      </template>
    </div>

    <div class="info-cards">
      <div class="info-card" @click="$router.push('/edit-profile')">
        <span>✏️ 编辑个人资料</span>
        <span class="arrow">›</span>
      </div>
      <div class="info-card" @click="$router.push('/foods?tab=favorites')">
        <span>❤️ 我的收藏</span>
        <span class="arrow">›</span>
      </div>
      <div class="info-card" @click="handleExport">
        <span>📥 导出数据</span>
        <span class="arrow" v-if="!exporting">›</span>
        <span v-else class="loading-text">导出中…</span>
      </div>
      <div class="info-card logout-card" @click="handleLogout">
        <span>🚪 退出登录</span>
        <span class="arrow">›</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import api from '@/api'
import { loadReminderConfig, saveReminderConfig, requestNotificationPermission } from '@/utils/reminders'

const router = useRouter()
const user = ref({})
const latestWeight = ref(null)
const streak = ref({ currentStreak: 0, longestStreak: 0 })
const exporting = ref(false)

// ===== 深色模式 =====
const isDark = ref(document.documentElement.dataset.theme === 'dark')
function toggleDarkMode(val) {
  isDark.value = val
  if (val) {
    document.documentElement.dataset.theme = 'dark'
    localStorage.setItem('theme', 'dark')
  } else {
    delete document.documentElement.dataset.theme
    localStorage.setItem('theme', 'light')
  }
}

// ===== 每日提醒设置 =====
const ALL_WATER_TIMES = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
const reminders = ref(loadReminderConfig())

function onReminderChange() {
  saveReminderConfig(reminders.value)
  if (reminders.value.enabled) requestNotificationPermission()
}
function toggleWaterTime(t) {
  const idx = reminders.value.water.times.indexOf(t)
  if (idx >= 0) reminders.value.water.times.splice(idx, 1)
  else reminders.value.water.times.push(t)
  saveReminderConfig(reminders.value)
}

onMounted(async () => {
  user.value = JSON.parse(localStorage.getItem('user') || '{}')
  loadLatestWeight()
  loadStreak()
})

async function loadLatestWeight() {
  try {
    const res = await api.get(`/weights/latest/${user.value.id}`)
    latestWeight.value = res.weightKg
  } catch (e) { latestWeight.value = null }
}

async function loadStreak() {
  try {
    const res = await api.get(`/stats/${user.value.id}/streak`)
    streak.value = res
  } catch (e) { /* 无记录 */ }
}

// 目标进度
const gaugePercent = computed(() => {
  const s = user.value.weightKg, t = user.value.targetWeightKg, c = latestWeight.value
  if (!s || !t || !c || s === t) return 0
  return Math.min(Math.max(Math.abs(s - c) / Math.abs(s - t), 0), 1)
})
const totalDelta = computed(() => {
  if (latestWeight.value === null) return 0
  const s = user.value.weightKg
  if (!s) return 0
  return Math.round((s - latestWeight.value) * 10) / 10
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出确认', { type: 'warning' })
  } catch (e) { return }
  localStorage.removeItem('user')
  localStorage.removeItem('token')
  ElMessage.success('已退出登录')
  router.push('/login')
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await api.get(`/users/${user.value.id}/export`)
    const blob = new Blob([JSON.stringify(res, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `calorie-data-${user.value.username || user.value.id}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('数据已导出')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  } finally { exporting.value = false }
}
</script>

<style scoped>
.profile-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--space-lg);
  background: transparent;
  min-height: 100vh;
  animation: page-fade-in 0.35s ease;
}
.profile-header {
  text-align: center; padding: 36px 0;
}
.avatar {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: var(--color-text-inverse); font-size: 36px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 8px 28px rgba(5,150,105,0.28);
}
.profile-header h2 { margin: 0; color: var(--color-primary-dark); font-size: var(--text-xl); font-weight: 700; }
.profile-header p { color: var(--color-text-muted); font-size: var(--text-sm); margin-top: 4px; }
.info-cards {
  margin-top: var(--space-xl);
  animation: anim-stagger-slide;
}
.info-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 20px; margin-bottom: 10px;
  background: var(--color-surface-glass);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  cursor: pointer; font-size: var(--text-base); color: var(--color-primary-dark);
  font-weight: 600;
  transition: all var(--transition-base);
  opacity: 0;
  animation: slide-up 0.4s ease forwards;
}
.info-card:nth-child(1) { animation-delay: 0.05s; }
.info-card:nth-child(2) { animation-delay: 0.15s; }
.info-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  background: var(--color-glass-strong);
}
.info-card:active { transform: scale(0.98); }
.arrow {
  font-size: 22px; color: var(--color-text-muted);
  transition: transform var(--transition-fast);
}
.info-card:hover .arrow { transform: translateX(3px); }
.logout-card { color: var(--color-danger); }
.loading-text { font-size: 12px; color: var(--color-text-muted); }

/* ===== 数据总览 ===== */
.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
}
.stat-card {
  background: var(--color-surface-glass); backdrop-filter: blur(10px);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-xs);
  padding: 14px 8px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.stat-label { font-size: 11px; color: var(--color-text-muted); }
.stat-value { font-size: 20px; font-weight: 800; color: var(--color-primary-dark); font-variant-numeric: tabular-nums; }
.stat-unit { font-size: 10px; color: var(--color-text-muted); }

/* ===== 目标进度 ===== */
.progress-card {
  margin-top: 14px;
  background: var(--color-surface-glass); backdrop-filter: blur(10px);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-xs);
  padding: 18px 20px;
}
.progress-labels {
  display: flex; justify-content: space-between;
  font-size: 12px; margin-bottom: 10px;
}
.progress-labels span:first-child { color: var(--color-text-muted); }
.progress-labels span:last-child { color: var(--color-accent); font-weight: 600; }
.progress-track {
  height: 12px; background: var(--color-border);
  border-radius: var(--radius-full); overflow: hidden;
}
.progress-fill {
  height: 100%; border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-primary-lighter), var(--color-primary));
  transition: width 0.8s ease;
}
.progress-info {
  display: flex; justify-content: center; align-items: baseline; gap: 12px;
  margin-top: 10px;
}
.progress-delta { font-size: 13px; font-weight: 700; }
.progress-delta.down { color: var(--color-primary); }
.progress-delta.up { color: var(--color-danger); }
.progress-pct { font-size: 13px; color: var(--color-text-muted); }
.streak-line {
  margin-top: 8px; text-align: center;
  font-size: 12px; color: var(--color-text-muted);
}

/* ===== 设置：每日提醒 ===== */
.settings-card {
  margin-top: 14px;
  background: var(--color-surface-glass);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  padding: 18px 20px;
}
.settings-title {
  font-size: var(--text-base); font-weight: 700;
  color: var(--color-primary-dark);
  letter-spacing: 0.04em;
  margin-bottom: 12px;
}
.settings-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-border-light);
}
.settings-row:last-child { border-bottom: none; }
.settings-label {
  font-size: 14px; font-weight: 600;
  color: var(--color-primary-dark);
}
.settings-sub { padding: 10px 0; }
.settings-sub-label {
  font-size: 12px; color: var(--color-text-muted);
  display: block; margin-bottom: 8px;
}
.time-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.time-chip {
  padding: 4px 10px; border-radius: 999px;
  font-size: 12px; color: var(--color-text-muted);
  background: var(--color-border-light); cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}
.time-chip.on {
  background: var(--color-primary); color: #fff; font-weight: 600;
}
.settings-hint { font-size: 11px; color: var(--color-text-muted); margin-top: 8px; }
</style>