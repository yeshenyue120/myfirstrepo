<template>
  <div class="home-page">
    <!-- ===== 热量日历热力图 ===== -->
    <div class="chart-card calendar-card">
      <div class="card-header">
        <span class="card-icon">📅</span>
        <span class="card-title">热量日历</span>
        <span class="card-date">{{ dateDisplayText }}</span>
        <div class="calendar-day-nav">
          <button class="day-nav-btn" @click="goToPrevDay">◀</button>
          <button v-if="!isToday" class="day-nav-today" @click="goToToday">跳转到今天</button>
          <button class="day-nav-btn" :class="{ disabled: !canGoNext }" @click="goToNextDay" :disabled="!canGoNext">▶</button>
        </div>
        <span class="report-link" @click="$router.push('/report')">周报</span>
      </div>
      <div class="calendar-month-row">
        <button class="calendar-month-btn" @click="goToPrevMonth">←</button>
        <span class="calendar-month-label">{{ calendarMonthLabel }}</span>
        <button class="calendar-month-btn" :class="{ disabled: !canGoNextMonth }" @click="goToNextMonth" :disabled="!canGoNextMonth">→</button>
      </div>
      <div ref="calendarChartRef" class="chart-box calendar-chart-box"></div>
      <div class="calendar-hint">点击色块筛选 · 可补录和修改最近 3 天内的记录</div>
    </div>

    <!-- ===== 体重仪表盘 ===== -->
    <div class="weight-card">
      <div class="card-header">
        <span class="card-icon">⚖️</span>
        <span class="card-title">今日体重</span>
        <span class="card-date">{{ selectedDate }}</span>
      </div>
      <div class="gauge-section">
        <!-- 空状态 -->
        <div v-if="latestWeight === null" class="gauge-empty">
          <span class="empty-icon">📊</span>
          <span>记录今日体重，开始追踪</span>
        </div>

        <!-- 进度条仪表盘 -->
        <div v-else class="gauge-bar-wrap">
          <div class="gauge-bar-labels">
            <span class="label-start">起始 {{ user.weightKg }} kg</span>
            <span class="label-target">目标 {{ gaugeTarget }} kg</span>
          </div>
          <div class="gauge-bar-track">
            <div class="gauge-bar-fill" :style="{ width: Math.round(gaugePercent * 100) + '%' }"></div>
            <div class="gauge-bar-dot" :style="{ left: Math.round(gaugePercent * 100) + '%' }"></div>
          </div>
          <div class="gauge-bar-info">
            <span class="gauge-num">{{ latestWeight }} <small>kg</small></span>
            <span class="gauge-delta" v-if="totalDelta !== 0" :class="totalDelta > 0 ? 'down' : 'up'">
              {{ totalDelta > 0 ? '已减' : '已增' }} {{ Math.abs(totalDelta) }} kg
            </span>
            <span class="gauge-progress-text">{{ Math.round(gaugePercent * 100) }}%</span>
          </div>
        </div>
      </div>
      <el-button class="record-btn" @click="if (checkDateEditable()) { resetWeightForm(); showWeightDialog = true }">记录体重</el-button>

      <!-- 修改近几日体重提示 -->
      <div class="weight-hint">想修改近几日体重？点击上方日历选中日期，再点「记录体重」保存即可</div>
    </div>

    <!-- ===== 体重趋势折线图 ===== -->
    <div class="chart-card">
      <div class="card-header">
        <span class="card-icon">📈</span>
        <span class="card-title">体重趋势</span>
        <span class="card-date">近30天</span>
      </div>
      <div v-if="weightHistory.length >= 2" ref="weightChartRef" class="chart-box"></div>
      <div v-else class="chart-empty">再记录几天就有趋势啦</div>
    </div>

    <!-- ===== 今日热量总览 ===== -->
    <div class="calorie-card">
      <div class="card-header">
        <span class="card-icon">🔥</span>
        <span class="card-title">今日热量总览</span>
      </div>

      <!-- 大数字：还可吃 -->
      <div class="remaining-hero">
        <span class="remaining-num" :class="{ over: remaining < 0 }">
          {{ remaining > 0 ? Math.round(remaining) : Math.abs(Math.round(remaining)) }}
        </span>
        <span class="remaining-label">{{ remaining > 0 ? '还可吃（千卡）' : '已超出（千卡）' }}</span>
      </div>

      <!-- 进度条 -->
      <div class="cal-bar-wrapper">
        <div class="cal-bar">
          <div class="cal-fill" :style="{ width: calPercent + '%' }" :class="{ over: calPercent > 100 }"></div>
        </div>
      </div>

      <!-- 底部三行小字 -->
      <div class="cal-details">
        <div class="cal-detail-item">
          <span class="detail-label">已摄入</span>
          <span class="detail-val">{{ Math.round(todayTotal) }}</span>
        </div>
        <div class="cal-detail-item">
          <span class="detail-label">运动<small class="hint"> (计入×0.9)</small></span>
          <span class="detail-val out">{{ Math.round(exerciseTotal*0.9) }}</span>
        </div>
        <div class="cal-detail-item">
          <span class="detail-label">目标</span>
          <span class="detail-val">{{ Math.round(targetCalories) }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 热量趋势柱状图 ===== -->
    <div class="chart-card">
      <div class="card-header">
        <span class="card-icon">📊</span>
        <span class="card-title">热量趋势</span>
        <span class="card-date">近7天</span>
      </div>
      <div v-if="Object.keys(calorieTrend).length > 0" ref="calorieChartRef" class="chart-box"></div>
      <div v-else class="chart-empty">从今天开始记录吧</div>
      <div v-if="goalRateText" class="trend-stat">{{ goalRateText }}</div>
    </div>

    <!-- ===== 营养素环形图 ===== -->
    <div class="chart-card">
      <div class="card-header">
        <span class="card-icon">🍩</span>
        <span class="card-title">今日营养素</span>
        <span class="card-date">灰色环 = 推荐摄入</span>
      </div>
      <div v-if="nutritionData && hasNutritionTarget" ref="nutritionChartRef" class="chart-box chart-box-sm"></div>
      <div v-else class="chart-empty">完成引导后即可查看营养素目标 🎯</div>
      <div v-if="nutritionData && hasNutritionTarget" class="nutrition-legend">
        <span v-for="k in macroKeys" :key="k" v-show="(nutritionData[k + 'Target'] || 0) > 0" class="nutrition-legend-item">
          <i class="legend-dot" :style="{ background: macroColors[k] }"></i>
          {{ macroNames[k] }} 目标 {{ Math.round(nutritionData[k + 'Target'] || 0) }}g
        </span>
      </div>
      <div v-if="nutritionWarnings.length" class="nutrition-warnings">
        <div v-for="w in nutritionWarnings" :key="w.text" class="nutrition-warning" :style="{ color: w.color }">
          ⚠️ {{ w.text }}
        </div>
      </div>
    </div>

    <!-- ===== 运动记录 ===== -->
    <div class="exercise-card">
      <div class="card-header">
        <span class="card-icon">🏃</span>
        <span class="card-title">运动记录</span>
        <span class="card-date">{{ selectedDate }}</span>
      </div>
      <div v-if="exerciseRecords.length === 0" class="exercise-empty">
        <span class="empty-text">今天还没有运动，快动起来吧</span>
      </div>
      <div v-else class="exercise-summary">
        <div class="exercise-total">
          <span class="exercise-big-num">{{ Math.round(exerciseTotal) }}</span>
          <span class="exercise-unit">千卡 已消耗</span>
        </div>
        <div class="exercise-list">
          <div v-for="(rec, i) in exerciseRecords" :key="rec.id || i" class="exercise-row">
            <span class="ex-type">{{ getExerciseIcon(rec.exerciseType) }} {{ rec.exerciseType }}</span>
            <span class="ex-detail">{{ rec.durationMin }} 分钟</span>
            <span class="ex-cal">-{{ Math.round(rec.caloriesBurned) }} 千卡</span>
            <span class="ex-edit" @click.stop="router.push({ path: '/exercise-record', query: { recordId: rec.id, date: selectedDate, type: rec.exerciseType, dur: rec.durationMin } })" title="编辑">✎</span>
            <span class="ex-del" @click.stop="deleteExercise(rec)" title="删除">🗑</span>
          </div>
        </div>
      </div>
      <el-button class="record-btn exercise-btn" @click="checkDateEditable() && router.push({ path: '/exercise-record', query: { date: selectedDate } })">记录运动</el-button>
    </div>

    <!-- ===== 三餐详情 ===== -->
    <div class="meals-card">
      <div class="card-header">
        <span class="card-icon">🍽️</span>
        <span class="card-title">三餐详情</span>
      </div>
      <div v-for="meal in meals" :key="meal.type" class="meal-row" @click="openMealDetail(meal)">
        <span class="meal-row-icon">{{ meal.icon }}</span>
        <span class="meal-row-name">{{ meal.label }}</span>
        <span class="meal-row-cal">{{ meal.calories }} 千卡</span>
        <span class="meal-row-arrow">›</span>
      </div>
    </div>

    <!-- ===== 喝水 + 睡眠（同行双栏） ===== -->
    <div class="water-sleep-row">
      <!-- 喝水记录 -->
      <div class="ws-card">
        <div class="card-header">
          <span class="card-icon">💧</span>
          <span class="card-title">喝水</span>
          <span class="card-detail-link" @click="showWaterDetail = true">详情 ›</span>
        </div>
        <div class="water-overview">
          <span class="water-big-num">{{ waterTotalMl }}</span>
          <span class="water-unit">/ {{ waterTargetMl }} ml</span>
        </div>
        <div class="water-bar-track">
          <div class="water-bar-fill" :style="{ width: Math.min(waterPercent, 100) + '%' }"></div>
        </div>
        <div class="water-actions">
          <el-button size="small" class="water-add-btn" @click="addWater(250)">+250</el-button>
          <el-button size="small" class="water-add-btn" @click="addWater(500)">+500</el-button>
          <el-button size="small" class="water-custom-btn" @click="showWaterDialog = true">自定义</el-button>
        </div>
      </div>

      <!-- 睡眠记录 -->
      <div class="ws-card">
        <div class="card-header">
          <span class="card-icon">😴</span>
          <span class="card-title">睡眠</span>
          <span class="card-detail-link" @click="showSleepDetail = true">详情 ›</span>
        </div>
        <div v-if="sleepRecord" class="sleep-overview">
          <span class="sleep-big-num">{{ formatSleepMin(sleepRecord.durationMin) }}</span>
          <span class="sleep-target">目标 {{ sleepTargetLabel }}</span>
        </div>
        <div v-else class="exercise-empty">
          <span class="empty-text">今天还没记录 😴</span>
          <span class="sleep-target">目标 {{ sleepTargetLabel }}</span>
        </div>
        <el-button class="record-btn exercise-btn" @click="openSleepDialog">{{ sleepRecord ? '编辑睡眠' : '记录睡眠' }}</el-button>
      </div>
    </div>

    <!-- ===== 喝水详情弹窗 ===== -->
    <el-dialog v-model="showWaterDetail" title="喝水详情" width="88%" align-center>
      <div class="detail-chart-title">近 7 天喝水趋势</div>
      <div ref="waterTrendChartRef" style="width:100%;height:180px;"></div>
      <div class="detail-setting">
        <span class="detail-setting-label">每日目标</span>
        <el-input-number v-model="waterTargetInput" :min="500" :max="5000" :step="100" style="width:130px;" size="small" />
        <span class="detail-setting-unit">ml</span>
        <el-button type="primary" size="small" @click="saveWaterTarget">保存</el-button>
      </div>
      <div class="detail-list-title" style="margin-top:16px;">全部记录</div>
      <div v-if="waterRecords.length === 0" style="text-align:center;color:#9ca3af;padding:20px;">暂无记录</div>
      <div v-else class="detail-list">
        <div v-for="rec in waterRecords" :key="rec.id" class="detail-row">
          <span class="detail-time">{{ rec.createdAt ? rec.createdAt.slice(0, 16).replace('T', ' ') : '' }}</span>
          <span class="detail-val">{{ rec.amountMl }} ml</span>
          <span class="detail-del" @click.stop="deleteWaterRecord(rec)">🗑</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showWaterDetail = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 睡眠详情弹窗 ===== -->
    <el-dialog v-model="showSleepDetail" title="睡眠详情" width="88%" align-center>
      <div class="detail-chart-title">近 7 天睡眠趋势</div>
      <div ref="sleepTrendChartRef" style="width:100%;height:180px;"></div>
      <div class="detail-setting">
        <span class="detail-setting-label">每日目标</span>
        <el-input-number v-model="sleepTargetInput" :min="60" :max="720" :step="30" style="width:130px;" size="small" />
        <span class="detail-setting-unit">分钟</span>
        <el-button type="primary" size="small" @click="saveSleepTarget">保存</el-button>
      </div>
      <template #footer>
        <el-button @click="showSleepDetail = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 记录/编辑体重弹窗 ===== -->
    <el-dialog v-model="showWeightDialog" :title="editingWeightId ? '编辑体重' : '记录体重'" width="85%" align-center>
      <el-form :model="weightForm" label-width="0">
        <el-form-item>
          <el-input-number v-model="weightForm.weightKg" :min="20" :max="300" :step="0.1" placeholder="今日体重 (kg)" style="width:100%" size="large" />
        </el-form-item>
        <el-form-item>
          <el-date-picker v-model="weightForm.recordedDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" style="width:100%" size="large" :disabled-date="disableFutureDate" />
        </el-form-item>
        <el-form-item>
          <div class="metric-hint">体脂、腰围、臀围（选填）</div>
          <div class="body-metric-row">
            <div class="metric-item">
              <span class="metric-label">体脂率 %</span>
              <el-input-number v-model="weightForm.bodyFatPct" :min="0" :max="70" :step="0.1" :precision="1" placeholder="体脂 %" controls-position="right" style="width:100%" />
            </div>
            <div class="metric-item">
              <span class="metric-label">腰围 (cm)</span>
              <el-input-number v-model="weightForm.waistCm" :min="20" :max="200" :step="0.1" :precision="1" placeholder="腰围 cm" controls-position="right" style="width:100%" />
            </div>
            <div class="metric-item">
              <span class="metric-label">臀围 (cm)</span>
              <el-input-number v-model="weightForm.hipCm" :min="20" :max="200" :step="0.1" :precision="1" placeholder="臀围 cm" controls-position="right" style="width:100%" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWeightDialog = false">取消</el-button>
        <el-button type="primary" @click="handleWeightSave" :loading="savingWeight">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 运动记录 ===== -->

    <!-- ===== 自定义喝水量弹窗 ===== -->
    <el-dialog v-model="showWaterDialog" title="记录喝水" width="85%" align-center>
      <el-form :model="waterForm" label-width="0">
        <el-form-item>
          <el-input-number v-model="waterForm.amountMl" :min="50" :max="2000" :step="50" style="width:100%" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWaterDialog = false">取消</el-button>
        <el-button type="primary" @click="handleWaterSave" :loading="savingWater">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 记录/编辑睡眠弹窗 ===== -->
    <el-dialog v-model="showSleepDialog" :title="editingSleepId ? '编辑睡眠' : '记录睡眠'" width="85%" align-center>
      <el-form :model="sleepForm" label-position="top">
        <el-form-item label="睡眠时长（分钟）">
          <el-input-number v-model="sleepForm.durationMin" :min="60" :max="960" :step="30" style="width:100%" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="editingSleepId" type="danger" text @click="handleSleepDelete" :loading="deletingSleep" style="margin-right:auto">删除</el-button>
        <el-button @click="showSleepDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSleepSave" :loading="savingSleep">
          {{ editingSleepId ? '更新' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 三餐详情弹窗（查看/修改/删除已记录条目） ===== -->
    <el-dialog v-model="showMealDialog" :title="activeMeal ? activeMeal.label + '详情' : ''" width="85%" :close-on-click-modal="false" align-center>
      <div v-if="activeMeal">
        <div v-if="activeMeal.items.length === 0" class="meal-detail-empty">
          <span class="empty-icon">🍽️</span>
          <span>这一餐还没有记录</span>
        </div>
        <div v-else class="meal-detail-list">
          <div v-for="rec in activeMeal.items" :key="rec.id" class="meal-detail-row">
            <div class="meal-detail-info">
              <span class="meal-detail-name">{{ rec.foodName }}</span>
              <span class="meal-detail-cal">{{ Math.round(rec.totalCalories) }} 千卡</span>
            </div>
            <div class="meal-detail-actions">
              <el-input-number v-model="editingGramsMap[rec.id]" :min="1" :max="2000" :step="10" size="small" controls-position="right" style="width: 110px" />
              <span class="grams-unit">克</span>
              <el-button size="small" @click="handleRecordSave(rec)" :loading="savingRecordId === rec.id">保存</el-button>
              <el-button size="small" type="danger" text @click="handleRecordDelete(rec)" :loading="deletingRecordId === rec.id">删除</el-button>
            </div>
          </div>
        </div>
        <div class="meal-detail-footer">
          <div class="meal-detail-footer-left">
            <el-button class="meal-detail-add-btn" @click="addFoodToActiveMeal">＋ 添加食物</el-button>
            <el-button class="meal-detail-copy-btn" @click="copyYesterdayMeal" :loading="copyingMeal">
              复制昨天的{{ activeMeal.label }}
            </el-button>
          </div>
          <span class="meal-detail-total">合计 {{ Math.round(activeMeal.calories) }} 千卡</span>
        </div>
      </div>
    </el-dialog>

    <!-- ===== 快速添加 FAB ===== -->
    <button class="fab-btn" @click="showQuickDialog = true" title="快速添加">＋</button>

    <!-- ===== 快速添加弹窗 ===== -->
    <el-dialog v-model="showQuickDialog" title="快速添加" width="80%" align-center>
      <div class="quick-actions">
        <div class="quick-action" @click="quickAddWeight">
          <span class="quick-icon">⚖️</span>
          <span>记录体重</span>
        </div>
        <div class="quick-action" @click="quickAddExercise">
          <span class="quick-icon">🏃</span>
          <span>记录运动</span>
        </div>
        <div class="quick-action" @click="quickAddWater">
          <span class="quick-icon">💧</span>
          <span>记录喝水</span>
        </div>
        <div class="quick-action" @click="quickAddSleep">
          <span class="quick-icon">😴</span>
          <span>记录睡眠</span>
        </div>
        <div class="quick-action" @click="quickAddMeal('BREAKFAST')">
          <span class="quick-icon">🌅</span>
          <span>记录早餐</span>
        </div>
        <div class="quick-action" @click="quickAddMeal('LUNCH')">
          <span class="quick-icon">🌞</span>
          <span>记录午餐</span>
        </div>
        <div class="quick-action" @click="quickAddMeal('DINNER')">
          <span class="quick-icon">🌙</span>
          <span>记录晚餐</span>
        </div>
        <div class="quick-action" @click="quickAddMeal('SNACK')">
          <span class="quick-icon">🍪</span>
          <span>记录加餐</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { fillTrend } from '@/utils/trend'
import { getExerciseIcon } from '@/data/exerciseCatalog'
import { useToday } from '@/composables/useToday'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, HeatmapChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, GraphicComponent, CalendarComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, PieChart, HeatmapChart, GridComponent, TooltipComponent, LegendComponent, GraphicComponent, CalendarComponent, VisualMapComponent, CanvasRenderer])

const router = useRouter()
const route = useRoute()

// 处理快捷操作（/home?action=weight|exercise）
function handleQuickAction(action) {
  if (action === 'weight') {
    resetWeightForm()
    showWeightDialog.value = true
  } else if (action === 'exercise') {
    router.push({ path: '/exercise-record', query: { date: selectedDate } })
  }
  if (action) {
    router.replace({ query: {} })
  }
}
watch(() => route.query.action, (val) => { if (val) handleQuickAction(val) })

// ===== 快速添加 FAB =====
const showQuickDialog = ref(false)

function quickAddWeight() {
  if (!checkDateEditable()) return
  showQuickDialog.value = false
  resetWeightForm()
  showWeightDialog.value = true
}

function quickAddExercise() {
  showQuickDialog.value = false
  router.push({ path: '/exercise-record', query: { date: selectedDate } })
}

function quickAddWater() {
  showQuickDialog.value = false
  showWaterDialog.value = true
}

function quickAddSleep() {
  showQuickDialog.value = false
  openSleepDialog()
}

function quickAddMeal(mealType) {
  showQuickDialog.value = false
  openMealPicker(mealType)
}

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// 响应式"今天"：页面常驻跨零点自动更新（useToday 监听可见性 + 定时刷新）
const { today: actualToday } = useToday()
const savedDate = sessionStorage.getItem('home_selectedDate')
const selectedDate = ref(savedDate || todayStr())
const isToday = computed(() => selectedDate.value === actualToday.value)
const canGoNext = computed(() => selectedDate.value < actualToday.value)

// 日期显示文本
const dateDisplayText = computed(() => {
  if (selectedDate.value === actualToday.value) return '今天'
  const d = new Date(selectedDate.value)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]}`
})

// 当天运动消耗（用于日历热力图调整目标）
const todayExerciseCal = ref(0)

// 3天前日期（用于限制修改，随"今天"联动更新）
const threeDaysAgoStr = computed(() => {
  const d = new Date(actualToday.value)
  d.setDate(d.getDate() - 3)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
const isDateTooOld = computed(() => selectedDate.value < threeDaysAgoStr.value)

function checkDateEditable() {
  if (isDateTooOld.value) {
    ElMessage.warning('只能修改三天内的记录')
    return false
  }
  return true
}

// ===== 图表 =====
const weightChartRef = ref(null)
const calorieChartRef = ref(null)
const nutritionChartRef = ref(null)
const weightHistory = ref([])
const calorieTrend = ref({})
const calorieExerciseTrend = ref({})
// 热量目标达成率：近 7 天净摄入（摄入−运动×0.9）在目标 ±10% 内的天数 / 7
const goalRateText = computed(() => {
  const target = user.value.dailyCalorieTarget
  if (!target || Object.keys(calorieTrend.value).length === 0) return ''
  const filled = fillTrend(calorieTrend.value, selectedDate.value, 7)
  const exFilled = fillTrend(calorieExerciseTrend.value, selectedDate.value, 7)
  const lower = target * 0.9
  const upper = target * 1.1
  const achieved = filled.filter((e, i) => {
    const net = e.value - (exFilled[i]?.value || 0) * 0.9
    return net > 0 && net >= lower && net <= upper
  }).length
  return `近 7 天 ${achieved}/7 天在目标内`
})
const nutritionData = ref(null)
const nutritionWarnings = ref([])

// 营养素元数据（环形图 + 图例共用）
const macroKeys = ['protein', 'fat', 'carbs']
const macroNames = { protein: '蛋白质', fat: '脂肪', carbs: '碳水' }
const macroColors = { protein: '#3b82f6', fat: '#f59e0b', carbs: '#10b981' }

// 用户是否有营养素推荐目标（完成引导后才有）
const hasNutritionTarget = computed(() => {
  const d = nutritionData.value
  return !!(d && (d.proteinTarget > 0 || d.fatTarget > 0 || d.carbsTarget > 0))
})

let weightChart = null
let calorieChart = null
let nutritionChart = null
let calendarChart = null

// ===== 热量日历热力图 =====
const calendarChartRef = ref(null)
const calendarData = ref({})
const exerciseTrendData = ref({})
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const calendarMonthLabel = computed(() => `${calendarYear.value}年${calendarMonth.value}月`)
const canGoNextMonth = computed(() => {
  const now = new Date()
  return calendarYear.value < now.getFullYear() || (calendarYear.value === now.getFullYear() && calendarMonth.value < now.getMonth() + 1)
})


// ===== 体重 =====
const showWeightDialog = ref(false)
const savingWeight = ref(false)
const latestWeight = ref(null)
const weightRecords = ref([])   // 完整体重记录（含 id，供历史列表编辑/删除）
const editingWeightId = ref(null)

// ---- 进度条仪表盘 ----
const gaugeTarget = computed(() => user.value.targetWeightKg)

const gaugePercent = computed(() => {
  if (latestWeight.value === null) return 0
  const s = user.value.weightKg, t = user.value.targetWeightKg
  if (!s || !t || s === t) return 0
  return Math.min(Math.max(Math.abs(s - latestWeight.value) / Math.abs(s - t), 0), 1)
})

const totalDelta = computed(() => {
  if (latestWeight.value === null) return 0
  const s = user.value.weightKg
  if (!s) return 0
  return Math.round((s - latestWeight.value) * 10) / 10
})

const weightForm = reactive({ weightKg: null, recordedDate: selectedDate.value, bodyFatPct: null, waistCm: null, hipCm: null })

function resetWeightForm() {
  editingWeightId.value = null
  // 优先取选中日期已记录的数据，否则取最近一次
  const dayRec = weightRecords.value.find(r => r.recordedDate === selectedDate.value)
  const fallback = dayRec ?? weightRecords.value[0]
  weightForm.weightKg = fallback?.weightKg ?? null
  weightForm.recordedDate = selectedDate.value
  weightForm.bodyFatPct = fallback?.bodyFatPct ?? null
  weightForm.waistCm = fallback?.waistCm ?? null
  weightForm.hipCm = fallback?.hipCm ?? null
}

// 禁止选择未来日期和超过3天前的日期
function disableFutureDate(date) {
  const now = Date.now()
  const threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  return dayStart > now || dayStart < new Date(threeDaysAgo).setHours(0, 0, 0, 0)
}

async function loadWeight() {
  try {
    const res = await api.get(`/weights/latest/${user.value.id}`)
    latestWeight.value = res.weightKg
  } catch (e) { /* 无记录 */ }
}

async function handleWeightSave() {
  if (!weightForm.weightKg) { ElMessage.warning('请输入体重'); return }
  savingWeight.value = true
  try {
    if (editingWeightId.value) {
      await api.put(`/weights/record/${editingWeightId.value}`, weightForm)
    } else {
      await api.post(`/weights/${user.value.id}`, weightForm)
    }
    ElMessage.success(editingWeightId.value ? '体重已更新' : '体重已记录')
    showWeightDialog.value = false
    editingWeightId.value = null
    weightForm.weightKg = null
    weightForm.recordedDate = selectedDate.value
    // 体重变化 → 后端已按最新体重重算目标，同步最新 user 并刷新目标相关图表
    try {
      const latest = await api.get(`/users/${user.value.id}/profile`)
      localStorage.setItem('user', JSON.stringify(latest))
      user.value = latest
      targetCalories.value = latest.dailyCalorieTarget || 2000
    } catch (e) { /* 静默，下次进页面自动同步 */ }
    loadWeight()
    loadWeightHistory()
    loadCalorieTrend()
    loadNutritionData()
    loadCalendarData()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '记录失败'
    ElMessage.error(msg)
  } finally { savingWeight.value = false }
}


// ===== 饮食概览 =====
const todayTotal = ref(0)
const targetCalories = ref(user.value.dailyCalorieTarget || 2000)
const meals = ref([
  { type: 'BREAKFAST', label: '早餐', icon: '🌅', calories: 0, items: [] },
  { type: 'LUNCH', label: '午餐', icon: '🌞', calories: 0, items: [] },
  { type: 'DINNER', label: '晚餐', icon: '🌙', calories: 0, items: [] },
  { type: 'SNACK', label: '加餐', icon: '🍪', calories: 0, items: [] }
])
// 剩余 = 目标 + 运动消耗 - 已吃（运动会增加可吃额度）
const remaining = computed(() => targetCalories.value + exerciseTotal.value * 0.9 - todayTotal.value)
const calPercent = computed(() => {
  const effective = targetCalories.value + exerciseTotal.value * 0.9
  if (effective <= 0) return 0
  return Math.min((todayTotal.value / effective) * 100, 120)
})

async function loadToday() {
  const seq = dateSeq
  try {
    const res = await api.get(`/records/summary/${user.value.id}?date=${selectedDate.value}`)
    if (seq !== dateSeq) return
    todayTotal.value = res.totalCalories || 0
    meals.value.forEach(meal => {
      meal.items = res.meals[meal.type] || []
      meal.calories = meal.items.reduce((sum, item) => sum + item.totalCalories, 0)
    })
  } catch (e) { /* 无记录 */ }
}

// ===== 跳转食物选择 =====
function openMealPicker(mealType) {
  if (!checkDateEditable()) return
  router.push({ path: '/food-library', query: { mealType, mealDate: selectedDate.value } })
}

// ===== 三餐详情弹窗（查看/修改/删除已记录条目） =====
const showMealDialog = ref(false)
const activeMeal = ref(null)
const editingGramsMap = reactive({})
const savingRecordId = ref(null)
const deletingRecordId = ref(null)
const copyingMeal = ref(false)

function openMealDetail(meal) {
  activeMeal.value = meal
  // 清空并预填当前克数
  Object.keys(editingGramsMap).forEach(k => delete editingGramsMap[k])
  meal.items.forEach(rec => { editingGramsMap[rec.id] = rec.grams })
  showMealDialog.value = true
}

function addFoodToActiveMeal() {
  if (!checkDateEditable()) return
  showMealDialog.value = false
  openMealPicker(activeMeal.value.type)
}

async function copyYesterdayMeal() {
  if (!activeMeal.value) return
  if (!checkDateEditable()) { copyingMeal.value = false; return }
  const mealType = activeMeal.value.type
  copyingMeal.value = true
  try {
    const res = await api.get(`/records/${user.value.id}/yesterday?mealType=${mealType}`)
    if (!res || res.length === 0) {
      ElMessage.info(`昨天${activeMeal.value.label}没有记录，无需复制`)
      return
    }
    const requests = res.map(r => ({
      foodId: r.foodId,
      grams: r.grams,
      mealType: mealType,
      mealDate: selectedDate.value
    }))
    await api.post(`/records/batch/${user.value.id}`, requests)
    ElMessage.success(`已复制昨天${activeMeal.value.label}（${res.length} 种食物）`)
    await refreshMealData()
    showMealDialog.value = false
  } catch (e) {
    ElMessage.error('复制失败')
  } finally { copyingMeal.value = false }
}

async function handleRecordSave(rec) {
  if (!checkDateEditable()) { savingRecordId.value = null; return }
  const grams = editingGramsMap[rec.id]
  if (!grams || grams <= 0) { ElMessage.warning('请输入有效克数'); return }
  savingRecordId.value = rec.id
  try {
    await api.put(`/records/${rec.id}?grams=${grams}`)
    ElMessage.success('克数已更新')
    await refreshMealData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '保存失败')
  } finally { savingRecordId.value = null }
}

async function handleRecordDelete(rec) {
  if (!checkDateEditable()) { deletingRecordId.value = null; return }
  try {
    await ElMessageBox.confirm(`确定删除「${rec.foodName}」这条记录吗？`, '删除确认', { type: 'warning' })
  } catch (e) { deletingRecordId.value = null; return }
  deletingRecordId.value = rec.id
  try {
    await api.delete(`/records/${rec.id}`)
    ElMessage.success(`已删除「${rec.foodName}」`)
    await refreshMealData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '删除失败')
  } finally { deletingRecordId.value = null }
}

// 增删改后刷新今日三餐汇总、营养素与热量趋势
async function refreshMealData() {
  await loadToday()
  await loadNutritionData()
  await loadCalorieTrend()
  await loadCalendarData()
  await loadWater()
  await loadSleep()
}

// ===== 运动记录 =====
const exerciseRecords = ref([])
const exerciseTotal = ref(0)

async function loadExercise() {
  const seq = dateSeq
  try {
    const res = await api.get(`/exercises/${user.value.id}/summary?date=${selectedDate.value}`)
    if (seq !== dateSeq) return
    exerciseRecords.value = res.records || []
    exerciseTotal.value = res.totalCalories || 0
  } catch (e) {
    if (seq !== dateSeq) return
    exerciseRecords.value = []; exerciseTotal.value = 0
  }
}

async function deleteExercise(rec) {
  if (!checkDateEditable()) return
  try {
    await ElMessageBox.confirm('确定删除这条运动记录吗？', '删除确认', { type: 'warning' })
  } catch (e) { return }
  try {
    await api.delete(`/exercises/record/${rec.id}`)
    ElMessage.success('运动记录已删除')
    loadExercise()
    loadNutritionData()
    loadCalendarData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ===== 喝水记录 =====
const waterTargetMl = ref(parseInt(localStorage.getItem('waterTarget')) || 2000)
const showWaterDialog = ref(false)
const savingWater = ref(false)
const waterRecords = ref([])
const waterTotalMl = ref(0)
const waterForm = reactive({ amountMl: 250 })
const waterTrendChartRef = ref(null)
const waterTrend = ref({})
let waterTrendChart = null
const showWaterDetail = ref(false)

const waterPercent = computed(() => {
  if (waterTargetMl.value <= 0) return 0
  return Math.round((waterTotalMl.value / waterTargetMl.value) * 100)
})

// 喝水目标设置
const waterTargetInput = ref(waterTargetMl.value)
function saveWaterTarget() {
  waterTargetMl.value = waterTargetInput.value
  localStorage.setItem('waterTarget', String(waterTargetInput.value))
  ElMessage.success('喝水目标设置成功')
  showWaterDetail.value = false
}

async function loadWater() {
  const seq = dateSeq
  try {
    const [res, trend] = await Promise.all([
      api.get(`/water/${user.value.id}/summary?date=${selectedDate.value}`),
      api.get(`/water/${user.value.id}/trend?endDate=${selectedDate.value}&days=7`)
    ])
    if (seq !== dateSeq) return
    waterRecords.value = res.records || []
    waterTotalMl.value = res.totalMl || 0
    waterTrend.value = trend
  } catch (e) {
    if (seq !== dateSeq) return
    waterRecords.value = []; waterTotalMl.value = 0
  }
}

async function addWater(ml) {
  if (!checkDateEditable()) return
  try {
    await api.post(`/water/${user.value.id}?date=${selectedDate.value}&amountMl=${ml}`)
    loadWater()
  } catch (e) {
    ElMessage.error('记录失败')
  }
}

async function handleWaterSave() {
  if (!waterForm.amountMl || waterForm.amountMl <= 0) { ElMessage.warning('请输入水量'); return }
  if (!checkDateEditable()) { savingWater.value = false; return }
  savingWater.value = true
  try {
    await api.post(`/water/${user.value.id}?date=${selectedDate.value}&amountMl=${waterForm.amountMl}`)
    ElMessage.success('已记录喝水')
    showWaterDialog.value = false
    waterForm.amountMl = 250
    loadWater()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '记录失败')
  } finally { savingWater.value = false }
}

async function deleteWaterRecord(rec) {
  try {
    await api.delete(`/water/record/${rec.id}`)
    ElMessage.success('已删除喝水记录')
    loadWater()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ===== 睡眠记录 =====
const sleepTargetMin = ref(parseInt(localStorage.getItem('sleepTarget')) || 480)
const sleepTargetLabel = computed(() => {
  const h = Math.floor(sleepTargetMin.value / 60)
  const m = sleepTargetMin.value % 60
  return m === 0 ? `${h} 小时` : `${h} 小时 ${m} 分`
})

// 睡眠目标设置（在详情弹窗内联编辑）
const sleepTargetInput = ref(sleepTargetMin.value)
function saveSleepTarget() {
  if (!sleepTargetInput.value || sleepTargetInput.value < 60) { ElMessage.warning('请填写有效的睡眠时长'); return }
  sleepTargetMin.value = sleepTargetInput.value
  localStorage.setItem('sleepTarget', String(sleepTargetInput.value))
  ElMessage.success('睡眠目标已更新')
  showSleepDetail.value = false
}

const showSleepDialog = ref(false)
const savingSleep = ref(false)
const deletingSleep = ref(false)
const editingSleepId = ref(null)
const sleepRecord = ref(null)
const sleepForm = reactive({ durationMin: 480 })
const sleepTrendChartRef = ref(null)
const sleepTrend = ref({})
let sleepTrendChart = null
const showSleepDetail = ref(false)

function formatSleepMin(min) {
  if (!min) return '0 小时'
  const h = Math.floor(min / 60)
  const m = min % 60
  return m === 0 ? `${h} 小时` : `${h} 小时 ${m} 分`
}

async function loadSleep() {
  const seq = dateSeq
  try {
    const [res, trend] = await Promise.all([
      api.get(`/sleep/${user.value.id}/summary?date=${selectedDate.value}`),
      api.get(`/sleep/${user.value.id}/trend?endDate=${selectedDate.value}&days=7`)
    ])
    if (seq !== dateSeq) return
    sleepRecord.value = res.record || null
    sleepTrend.value = trend
  } catch (e) {
    if (seq !== dateSeq) return
    sleepRecord.value = null
  }
}

function resetSleepForm() {
  editingSleepId.value = null
  sleepForm.durationMin = 480
}

function openSleepDialog() {
  if (sleepRecord.value) {
    editingSleepId.value = sleepRecord.value.id
    sleepForm.durationMin = sleepRecord.value.durationMin
  } else {
    editingSleepId.value = null
    sleepForm.durationMin = 480
  }
  showSleepDialog.value = true
}

async function handleSleepSave() {
  if (!sleepForm.durationMin || sleepForm.durationMin <= 0) { ElMessage.warning('请输入睡眠时长'); return }
  if (!checkDateEditable()) { savingSleep.value = false; return }
  savingSleep.value = true
  try {
    await api.post(`/sleep/${user.value.id}?date=${selectedDate.value}&durationMin=${sleepForm.durationMin}`)
    ElMessage.success('睡眠已记录')
    showSleepDialog.value = false
    loadSleep()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '保存失败')
  } finally { savingSleep.value = false }
}

async function handleSleepDelete() {
  if (!editingSleepId.value) return
  deletingSleep.value = true
  try {
    await api.delete(`/sleep/record/${editingSleepId.value}`)
    ElMessage.success('睡眠记录已删除')
    showSleepDialog.value = false
    editingSleepId.value = null
    loadSleep()
  } catch (e) {
    ElMessage.error('删除失败')
  } finally { deletingSleep.value = false }
}

// ===== 喝水/睡眠 趋势小图 =====
function renderWaterTrendChart() {
  if (!waterTrendChartRef.value) return
  if (!waterTrendChart) waterTrendChart = echarts.getInstanceByDom(waterTrendChartRef.value) || echarts.init(waterTrendChartRef.value)
  const filled = fillTrend(waterTrend.value, selectedDate.value, 7)
  const dates = filled.map(e => e.date.slice(5))
  const values = filled.map(e => Math.round(e.value))
  waterTrendChart.setOption({
    grid: { top: 4, right: 8, bottom: 14, left: 36 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 9, color: '#9ca3af', rotate: 0 }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', name: 'ml', splitLine: { show: false }, axisLabel: { fontSize: 9, color: '#9ca3af' }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#34d399', borderRadius: [3, 3, 0, 0] }, barMaxWidth: 14 }]
  }, { notMerge: true })
}

function renderSleepTrendChart() {
  if (!sleepTrendChartRef.value) return
  if (!sleepTrendChart) sleepTrendChart = echarts.getInstanceByDom(sleepTrendChartRef.value) || echarts.init(sleepTrendChartRef.value)
  const filled = fillTrend(sleepTrend.value, selectedDate.value, 7)
  const dates = filled.map(e => e.date.slice(5))
  const values = filled.map(e => Math.round(e.value / 60))
  sleepTrendChart.setOption({
    grid: { top: 4, right: 8, bottom: 14, left: 32 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 9, color: '#9ca3af' }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', name: 'h', splitLine: { show: false }, axisLabel: { fontSize: 9, color: '#9ca3af' }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#818cf8', borderRadius: [3, 3, 0, 0] }, barMaxWidth: 14 }]
  }, { notMerge: true })
}

// ===== 日期导航 =====

function formatDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function goToPrevDay() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() - 1)
  selectedDate.value = formatDate(d)
}

function goToNextDay() {
  if (!canGoNext.value) return
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() + 1)
  selectedDate.value = formatDate(d)
}

function goToToday() {
  selectedDate.value = actualToday.value
}

// ===== 日历热力图 =====

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

async function loadCalendarData() {
  const seq = calendarSeq
  const year = calendarYear.value
  const month = calendarMonth.value
  const lastDay = new Date(year, month, 0).getDate()
  const lastOfMonth = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  const endDate = lastOfMonth > actualToday.value ? actualToday.value : lastOfMonth
  const daysDiff = Math.ceil((new Date(endDate) - new Date(`${year}-${String(month).padStart(2, '0')}-01`)) / (1000 * 60 * 60 * 24)) + 1
  try {
    const [intakeRes, todayExRes, exTrendRes] = await Promise.all([
      api.get(`/records/${user.value.id}/trend?endDate=${endDate}&days=${daysDiff}`),
      api.get(`/exercises/${user.value.id}/summary?date=${actualToday.value}`),
      api.get(`/exercises/${user.value.id}/trend?endDate=${endDate}&days=${daysDiff}`)
    ])
    if (seq !== calendarSeq) return
    calendarData.value = intakeRes
    exerciseTrendData.value = exTrendRes
    todayExerciseCal.value = todayExRes.totalCalories || 0
    await nextTick()
    if (seq !== calendarSeq) return
    renderCalendarChart()
  } catch (e) {
    if (seq !== calendarSeq) return
    calendarData.value = {}
  }
}

function renderCalendarChart() {
  if (!calendarChartRef.value) return
  if (!calendarChart) calendarChart = echarts.getInstanceByDom(calendarChartRef.value) || echarts.init(calendarChartRef.value)
  const isDark = document.documentElement.dataset.theme === 'dark'

  const year = calendarYear.value
  const month = calendarMonth.value
  const lastDay = new Date(year, month, 0).getDate()
  const firstOfMonth = `${year}-${String(month).padStart(2, '0')}-01`
  const lastOfMonth = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  const target = user.value.dailyCalorieTarget || 2000
  const data = []

  for (let day = 1; day <= lastDay; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (dateStr > actualToday.value) {
      data.push([dateStr, 0, 'future'])
      continue
    }
    const cals = calendarData.value[dateStr]
    if (cals !== undefined && cals !== null) {
      // 当天：用有效目标（含运动）归一化，使 visualMap 阈值仍然适用
      if (dateStr === actualToday.value && todayExerciseCal.value > 0) {
        const effectiveTarget = target + todayExerciseCal.value
        const normalized = Math.round(cals / effectiveTarget * target)
        data.push([dateStr, normalized, cals, todayExerciseCal.value])
      } else {
        data.push([dateStr, Math.round(cals)])
      }
    } else {
      data.push([dateStr, 0])
    }
  }

  calendarChart.off('click')
  calendarChart.on('click', (params) => {
    if (params.value && params.value[0] && params.value[0] <= actualToday.value) {
      selectedDate.value = params.value[0]
    }
  })

  calendarChart.setOption({
    tooltip: {
      position: 'top',
      formatter: function (p) {
        if (!p.value) return ''
        const dateStr = p.value[0]
        if (dateStr > actualToday.value) {
          const d = new Date(dateStr)
          return `${d.getMonth() + 1}月${d.getDate()}日<br/>未来的日期`
        }
        const d = new Date(dateStr)
        const m = d.getMonth() + 1
        const day = d.getDate()
        const cals = p.value[1]
        const rawIntake = p.value[2] || 0
        const exercise = p.value[3] || 0
        const exCal = parseFloat(exerciseTrendData.value[dateStr]) || 0
        const effectiveTarget = target + (exCal || exercise) * 0.9
        // 无饮食：显示运动消耗（如果有）
        if (cals === 0 && rawIntake === 0) {
          if (exCal > 0) return `${m}月${day}日<br/>无饮食记录<br/>🏃 运动消耗 ${Math.round(exCal)} 千卡<br/><small>点击跳转到该日</small>`
          return `${m}月${day}日<br/>无记录<br/><small>点击跳转到该日</small>`
        }
        const totalEx = exCal || exercise
        if (totalEx > 0) {
          const pct = Math.round((rawIntake / effectiveTarget) * 100)
          const status = pct <= 80 ? '✅ 低于目标' : pct <= 100 ? '⚠️ 接近目标' : '❌ 超出目标'
          return `${m}月${day}日<br/>摄入 ${Math.round(rawIntake)} 千卡<br/>🏃 运动消耗 ${Math.round(totalEx)} 千卡<br/>有效目标 ${Math.round(effectiveTarget)} 千卡 (${pct}%)<br/>${status}<br/><small>点击跳转到该日</small>`
        }
        const pct = Math.round((cals / target) * 100)
        const status = pct <= 80 ? '✅ 低于目标' : pct <= 100 ? '⚠️ 接近目标' : '❌ 超出目标'
        return `${m}月${day}日<br/>摄入 ${cals} 千卡 (${pct}%)<br/>${status}<br/><small>点击跳转到该日</small>`
      },
      backgroundColor: isDark ? '#1e293b' : '#fff', borderColor: isDark ? '#334155' : '#e5e7eb',
      textStyle: { color: isDark ? '#e2e8f0' : '#374151', fontSize: 11 },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-radius: 6px; padding: 3px 8px; line-height: 1.4;'
    },
    visualMap: {
      min: 0, max: target * 1.2,
      type: 'piecewise', orient: 'horizontal', left: 'center', bottom: 0,
      hoverLink: true,
      pieces: [
        { lt: 1, color: isDark ? '#1e293b' : '#e5e7eb', label: '无记录' },
        { min: 1, lte: target * 0.8, color: '#10b981', label: '低于目标' },
        { gt: target * 0.8, lte: target, color: '#f59e0b', label: '接近目标' },
        { gt: target, color: '#ef4444', label: '超出目标' }
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
  }, { notMerge: true })
}

// ===== 日期切换时刷新 =====
// 请求竞态守卫：切日期序号 +1，旧批次响应晚到直接丢弃（load 内部用 seq 对比）
let dateSeq = 0
let calendarSeq = 0
watch(selectedDate, (val) => {
  sessionStorage.setItem('home_selectedDate', val)
  dateSeq++
  loadToday()
  loadExercise()
  loadNutritionData()
  loadCalorieTrend()
  loadWeightHistory()
  loadWater()
  loadSleep()
  weightForm.recordedDate = val
})

watch(showWaterDetail, async (v) => {
  if (v) {
    waterTargetInput.value = waterTargetMl.value
    await nextTick(); renderWaterTrendChart()
  }
})
watch(showSleepDetail, async (v) => {
  if (v) {
    sleepTargetInput.value = sleepTargetMin.value
    await nextTick(); renderSleepTrendChart()
  }
})

watch([calendarMonth, calendarYear], () => {
  calendarSeq++
  loadCalendarData()
})

// ===== 图表数据加载 & 渲染 =====

async function loadWeightHistory() {
  const seq = dateSeq
  try {
    const [res, records] = await Promise.all([
      api.get(`/weights/history/${user.value.id}?days=30`),
      api.get(`/weights/${user.value.id}`)
    ])
    if (seq !== dateSeq) return
    weightHistory.value = res
    weightRecords.value = records
    // 从历史数据中查找 selectedDate 当天的体重
    const dayRecord = res.find(p => p.date === selectedDate.value)
    if (dayRecord) {
      latestWeight.value = dayRecord.weightKg
    } else {
      // 该日期无体重记录，保持 null（显示空状态）
      latestWeight.value = null
    }
    await nextTick()
    if (seq !== dateSeq) return
    renderWeightChart()
  } catch (e) { /* 无记录 */ }
}

async function loadCalorieTrend() {
  const seq = dateSeq
  try {
    const endDate = selectedDate.value
    const [intakeRes, exTrendRes] = await Promise.all([
      api.get(`/records/${user.value.id}/trend?endDate=${endDate}&days=7`),
      api.get(`/exercises/${user.value.id}/trend?endDate=${endDate}&days=7`)
    ])
    if (seq !== dateSeq) return
    calorieTrend.value = intakeRes
    calorieExerciseTrend.value = exTrendRes
    await nextTick()
    if (seq !== dateSeq) return
    renderCalorieChart()
  } catch (e) { /* 无记录 */ }
}

async function loadNutritionData() {
  const seq = dateSeq
  try {
    const res = await api.get(`/records/nutrition/${user.value.id}?date=${selectedDate.value}`)
    if (seq !== dateSeq) return
    nutritionData.value = res
    await nextTick()
    if (seq !== dateSeq) return
    renderNutritionChart()
  } catch (e) { /* 无记录 */ }
}

function renderWeightChart() {
  if (!weightChartRef.value || weightHistory.value.length < 2) return
  // 防止 HMR 热更新后同一 DOM 上重复 init 报错：已有实例则复用
  if (!weightChart) weightChart = echarts.getInstanceByDom(weightChartRef.value) || echarts.init(weightChartRef.value)
  const dates = weightHistory.value.map(p => p.date)
  const values = weightHistory.value.map(p => p.weightKg)
  const startW = user.value.weightKg
  const targetW = user.value.targetWeightKg

  const dataMin = Math.min(...values)
  const dataMax = Math.max(...values)
  const dataRange = dataMax - dataMin
  const targetRange = Math.abs((startW || dataMax) - (targetW || dataMin))

  // 智能自适应：数据跨度 < 目标跨度 40% → 聚焦数据（斜率陡）；否则展开覆盖全区间
  const focusMode = targetRange > 0 && dataRange < targetRange * 0.4

  let yMin, yMax
  const markLines = []
  const graphics = []

  if (focusMode) {
    // 聚焦模式：Y 轴只看已记录体重范围，动态 padding
    const pad = Math.max(dataRange * 0.35, 0.3)
    yMin = dataMin - pad
    yMax = dataMax + pad
    // 目标线：范围内画虚线，出界用文字标注
    if (targetW && targetW >= yMin && targetW <= yMax) {
      markLines.push({ yAxis: targetW, name: '目标', lineStyle: { color: '#f59e0b', type: 'dashed' }, label: { color: '#f59e0b', formatter: '目标 {c}kg', position: 'insideStartTop', distance: 6 } })
    } else if (targetW && targetW < yMin) {
      graphics.push({ type: 'text', left: 8, bottom: 2, style: { text: `目标 ${targetW}kg ↓`, fill: '#f59e0b', fontSize: 11, fontWeight: 600 } })
    }
    // 起始线：范围内画无标签虚线
    if (startW && startW >= yMin && startW <= yMax) {
      markLines.push({ yAxis: startW, name: '起始', lineStyle: { color: '#9ca3af', type: 'dashed' }, label: { show: false } })
    }
    // 起始体重标注统一放在右下角
    if (startW) {
      graphics.push({ type: 'text', right: 8, bottom: 2, style: { text: `起始 ${startW}kg`, fill: '#9ca3af', fontSize: 11, fontWeight: 500 } })
    }
  } else {
    // 展开模式：覆盖起始到目标全区间
    yMin = Math.min(dataMin, targetW || dataMin) - 1
    yMax = Math.max(dataMax, startW || dataMax) + 1
    markLines.push(
      { yAxis: startW, name: '起始', lineStyle: { color: '#9ca3af', type: 'dashed', width: 1.5 }, label: { show: false } },
      { yAxis: targetW, name: '目标', lineStyle: { color: '#f59e0b', type: 'dashed', width: 1.5 }, label: { fontSize: 10, color: '#f59e0b', formatter: '目标 {c}kg', position: 'insideStartTop', distance: 6 } }
    )
    // 起始体重标注放在右下角，不遮挡刻度
    if (startW) {
      graphics.push({ type: 'text', right: 8, bottom: 2, style: { text: `起始 ${startW}kg`, fill: '#9ca3af', fontSize: 11, fontWeight: 500 } })
    }
  }

  // 体脂副线（右侧 Y 轴）：仅当有 >=2 个体脂数据点时才显示
  const bfRaw = weightHistory.value.map(p => p.bodyFatPct ?? null)
  const bfValid = bfRaw.filter(v => v != null)
  const hasBodyFat = bfValid.length >= 2
  let bfMin, bfMax, bfInterval
  if (hasBodyFat) {
    const lo = Math.min(...bfValid), hi = Math.max(...bfValid)
    const range = hi - lo || 2
    // 取整到美观的刻度步长
    const step = range <= 1 ? 0.5 : range <= 3 ? 1 : range <= 6 ? 2 : 5
    bfMin = Math.max(0, Math.floor((lo - step * 0.5) / step) * step)
    bfMax = Math.ceil((hi + step * 0.5) / step) * step
    bfInterval = step
  }

  weightChart.setOption({
    grid: { top: 24, right: hasBodyFat ? 52 : 16, bottom: 44, left: 56 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, color: '#9ca3af' }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: 'kg', min: yMin, max: yMax, splitNumber: 5, splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { fontSize: 10, color: '#9ca3af', margin: 8 }, axisTick: { show: false }, axisLine: { show: false } },
      ...(hasBodyFat ? [{ type: 'value', name: '体脂%', min: bfMin, max: bfMax, interval: bfInterval, splitNumber: 4, position: 'right', splitLine: { show: false }, axisLabel: { fontSize: 10, color: '#818cf8', margin: 8 }, nameTextStyle: { color: '#818cf8', fontSize: 10 }, axisTick: { show: false }, axisLine: { show: false } }] : [])
    ],
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const arr = Array.isArray(params) ? params : [params]
        return arr.map(p => {
          const unit = p.seriesName === '体脂' ? '%' : 'kg'
          const v = p.value == null ? '—' : `${p.value}${unit}`
          return `${p.marker}${p.seriesName}：${v}`
        }).join('<br/>')
      },
      backgroundColor: '#fff', borderColor: '#e5e7eb', textStyle: { color: '#374151', fontSize: 13 }
    },
    graphic: graphics,
    series: [
      { name: '体重', type: 'line', data: values, smooth: true, lineStyle: { color: '#10b981', width: 2.5 }, itemStyle: { color: '#10b981' }, symbol: 'circle', symbolSize: 5, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0)' }]) }, markLine: { silent: true, symbol: 'none', lineStyle: { type: 'dashed', color: '#d1d5db', width: 1.5 }, label: { fontSize: 10, color: '#9ca3af' }, data: markLines } },
      ...(hasBodyFat ? [{ name: '体脂', type: 'line', yAxisIndex: 1, data: bfRaw, connectNulls: true, smooth: true, lineStyle: { color: '#818cf8', width: 2 }, itemStyle: { color: '#818cf8' }, symbol: 'circle', symbolSize: 4, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(129,140,248,0.15)' }, { offset: 1, color: 'rgba(129,140,248,0)' }]) } }] : [])
    ]
  }, { notMerge: true })
}

function renderCalorieChart() {
  if (!calorieChartRef.value || Object.keys(calorieTrend.value).length === 0) return
  if (!calorieChart) calorieChart = echarts.getInstanceByDom(calorieChartRef.value) || echarts.init(calorieChartRef.value)
  const filled = fillTrend(calorieTrend.value, selectedDate.value, 7)
  const exFilled = fillTrend(calorieExerciseTrend.value, selectedDate.value, 7)
  const dates = filled.map(e => e.date.slice(5)) // MM-DD
  const target = Math.round(user.value.dailyCalorieTarget || 2000)
  // 净摄入 = 饮食摄入 − 运动消耗 × 0.9（与热量总览计算逻辑一致）
  const netValues = filled.map((e, i) => Math.round(e.value - (exFilled[i]?.value || 0) * 0.9))
  const rawValues = filled.map(e => Math.round(e.value))
  const exValues = exFilled.map(e => Math.round(e.value || 0))

  calorieChart.setOption({
    grid: { top: 12, right: 16, bottom: 24, left: 44 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, color: '#9ca3af' }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '千卡', splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { fontSize: 10, color: '#9ca3af' } },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff', borderColor: '#e5e7eb', textStyle: { color: '#374151', fontSize: 13 },
      formatter: (params) => {
        const idx = params[0]?.dataIndex ?? 0
        const raw = rawValues[idx] || 0
        const ex = exValues[idx] || 0
        const net = netValues[idx] || 0
        const pct = target > 0 ? Math.round((net / target) * 100) : 0
        let status = pct <= 80 ? '✅ 低于目标' : pct <= 100 ? '⚠️ 接近目标' : '❌ 超出目标'
        let lines = [`摄入 ${raw} 千卡`]
        if (ex > 0) lines.push(`🏃 运动消耗 ${ex} 千卡`)
        lines.push(`净摄入 ${net} 千卡 (${pct}%)`)
        lines.push(status)
        return lines.join('<br/>')
      }
    },
    series: [
      {
        type: 'bar',
        data: netValues.map(v => {
          const isOver = v > target
          const c = isOver ? '#ef4444' : '#10b981'
          return { value: v, itemStyle: { color: c, borderRadius: [6, 6, 0, 0] }, emphasis: { itemStyle: { color: c } } }
        }),
        barMaxWidth: 26,
        markLine: { silent: true, symbol: 'none', lineStyle: { type: 'dashed', color: '#f59e0b', width: 1.5 }, label: { fontSize: 10, color: '#f59e0b', formatter: '目标 ' + target }, data: [{ yAxis: target }] }
      }
    ]
  }, { notMerge: true })
}

function renderNutritionChart() {
  if (!nutritionChartRef.value || !hasNutritionTarget.value) return
  if (!nutritionChart) nutritionChart = echarts.getInstanceByDom(nutritionChartRef.value) || echarts.init(nutritionChartRef.value)
  const d = nutritionData.value
  const calPerGram = { protein: 4, fat: 9, carbs: 4 }
  const actual = { protein: d.protein || 0, fat: d.fat || 0, carbs: d.carbs || 0 }
  const target = { protein: d.proteinTarget || 0, fat: d.fatTarget || 0, carbs: d.carbsTarget || 0 }
  const max = { protein: d.proteinMax, fat: d.fatMax, carbs: d.carbsMax }

  // 只画有推荐目标的营养素
  const keys = macroKeys.filter(k => target[k] > 0)
  const totalTargetCal = keys.reduce((s, k) => s + target[k] * calPerGram[k], 0)

  // 灰色环按营养素比例分成若干段（每段 = 该营养素的推荐量）
  // 每个营养素 = 1 段彩色进度 + 1 段灰色轨道，成对排列（彩色在前，从轨道起点增长）
  // 每组后插入透明间隔段，拉开轨道距离以区分营养素种类（含最后一组，闭合 12 点方向）
  const data = []
  keys.forEach(k => {
    const share = totalTargetCal > 0 ? (target[k] * calPerGram[k]) / totalTargetCal : 0
    const fill = Math.min(actual[k] / target[k], 1)
    const colored = Math.round(share * fill * 1000) / 10
    const gray = Math.round(share * (1 - fill) * 1000) / 10
    if (colored > 0.05) {
      data.push({ name: `${macroNames[k]} ${Math.round(actual[k])}/${Math.round(target[k])}g`, value: colored, itemStyle: { color: macroColors[k] } })
    }
    if (gray > 0.05) {
      data.push({ name: '', value: gray, itemStyle: { color: '#e5e7eb' }, label: { show: false }, tooltip: { show: false }, emphasis: { scale: false } })
    }
    data.push({ name: '', value: 2, itemStyle: { color: 'transparent', borderWidth: 0 }, label: { show: false }, tooltip: { show: false }, emphasis: { scale: false } })
  })

  // 超量提示：超最大 → 红；超推荐 → 黄
  const warnings = []
  keys.forEach(k => {
    if (max[k] != null && actual[k] > max[k]) {
      warnings.push({ text: `${macroNames[k]}已超出当日最大摄入 ${Math.round(max[k])}g`, color: '#ef4444' })
    } else if (actual[k] > target[k]) {
      warnings.push({ text: `${macroNames[k]}已超出当日推荐摄入 ${Math.round(target[k])}g`, color: '#f59e0b' })
    }
  })
  nutritionWarnings.value = warnings

  const calTarget = Math.round(d.calorieTarget || 0)
  const calActual = Math.round(d.totalCalories || 0)

  nutritionChart.setOption({
    series: [{
      type: 'pie', radius: ['55%', '78%'], center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, position: 'outside', formatter: '{b}', fontSize: 11, color: '#6b7280', lineHeight: 16 },
      emphasis: { scaleSize: 8 },
      data
    }],
    graphic: [{
      type: 'text', left: 'center', top: 'center',
      style: { text: `${calActual} / ${calTarget}\n千卡`, textAlign: 'center', fill: '#374151', fontSize: 15, fontWeight: 700, lineHeight: 20 }
    }]
  }, { notMerge: true })
}

function handleResize() {
  weightChart?.resize()
  calorieChart?.resize()
  nutritionChart?.resize()
  calendarChart?.resize()
  waterTrendChart?.resize()
  sleepTrendChart?.resize()
}

onMounted(() => {
  // 每次进入页面刷新 user 数据（避免 EditProfile 修改后数据过期）
  const u = JSON.parse(localStorage.getItem('user') || '{}')
  user.value = u
  targetCalories.value = u.dailyCalorieTarget || 2000
  loadWeight()
  loadToday()
  nextTick(() => loadExercise())
  // 图表
  loadWeightHistory()
  loadCalorieTrend()
  loadNutritionData()
  loadCalendarData()
  loadWater()
  loadSleep()
  window.addEventListener('resize', handleResize)
  // 首次加载的快捷操作
  if (route.query.action) handleQuickAction(route.query.action)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  weightChart?.dispose()
  calorieChart?.dispose()
  nutritionChart?.dispose()
  calendarChart?.dispose()
  waterTrendChart?.dispose()
  sleepTrendChart?.dispose()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: transparent;
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 16px 100px;
  animation: page-fade-in 0.4s ease;
}

/* ===== 日历热力图 ===== */
.calendar-card .card-header {
  flex-wrap: wrap; gap: 6px;
}
.calendar-day-nav {
  display: flex; align-items: center; gap: 4px;
  margin-left: auto;
}
.day-nav-btn {
  width: 28px; height: 28px;
  border: none; border-radius: 50%;
  background: var(--color-primary-bg-light);
  color: var(--color-primary);
  font-size: 11px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.day-nav-btn:hover { background: var(--color-primary-bg); }
.day-nav-btn.disabled { color: var(--color-border); background: var(--color-border-light); cursor: not-allowed; }
.day-nav-today {
  padding: 3px 10px;
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--color-primary);
  font-size: 11px; font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}
.day-nav-today:hover { background: var(--color-primary); color: var(--color-text-inverse); }

.calendar-month-row {
  display: flex; align-items: center; justify-content: center; gap: 10px;
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
.calendar-chart-box {
  height: 240px;
}
.calendar-hint {
  margin-top: 4px;
  text-align: center;
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow-x: auto;
  padding-bottom: 2px;
}

/* ===== 卡片通用 ===== */
.weight-card, .calorie-card, .exercise-card, .meals-card {
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  padding: 20px;
  margin-bottom: var(--space-md);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.weight-card:hover, .calorie-card:hover, .exercise-card:hover, .meals-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.card-header {
  display: flex; align-items: center; gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.card-icon { font-size: 20px; }
.card-title { font-weight: 700; color: var(--color-text-secondary); flex: 1; font-size: 17px; letter-spacing: 0.04em; }
.card-date { font-size: 13px; color: var(--color-text-muted); }
.report-link {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-bg-light);
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.report-link:hover { background: var(--color-primary-bg); }

/* ===== 体重进度条 ===== */
.gauge-section { text-align: center; }

.gauge-empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--space-sm);
  padding: 30px 0 16px; color: var(--color-text-muted); font-size: var(--text-sm);
}
.empty-icon { font-size: 40px; }

.gauge-bar-wrap { padding: 0 4px; }

.gauge-bar-labels {
  display: flex; justify-content: space-between;
  font-size: 13px; margin-bottom: 12px;
}
.label-start { color: var(--color-text-muted); }
.label-target { color: var(--color-accent); font-weight: 600; }

.gauge-bar-track {
  position: relative; height: 18px;
  background: var(--color-border); border-radius: var(--radius-full);
  overflow: visible;
}
.gauge-bar-fill {
  height: 100%; border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-primary-lighter), var(--color-primary));
  transition: width 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}
.gauge-bar-fill::after {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.25), transparent);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}
.gauge-bar-dot {
  position: absolute; top: -3px;
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--color-primary);
  border: 3px solid var(--color-white);
  box-shadow: 0 2px 10px rgba(5,150,105,0.45);
  transform: translateX(-50%);
  transition: left 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.gauge-bar-info {
  display: flex; align-items: baseline; justify-content: center; gap: 12px;
  margin-top: 16px; margin-bottom: 8px;
}
.gauge-num {
  font-size: var(--text-4xl); font-weight: 800;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
.gauge-num small { font-size: var(--text-base); color: var(--color-text-muted); font-weight: 500; }
.gauge-delta { font-size: var(--text-sm); font-weight: 600; }
.gauge-delta.down { color: var(--color-primary); }
.gauge-delta.up { color: var(--color-danger-light); }
.gauge-progress-text { font-size: var(--text-sm); color: var(--color-text-muted); font-weight: 500; }
.record-btn {
  width: 100%; height: 42px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: var(--color-text-inverse); border: none; font-weight: 600;
  box-shadow: 0 4px 14px rgba(5,150,105,0.22);
  transition: all var(--transition-spring);
}
.record-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(5,150,105,0.32);
}

/* ===== 体重卡片提示 ===== */
.weight-hint {
  margin-top: 12px;
  text-align: center;
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

/* ===== 体成分三栏输入 ===== */
.metric-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0 0 8px 2px;
}
.body-metric-row {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; width: 100%;
}
.metric-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  background: var(--color-glass-strong);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
}
.metric-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 600;
  text-align: left;
  padding-left: 2px;
}

/* ===== 热量总览（方案A：大数字+进度条+底部三行） ===== */
.remaining-hero {
  text-align: center; padding: 8px 0 16px;
}
.remaining-num {
  font-size: 52px; font-weight: 800;
  color: var(--color-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.remaining-num.over {
  color: var(--color-danger);
}
.remaining-label {
  display: block; margin-top: 6px;
  font-size: var(--text-sm); color: var(--color-text-muted);
}

.cal-bar-wrapper { margin-bottom: 14px; }
.cal-bar { height: 10px; background: var(--color-border); border-radius: var(--radius-full); overflow: hidden; }
.cal-fill {
  height: 100%; border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-primary-lighter), var(--color-primary));
  transition: width 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}
.cal-fill::after {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.25), transparent);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}
.cal-fill.over {
  background: linear-gradient(90deg, var(--color-danger-light), var(--color-danger));
}
.cal-fill.over::after { display: none; }

.cal-details {
  display: flex; justify-content: space-around;
  padding-top: 4px;
}
.cal-detail-item {
  text-align: center;
}
.detail-label {
  display: block; font-size: 12px; color: var(--color-text-muted);
  margin-bottom: 2px;
}
.detail-label .hint {
  color: var(--color-text-muted);
  opacity: 0.65;
  font-weight: 400;
}
.detail-val {
  font-size: 17px; font-weight: 700; color: var(--color-text);
  font-variant-numeric: tabular-nums;
}
.detail-val.out { color: var(--color-accent); }

/* ===== 运动记录 ===== */
.exercise-empty {
  text-align: center; padding: 24px 0 16px;
}
.empty-text { color: var(--color-text-muted); font-size: var(--text-sm); }

.exercise-summary { margin-bottom: 14px; }
.exercise-total {
  display: flex; align-items: baseline; justify-content: center; gap: 8px;
  margin-bottom: 14px;
}
.exercise-big-num {
  font-size: var(--text-4xl); font-weight: 800;
  color: var(--color-accent);
  font-variant-numeric: tabular-nums;
}
.exercise-unit { font-size: var(--text-sm); color: var(--color-text-muted); }

.exercise-list { margin-bottom: 4px; }
.exercise-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0; border-bottom: 1px solid var(--color-border-light);
}
.exercise-row:last-child { border: none; }
.ex-type { font-size: var(--text-sm); font-weight: 600; color: var(--color-text); flex: 1; }
.ex-detail { font-size: 12px; color: var(--color-text-muted); }
.ex-cal { font-size: var(--text-sm); font-weight: 600; color: var(--color-accent); }
.ex-edit, .ex-del {
  font-size: 16px; color: var(--color-text-muted); cursor: pointer;
  padding: 4px 6px; border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  opacity: 0;
}
.exercise-row:hover .ex-edit, .exercise-row:hover .ex-del { opacity: 1; }
.ex-edit:hover { color: var(--color-primary); background: var(--color-primary-bg-light); }
.ex-del { margin-left: 2px; }
.ex-del:hover { color: var(--color-danger); }

.exercise-btn {
  background: linear-gradient(135deg, #f97316, #ea580c) !important;
  box-shadow: 0 4px 14px rgba(234,88,12,0.25) !important;
}
.exercise-btn:hover {
  box-shadow: 0 6px 20px rgba(234,88,12,0.38) !important;
}

/* ===== 三餐 ===== */
.meal-row {
  display: flex; align-items: center; gap: 10px;
  padding: 15px 0; border-bottom: 1px solid var(--color-border-light);
  cursor: pointer; border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}
.meal-row:last-child { border: none; }
.meal-row:hover { background: var(--color-primary-bg-light); margin: 0 -12px; padding: 15px 12px; border-radius: var(--radius-md); }
.meal-row-icon { font-size: 22px; }
.meal-row-name { font-weight: 600; color: var(--color-text); flex: 1; }
.meal-row-cal { font-size: var(--text-sm); color: var(--color-primary); font-weight: 600; }
.meal-row-arrow { font-size: 20px; color: var(--color-border); transition: transform var(--transition-fast); }
.meal-row:hover .meal-row-arrow { transform: translateX(3px); color: var(--color-primary); }

/* ===== 图表卡片 ===== */
.chart-card {
  background: var(--color-glass);
  border-radius: var(--radius-xl);
  padding: 20px;
  margin-bottom: var(--space-md);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.chart-box {
  height: 220px;
  width: 100%;
}
.chart-box-sm {
  height: 260px;
}
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
.trend-stat {
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--color-primary-bg-light);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  text-align: center;
}
.nutrition-legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
}
.nutrition-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
.nutrition-warnings {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.nutrition-warning {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

/* ===== 三餐详情弹窗 ===== */
.meal-detail-empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--space-sm);
  padding: 30px 0; color: var(--color-text-muted); font-size: var(--text-sm);
}
.meal-detail-list { max-height: 50vh; overflow-y: auto; }
.meal-detail-row {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  padding: 12px 0; border-bottom: 1px solid var(--color-border-light);
}
.meal-detail-row:last-child { border: none; }
.meal-detail-info { flex: 1; min-width: 0; }
.meal-detail-name { font-weight: 600; font-size: 15px; color: var(--color-text); display: block; }
.meal-detail-cal { font-size: 12px; color: var(--color-primary); font-weight: 600; }
.meal-detail-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.grams-unit { font-size: 13px; color: var(--color-text-muted); font-weight: 500; }
.meal-detail-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 12px; padding-top: 12px; border-top: 2px dashed var(--color-border);
}
.meal-detail-footer-left {
  display: flex; align-items: center; gap: 8px;
}
.meal-detail-add-btn {
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: var(--color-text-inverse); border: none; font-weight: 600;
  box-shadow: 0 4px 14px rgba(5,150,105,0.22);
  transition: all var(--transition-spring);
}
.meal-detail-add-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(5,150,105,0.32); }
.meal-detail-copy-btn {
  border-radius: var(--radius-md);
  background: var(--color-glass-strong);
  color: var(--color-primary);
  border: 1.5px dashed var(--color-primary);
  font-weight: 600; font-size: 13px;
  transition: all var(--transition-fast);
}
.meal-detail-copy-btn:hover {
  background: var(--color-primary-bg-light);
  border-color: var(--color-primary);
}
.meal-detail-total { font-size: 15px; font-weight: 700; color: var(--color-text); }

/* ===== 喝水 + 睡眠 同行双栏 ===== */
.water-sleep-row { display: flex; gap: 12px; margin-bottom: var(--space-md); }
.ws-card {
  flex: 1; min-width: 0;
  background: var(--color-glass); border-radius: var(--radius-xl);
  padding: 16px; box-shadow: var(--shadow-sm);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.ws-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.ws-card .card-header { margin-bottom: 10px; }
.ws-card .card-title { font-size: var(--text-sm); }
.ws-card .record-btn { height: 36px; font-size: 13px; }
.ws-card .exercise-empty { padding: 12px 0 8px; }
.ws-card .exercise-empty .empty-text { font-size: 12px; }

/* 详情入口链接 */
.card-detail-link {
  font-size: 12px; color: var(--color-primary); cursor: pointer;
  white-space: nowrap; transition: color var(--transition-fast);
}
.card-detail-link:hover { color: var(--color-primary-dark); }

/* 可编辑的目标文字 */
.target-editable {
  border-bottom: 1px dashed var(--color-text-muted); cursor: pointer;
  transition: border-color var(--transition-fast);
}
.target-editable:hover { border-color: var(--color-primary); color: var(--color-primary); }

/* 详情弹窗 */
.detail-chart-title { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px; }
.detail-list-title { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 6px; }
.detail-list { max-height: 240px; overflow-y: auto; }
.detail-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 4px; border-bottom: 1px solid var(--color-border-light);
}
.detail-time { flex: 1; font-size: 13px; color: var(--color-text-muted); }
.detail-val { font-size: 13px; font-weight: 600; color: var(--color-text); }
.detail-del { font-size: 14px; cursor: pointer; opacity: 0.5; transition: opacity var(--transition-fast); }
.detail-del:hover { opacity: 1; }

/* 详情页内联设置 */
.detail-setting {
  display: flex; align-items: center; gap: 8px;
  margin-top: 14px; padding: 10px 12px;
  background: var(--color-glass-strong); border-radius: var(--radius-md);
}
.detail-setting-label { font-size: 13px; color: var(--color-text-secondary); white-space: nowrap; }
.detail-setting-unit { font-size: 13px; color: var(--color-text-muted); }

/* ===== 喝水 ===== */
.water-overview {
  display: flex; align-items: baseline; justify-content: center; gap: 6px;
  padding: 10px 0 12px;
}
.water-big-num {
  font-size: var(--text-4xl); font-weight: 800; color: #3b82f6;
  font-variant-numeric: tabular-nums;
}
.water-unit { font-size: var(--text-sm); color: var(--color-text-muted); }
.water-bar-track {
  height: 12px; background: var(--color-border); border-radius: var(--radius-full); overflow: hidden;
}
.water-bar-fill {
  height: 100%; border-radius: var(--radius-full);
  background: linear-gradient(90deg, #93c5fd, #3b82f6);
  transition: width 0.6s ease;
}
.water-actions {
  display: flex; justify-content: center; gap: 8px; margin-top: 12px;
}
.water-add-btn {
  border-radius: var(--radius-md);
  background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe;
  font-weight: 600;
}
.water-add-btn:hover { background: #dbeafe; }
.water-custom-btn {
  border-radius: var(--radius-md);
  color: var(--color-text-muted); border: 1px dashed var(--color-border);
}
.water-list { margin-top: 10px; max-height: 96px; overflow-y: auto; }
.water-more {
  font-size: 12px; color: var(--color-primary); cursor: pointer;
  padding: 4px 0; text-align: center;
}
.water-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; border-bottom: 1px dashed var(--color-border-light);
  font-size: 13px;
}
.water-row-time { color: var(--color-text-muted); flex: 1; }
.water-row-ml { font-weight: 600; color: #2563eb; }
.water-row-del { cursor: pointer; color: var(--color-text-muted); padding: 2px 4px; }
.water-row-del:hover { color: var(--color-danger); }

/* ===== 睡眠 ===== */
.sleep-overview {
  display: flex; align-items: baseline; justify-content: center; gap: 10px;
  padding: 14px 0;
}
.sleep-big-num {
  font-size: var(--text-3xl); font-weight: 800; color: #7c3aed;
  font-variant-numeric: tabular-nums;
}
.sleep-target { font-size: var(--text-sm); color: var(--color-text-muted); }

/* ===== 快速添加 FAB（右下角悬浮按钮） ===== */
.fab-btn {
  position: fixed;
  right: 18px;
  bottom: 16px;
  width: 54px; height: 54px; border: none; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: var(--color-text-inverse); font-size: 30px; font-weight: 600;
  box-shadow: 0 6px 20px rgba(5,150,105,0.4);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  z-index: 90;
  transition: all var(--transition-spring);
}
.fab-btn:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 26px rgba(5,150,105,0.5);
}
.fab-btn:active { transform: scale(0.94); }

/* 快捷操作浮层 */
.quick-actions {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
}
.quick-action {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 16px 8px;
  background: var(--color-primary-bg-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.quick-action:hover { background: var(--color-primary-bg); transform: translateY(-2px); }
.quick-icon { font-size: 26px; }
.quick-action span:last-child { font-size: 13px; font-weight: 600; color: var(--color-text); }

</style>