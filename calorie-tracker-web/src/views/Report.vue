<template>
  <div class="report-page">
    <!-- 顶部标题 + 周报/月报切换 -->
    <div class="report-header">
      <div class="report-title-block">
        <h2 class="report-title">数据报告</h2>
        <p v-if="report.startDate" class="report-sub">{{ reportPeriodText }}</p>
      </div>
      <div class="period-toggle">
        <button class="period-btn" :class="{ active: period === 'week' }" @click="switchPeriod('week')">周报</button>
        <button class="period-btn" :class="{ active: period === 'month' }" @click="switchPeriod('month')">月报</button>
      </div>
    </div>

    <!-- 空态：完全没有数据 -->
    <div v-if="!loading && !hasAnyData" class="empty-state">
      <div class="empty-icon">📊</div>
      <p class="empty-text">还没有可统计的数据</p>
      <p class="empty-sub">先去记录饮食 / 体重 / 运动，这里会自动生成你的周报月报</p>
      <el-button type="primary" @click="$router.push('/home')">去记录</el-button>
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon">🍽️</div>
          <div class="stat-label">平均每日摄入</div>
          <div class="stat-value">{{ report.averageCalories ?? 0 }} <span class="unit">千卡</span></div>
          <div class="stat-sub">目标 {{ report.dailyCalorieTarget ?? '—' }} 千卡</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">⚖️</div>
          <div class="stat-label">体重变化</div>
          <div class="stat-value">{{ weightChangeText }}</div>
          <div class="stat-sub">{{ weightSubText }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏃</div>
          <div class="stat-label">运动消耗</div>
          <div class="stat-value">{{ report.exerciseCalories ?? 0 }} <span class="unit">千卡</span></div>
          <div class="stat-sub">共 {{ denominator }} 天</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📅</div>
          <div class="stat-label">打卡天数</div>
          <div class="stat-value">{{ checkInDays }} <span class="unit">/ {{ denominator }} 天</span></div>
          <div class="stat-sub">有饮食或体重记录</div>
        </div>
        <div class="stat-card stat-card-wide">
          <div class="stat-icon">🎯</div>
          <div class="stat-label">达成目标天数</div>
          <div class="stat-value">{{ report.goalDays ?? 0 }} <span class="unit">/ {{ denominator }} 天</span></div>
          <div class="stat-sub">摄入在目标 ±10% 内</div>
        </div>
      </div>

      <!-- 成就徽章 -->
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">成就徽章</span>
          <span class="card-tag">{{ unlockedCount }}/{{ badges.length }} 已解锁</span>
        </div>
        <div class="badge-grid">
          <div
            v-for="b in badges"
            :key="b.key"
            class="badge-item"
            :class="{ unlocked: b.unlocked }"
          >
            <div class="badge-icon">{{ b.icon }}</div>
            <div class="badge-name">{{ b.name }}</div>
            <div class="badge-desc">{{ b.desc }}</div>
          </div>
        </div>
      </div>

      <!-- 摄入趋势图 -->
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">摄入趋势</span>
          <span v-if="report.dailyCalorieTarget" class="card-tag">目标线 {{ report.dailyCalorieTarget }} 千卡</span>
          <button v-if="period === 'month'" class="month-detail-btn" @click="$router.push('/report/intake')">整月详情 ›</button>
        </div>
        <div ref="intakeChartRef" class="chart-box"></div>
      </div>

      <!-- 打卡日历 -->
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">打卡日历</span>
          <span class="card-tag">有记录 = 已打卡</span>
        </div>
        <div class="calendar-month-row">
          <button class="calendar-month-btn" @click="goToPrevMonth">←</button>
          <span class="calendar-month-label">{{ calendarMonthLabel }}</span>
          <button class="calendar-month-btn" :class="{ disabled: !canGoNextMonth }" @click="goToNextMonth" :disabled="!canGoNextMonth">→</button>
        </div>
        <div ref="checkinChartRef" class="chart-box calendar-chart-box"></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { fillTrend } from '@/utils/trend'
import { useToday } from '@/composables/useToday'
import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, GraphicComponent, CalendarComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, HeatmapChart, GridComponent, TooltipComponent, GraphicComponent, CalendarComponent, VisualMapComponent, CanvasRenderer])

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

// 响应式"今天"：页面常驻跨零点自动更新
const { today: actualToday } = useToday()

const period = ref('week') // week | month
const loading = ref(true)
const report = ref({})
const intakeChartRef = ref(null)
const checkinChartRef = ref(null)

let intakeChart = null
let checkinChart = null

// 某年某月（1 起）的总天数
function daysInMonth(y, m) {
  return new Date(y, m, 0).getDate()
}

// 统计分母：周报固定 7 天，月报 = 当月总天数（不看有没有记录）
const denominator = computed(() => {
  if (period.value === 'week') return 7
  const now = new Date()
  return daysInMonth(now.getFullYear(), now.getMonth() + 1)
})

// 数据窗口：周报近 7 天，月报 = 本月 1 号至今
const windowDays = computed(() => {
  if (period.value === 'week') return 7
  return new Date().getDate()
})

// 周报显示日期区间；月报只显示月份（如 "2026年8月"）
const reportPeriodText = computed(() => {
  if (period.value === 'month') {
    const date = report.value.endDate || report.value.startDate || actualToday.value
    const [y, m] = date.split('-')
    return `${y}年${Number(m)}月`
  }
  return `${report.value.startDate} ~ ${report.value.endDate}`
})

const checkInDays = computed(() => (report.value.checkInDates || []).length)
const hasAnyData = computed(() => {
  const r = report.value
  return (r.averageCalories > 0) || (r.exerciseCalories > 0)
    || (r.weightChange !== null && r.weightChange !== undefined) || checkInDays.value > 0
})

// ===== 成就徽章 =====
const streakData = ref({})
const latestWeight = ref(null)
const ACHIEVEMENT_KEY = 'achievements_unlocked'

const badges = computed(() => {
  const streak = streakData.value.longestStreak || 0
  const startW = user.value.weightKg
  const loss = startW && latestWeight.value != null
    ? Math.max(0, Math.round((startW - latestWeight.value) * 10) / 10)
    : 0
  return [
    { key: 'first-checkin', icon: '🏅', name: '首次打卡', desc: '有第一条记录', unlocked: streak >= 1 },
    { key: 'streak-7', icon: '📅', name: '连续打卡 7 天', desc: '最长连续 7 天', unlocked: streak >= 7 },
    { key: 'streak-30', icon: '🗓️', name: '连续打卡 30 天', desc: '最长连续 30 天', unlocked: streak >= 30 },
    { key: 'loss-2.5', icon: '⚖️', name: '初见成效', desc: '减重 2.5kg', unlocked: loss >= 2.5 },
    { key: 'loss-5', icon: '🏆', name: '稳步前行', desc: '减重 5kg', unlocked: loss >= 5 },
    { key: 'loss-10', icon: '🚀', name: '脱胎换骨', desc: '减重 10kg', unlocked: loss >= 10 }
  ]
})
const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)

async function loadAchievements() {
  try {
    const [streakRes, latestRes] = await Promise.all([
      api.get(`/stats/${user.value.id}/streak`),
      api.get(`/weights/latest/${user.value.id}`)
    ])
    streakData.value = streakRes
    latestWeight.value = latestRes.weightKg ?? null
    await nextTick()
    syncBadgeToasts()
  } catch (e) { /* 无记录 */ }
}

// 新解锁徽章 → 弹提示，已解锁记 localStorage 防重复弹
function syncBadgeToasts() {
  const stored = new Set(JSON.parse(localStorage.getItem(ACHIEVEMENT_KEY) || '[]'))
  const newly = badges.value.filter(b => b.unlocked && !stored.has(b.key))
  if (newly.length) {
    newly.forEach(b => stored.add(b.key))
    localStorage.setItem(ACHIEVEMENT_KEY, JSON.stringify([...stored]))
    ElMessage.success(`🎉 解锁成就：${newly.map(b => b.name).join('、')}`)
  }
}

const weightChangeText = computed(() => {
  const wc = report.value.weightChange
  if (wc === null || wc === undefined) return '—'
  if (wc === 0) return '0.0 kg'
  return `${wc > 0 ? '+' : ''}${wc} kg`
})
const weightSubText = computed(() => {
  const { startWeight, endWeight } = report.value
  if (startWeight !== null && startWeight !== undefined) return `${startWeight} → ${endWeight} kg`
  return '期内体重记录不足'
})

// ===== 周报/月报数据 =====
// 请求竞态守卫：周期切换/日历翻页序号，旧响应晚到直接丢弃
let reportSeq = 0
let calendarSeq = 0

async function loadReport() {
  const seq = reportSeq
  loading.value = true
  try {
    const res = await api.get(`/stats/${user.value.id}/report?endDate=${actualToday.value}&days=${windowDays.value}`)
    if (seq !== reportSeq) return
    report.value = res
    await nextTick()
    if (seq !== reportSeq) return
    renderIntakeChart()
  } catch (e) {
    if (seq !== reportSeq) return
    report.value = {}
  } finally {
    if (seq === reportSeq) loading.value = false
  }
}

function switchPeriod(p) {
  if (period.value === p) return
  period.value = p
  reportSeq++
  loadReport()
}

// 月报摄入趋势：固定最近 7 天且必须在当月内；月初往前推越界时起点收回当月 1 日，
// 不足的天用当月内（endDate 之后）的未来天补 0，不显示上月日期
function buildMonthTrend(trendObj, endDate) {
  const base = new Date(endDate)
  const firstOfMonth = new Date(base.getFullYear(), base.getMonth(), 1)
  let start = new Date(base)
  start.setDate(start.getDate() - 6)
  if (start < firstOfMonth) start = firstOfMonth
  const arr = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    const k = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    arr.push({ date: k, value: trendObj[k] || 0 })
  }
  return arr
}

function renderIntakeChart() {
  if (!intakeChartRef.value) return
  if (!intakeChart) intakeChart = echarts.getInstanceByDom(intakeChartRef.value) || echarts.init(intakeChartRef.value)

  // 周报/月报卡片统一默认展示最近 7 天；月报整月数据在详情页看
  const days = period.value === 'month'
    ? 7
    : (report.value.days || windowDays.value)
  const trendArr = period.value === 'month'
    ? buildMonthTrend(report.value.intakeTrend || {}, report.value.endDate || actualToday.value)
    : fillTrend(report.value.intakeTrend || {}, report.value.endDate || actualToday.value, days)
  const target = report.value.dailyCalorieTarget

  intakeChart.setOption({
    grid: { top: 28, left: 8, right: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        const val = p.value
        let status = ''
        if (target && val > 0) {
          const pct = Math.round(val / target * 100)
          status = pct <= 80 ? '低于目标' : pct <= 100 ? '接近目标' : '超出目标'
        }
        return `${p.name}<br/>摄入 ${val} 千卡${status ? `<br/>${status}` : ''}`
      },
      backgroundColor: '#fff', borderColor: '#e5e7eb',
      textStyle: { color: '#374151', fontSize: 11 },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-radius: 6px; padding: 3px 8px; line-height: 1.4;'
    },
    xAxis: {
      type: 'category',
      data: trendArr.map(t => t.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      axisLabel: { fontSize: 10, color: '#9ca3af' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { fontSize: 10, color: '#9ca3af' }
    },
    series: [{
      type: 'bar',
      data: trendArr.map(t => t.value),
      barWidth: '55%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#34d399' },
          { offset: 1, color: '#059669' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      markLine: target ? {
        silent: true,
        symbol: 'none',
        label: { show: true, formatter: '目标 {c}', position: 'end', color: '#ea580c', fontSize: 10 },
        lineStyle: { color: '#ea580c', type: 'dashed', width: 1 },
        data: [{ yAxis: target }]
      } : {}
    }]
  })
}

// ===== 打卡日历 =====
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const calendarMonthLabel = computed(() => `${calendarYear.value}年${calendarMonth.value}月`)
const canGoNextMonth = computed(() => {
  const now = new Date()
  return calendarYear.value < now.getFullYear()
    || (calendarYear.value === now.getFullYear() && calendarMonth.value < now.getMonth() + 1)
})
const calendarCheckInDates = ref(new Set())

function goToPrevMonth() {
  if (calendarMonth.value === 1) {
    calendarMonth.value = 12
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
}
function goToNextMonth() {
  if (!canGoNextMonth.value) return
  if (calendarMonth.value === 12) {
    calendarMonth.value = 1
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
}

async function loadCalendar() {
  const seq = calendarSeq
  const year = calendarYear.value
  const month = calendarMonth.value
  const lastDay = new Date(year, month, 0).getDate()
  const firstOfMonth = `${year}-${String(month).padStart(2, '0')}-01`
  const lastOfMonth = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  const endDate = lastOfMonth > actualToday.value ? actualToday.value : lastOfMonth
  const daysDiff = Math.ceil((new Date(endDate) - new Date(firstOfMonth)) / (1000 * 60 * 60 * 24)) + 1
  try {
    const res = await api.get(`/stats/${user.value.id}/report?endDate=${endDate}&days=${daysDiff}`)
    if (seq !== calendarSeq) return
    calendarCheckInDates.value = new Set(res.checkInDates || [])
  } catch (e) {
    if (seq !== calendarSeq) return
    calendarCheckInDates.value = new Set()
  }
  await nextTick()
  if (seq !== calendarSeq) return
  renderCheckinChart()
}

function renderCheckinChart() {
  if (!checkinChartRef.value) return
  if (!checkinChart) checkinChart = echarts.getInstanceByDom(checkinChartRef.value) || echarts.init(checkinChartRef.value)
  const isDark = document.documentElement.dataset.theme === 'dark'

  const year = calendarYear.value
  const month = calendarMonth.value
  const lastDay = new Date(year, month, 0).getDate()
  const firstOfMonth = `${year}-${String(month).padStart(2, '0')}-01`
  const lastOfMonth = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  const data = []

  for (let day = 1; day <= lastDay; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (dateStr > actualToday.value) {
      data.push([dateStr, 0, 'future'])
    } else if (calendarCheckInDates.value.has(dateStr)) {
      data.push([dateStr, 1])
    } else {
      data.push([dateStr, 0])
    }
  }

  checkinChart.setOption({
    tooltip: {
      position: 'top',
      formatter: function (p) {
        if (!p.value) return ''
        const dateStr = p.value[0]
        const d = new Date(dateStr)
        const label = `${d.getMonth() + 1}月${d.getDate()}日`
        if (dateStr > actualToday.value) return `${label}<br/>未来`
        return `${label}<br/>${p.value[1] === 1 ? '✅ 已打卡' : '未打卡'}`
      },
      backgroundColor: isDark ? '#1e293b' : '#fff', borderColor: isDark ? '#334155' : '#e5e7eb',
      textStyle: { color: isDark ? '#e2e8f0' : '#374151', fontSize: 11 },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-radius: 6px; padding: 3px 8px; line-height: 1.4;'
    },
    visualMap: {
      min: 0, max: 1,
      type: 'piecewise', orient: 'horizontal', left: 'center', bottom: 0,
      hoverLink: true,
      pieces: [
        { lt: 1, color: isDark ? '#1e293b' : '#e5e7eb', label: '未打卡' },
        { min: 1, color: '#059669', label: '已打卡' }
      ],
      textStyle: { fontSize: 10, color: isDark ? '#94a3b8' : '#6b7280' },
      itemWidth: 11, itemHeight: 11
    },
    calendar: {
      top: 20, left: 0, right: 0,
      range: [firstOfMonth, lastOfMonth],
      cellSize: ['auto', 24],
      yearLabel: { show: false },
      monthLabel: { show: false },
      dayLabel: { fontSize: 10, color: isDark ? '#64748b' : '#9ca3af', nameMap: ['日', '一', '二', '三', '四', '五', '六'] },
      itemStyle: { borderWidth: 2, borderColor: isDark ? '#0f172a' : '#fff', borderRadius: 3 },
      splitLine: { show: false }
    },
    series: [{
      type: 'heatmap', coordinateSystem: 'calendar', data: data,
      emphasis: { itemStyle: { borderColor: '#059669', borderWidth: 2, shadowColor: 'rgba(5,150,105,0.5)', shadowBlur: 6 } },
      stateAnimation: { duration: 0 }
    }]
  })
}

watch([calendarMonth, calendarYear], () => {
  calendarSeq++
  loadCalendar()
})

// ===== 生命周期 =====
const onResize = () => {
  intakeChart?.resize()
  checkinChart?.resize()
}

onMounted(() => {
  loadReport()
  loadCalendar()
  loadAchievements()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  intakeChart?.dispose()
  checkinChart?.dispose()
})
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: transparent;
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 16px 40px;
  animation: page-fade-in 0.4s ease;
}

/* ===== 顶部标题 + 切换 ===== */
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}
.report-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}
.report-sub {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 4px 0 0;
}
.period-toggle {
  display: flex;
  background: var(--color-border-light);
  border-radius: 999px;
  padding: 3px;
  flex-shrink: 0;
}
.period-btn {
  border: none;
  background: transparent;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.period-btn.active {
  background: var(--color-glass-strong);
  color: var(--color-primary);
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* ===== 统计卡片 ===== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.stat-card {
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}
.stat-card-wide {
  grid-column: 1 / -1;
}
.stat-icon {
  font-size: 20px;
  margin-bottom: 6px;
}
.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}
.stat-value .unit {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
}
.stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

/* ===== 卡片通用 ===== */
.chart-card {
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  padding: 20px;
  margin-bottom: var(--space-md);
  box-shadow: var(--shadow-sm);
}
.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.card-title {
  font-weight: 700;
  color: var(--color-text-secondary);
  flex: 1;
  font-size: var(--text-base);
  letter-spacing: 0.04em;
}
.card-tag {
  font-size: 11px;
  color: var(--color-text-muted);
  background: var(--color-primary-bg-light);
  padding: 3px 8px;
  border-radius: 999px;
}
.month-detail-btn {
  border: none;
  background: var(--color-primary-bg-light);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}
.month-detail-btn:hover { background: var(--color-primary-bg); }
.chart-box {
  height: 220px;
  width: 100%;
}
.calendar-chart-box {
  height: 240px;
}

/* ===== 成就徽章 ===== */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 14px 8px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-bg-light);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-base);
}
.badge-item.unlocked {
  background: var(--color-primary-bg);
  border-color: var(--color-primary-lighter);
}
.badge-icon {
  font-size: 26px;
  margin-bottom: 6px;
  filter: grayscale(1);
  opacity: 0.4;
  transition: all var(--transition-base);
}
.badge-item.unlocked .badge-icon {
  filter: none;
  opacity: 1;
}
.badge-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-muted);
}
.badge-item.unlocked .badge-name {
  color: var(--color-primary-dark);
}
.badge-desc {
  font-size: 10px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

/* ===== 打卡日历月份导航 ===== */
.calendar-month-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 4px;
}
.calendar-month-btn {
  width: 26px; height: 26px;
  border: none; border-radius: 50%;
  background: var(--color-primary-bg-light);
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.calendar-month-btn:hover { background: var(--color-primary-bg); }
.calendar-month-btn.disabled { color: var(--color-border); background: var(--color-border-light); cursor: not-allowed; }
.calendar-month-label {
  font-size: var(--text-sm); font-weight: 700; color: var(--color-text);
  min-width: 70px; text-align: center;
}

/* ===== 空态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 24px;
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
}
.empty-sub {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0 0 16px;
}
</style>
