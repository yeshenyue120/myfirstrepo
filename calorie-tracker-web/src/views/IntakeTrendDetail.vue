<template>
  <div class="intake-detail-page">
    <!-- 顶部：返回 + 标题 -->
    <div class="detail-header">
      <span class="detail-back" @click="router.back()">←</span>
      <span class="detail-title">{{ monthTitle }} · 摄入趋势</span>
    </div>

    <!-- 统计信息 -->
    <div class="detail-meta">
      <span>📊 {{ recordDays }} 天有记录</span>
      <span v-if="target">目标线 {{ target }} 千卡</span>
    </div>

    <!-- 整月柱状图 -->
    <div v-loading="loading" ref="chartRef" class="detail-chart"></div>
    <div class="detail-hint">悬停柱体查看每天摄入 · 浅色格子为尚未到期的日期</div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { useToday } from '@/composables/useToday'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, GridComponent, TooltipComponent, GraphicComponent, CanvasRenderer])

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const chartRef = ref(null)
const loading = ref(true)

const now = new Date()
const year = now.getFullYear()
const month = now.getMonth() + 1
const monthTitle = `${year}年${month}月`
const { today: actualToday } = useToday()
const totalDays = new Date(year, month, 0).getDate() // 本月总天数，如 31

const target = ref(0)
const recordDays = ref(0)
let chart = null

async function loadDetail() {
  loading.value = true
  try {
    // 数据窗口：本月 1 号至今（月末即整月）
    const res = await api.get(`/stats/${user.value.id}/report?endDate=${actualToday.value}&days=${now.getDate()}`)
    target.value = res.dailyCalorieTarget || 0
    const trend = res.intakeTrend || {}

    // 组装整月槽位：未来日期值为 0（浅色占位）
    const data = []
    let recorded = 0
    for (let day = 1; day <= totalDays; day++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      const value = trend[dateStr] || 0
      if (dateStr <= actualToday.value && value > 0) recorded++
      data.push({ date: dateStr, value, isFuture: dateStr > actualToday.value })
    }
    recordDays.value = recorded
    await nextTick()
    renderChart(data)
  } catch (e) {
    // 出错时也渲染一次空图，避免一直 loading
    await nextTick()
    renderChart([])
  } finally {
    loading.value = false
  }
}

function renderChart(data) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.getInstanceByDom(chartRef.value) || echarts.init(chartRef.value)

  chart.setOption({
    grid: { top: 30, left: 8, right: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = data[params[0].dataIndex]
        if (!item) return ''
        const label = `${item.date.slice(5)}`
        if (item.isFuture) return `${label}<br/>还未到`
        return `${label}<br/>摄入 ${item.value} 千卡`
      },
      backgroundColor: '#fff', borderColor: '#e5e7eb',
      textStyle: { color: '#374151', fontSize: 11 },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-radius: 6px; padding: 3px 8px; line-height: 1.4;'
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      // 本月视图只显示日号（标题已含月份），避免 MM-DD 拥挤
      axisLabel: { fontSize: 10, color: '#9ca3af', interval: 0, formatter: (v) => String(Number(v.slice(3))) }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { fontSize: 10, color: '#9ca3af' }
    },
    series: [{
      type: 'bar',
      data: data.map(d => ({
        value: d.value,
        itemStyle: d.isFuture
          ? { color: '#f3f4f6', borderRadius: [2, 2, 0, 0] }
          : d.value > 0
            ? {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#34d399' },
                  { offset: 1, color: '#059669' }
                ]),
                borderRadius: [4, 4, 0, 0]
              }
            : { color: '#e5e7eb', borderRadius: [2, 2, 0, 0] }
      })),
      barWidth: '60%',
      barMinHeight: 2,
      markLine: target.value ? {
        silent: true,
        symbol: 'none',
        label: { show: true, formatter: '目标 {c}', position: 'end', color: '#ea580c', fontSize: 10 },
        lineStyle: { color: '#ea580c', type: 'dashed', width: 1 },
        data: [{ yAxis: target.value }]
      } : {}
    }]
  })
}

const onResize = () => chart?.resize()

onMounted(() => {
  window.addEventListener('resize', onResize)
  loadDetail()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.intake-detail-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 20px 24px 48px;
  animation: page-fade-in 0.4s ease;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.detail-back {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-primary-bg-light);
  color: var(--color-primary);
  font-size: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.detail-back:hover { background: var(--color-primary-bg); }
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 0.04em;
}
.detail-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.detail-meta span {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-primary-bg-light);
  padding: 3px 10px;
  border-radius: 999px;
}
.detail-chart {
  width: 100%;
  height: 380px;
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}
.detail-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 12px;
  text-align: center;
}
</style>
