<template>
  <div class="exr-page">
    <!-- 左侧边栏：功能标签 + 虚线 + 运动分类（仅文字） -->
    <div class="side-bar">
      <div class="side-tab" :class="{ active: activeTab === 'mine' }" @click="switchTab('mine')">
        <span class="tab-label">我的</span>
      </div>
      <div class="side-tab" :class="{ active: activeTab === 'custom' }" @click="switchTab('custom')">
        <span class="tab-label">自定义</span>
      </div>
      <div class="side-tab" :class="{ active: activeTab === 'hot' }" @click="switchTab('hot')">
        <span class="tab-label">热门</span>
      </div>
      <!-- 虚线分隔 -->
      <div class="side-divider"></div>
      <!-- 运动分类 -->
      <div
        v-for="g in exerciseGroups"
        :key="g.label"
        class="side-tab"
        :class="{ active: activeTab === 'cat' && activeCat === g.label }"
        @click="switchCategory(g.label)"
      >
        <span class="tab-label">{{ g.label.replace(/^\S+\s+/, '') }}</span>
      </div>
    </div>

    <!-- 右侧面板 -->
    <div class="exr-panel">
      <div class="panel-title">
        <span class="picker-back" @click="router.back()">←</span>
        {{ panelTitle }}
      </div>

      <div v-if="loadingRecent" class="loading-text">加载中...</div>
      <div v-else-if="filteredExercises.length === 0" class="empty-text">
        {{ emptyText }}
      </div>
      <div v-else class="exr-list">
        <div
          v-for="ex in filteredExercises"
          :key="ex.name"
          class="exr-card"
          :class="{ selected: inCart(ex.name) }"
          @click="openFillDialog(ex)"
        >
          <div class="food-check">
            <div class="check-circle" :class="{ checked: inCart(ex.name) }">
              <span v-if="inCart(ex.name)" class="check-mark">✓</span>
            </div>
          </div>
          <div class="exr-info">
            <span class="exr-name">{{ getExerciseIcon(ex.name) }} {{ ex.name }}</span>
            <span class="exr-met">{{ ex.met }} MET</span>
            <span class="exr-kcal">≈{{ ex.kcal60 }}千卡/小时</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 填写时长弹窗 -->
    <el-dialog v-model="fillVisible" :title="fillTitle" width="88%" :close-on-click-modal="false" align-center>
      <div class="fill-form">
        <div class="fill-name">{{ getExerciseIcon(fillForm.name) }} {{ fillForm.name }}</div>
        <div class="fill-meta">{{ fillForm.met }} MET · ≈{{ calKcal60(fillForm.met) }} 千卡/小时</div>
        <div class="fill-input-row">
          <el-input-number v-model="fillForm.durationMin" :min="1" :max="480" :step="5" size="large" controls-position="right" style="width: 160px" />
          <span class="fill-unit">分钟</span>
        </div>
        <div class="fill-preview" v-if="fillForm.durationMin > 0">
          预估消耗 <strong>≈ {{ fillPreviewCal }} 千卡</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="fillVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmFill">确认加入</el-button>
      </template>
    </el-dialog>

    <!-- 底部悬浮条 -->
    <div class="picker-bar">
      <div class="picker-left" @click="openCart">
        <span class="cart-icon">🛒</span>
        <span v-if="cart.length > 0" class="cart-badge">{{ cart.length }}</span>
        <span class="picker-count">已选 {{ cart.length }} 项</span>
      </div>
      <el-button type="primary" :disabled="cart.length === 0" @click="handleSubmit" :loading="submitting" class="picker-confirm-btn">确认选择</el-button>
    </div>

    <!-- 上滑购物车面板 -->
    <div v-if="cartVisible" class="sheet-mask" @click="cartVisible = false"></div>
    <transition name="sheet-up">
      <div v-if="cartVisible" class="sheet-panel">
        <div class="sheet-handle" @click="cartVisible = false">▾</div>
        <div class="cart-header">
          <span class="cart-title">已选购物车<em v-if="cart.length"> ({{ cart.length }})</em></span>
          <span v-if="cartTotal > 0" class="cart-total">合计 ≈{{ cartTotal }} 千卡</span>
        </div>
        <div v-if="cart.length === 0" class="cart-empty">还没有添加任何运动</div>
        <div v-else class="cart-list">
          <div v-for="item in cart" :key="item.name" class="cart-item">
            <span class="cart-item-name">{{ getExerciseIcon(item.name) }} {{ item.name }}</span>
            <span class="cart-item-dur">{{ item.durationMin }} 分钟</span>
            <span class="cart-item-cal">≈{{ item.calories }} 千卡</span>
            <span class="cart-item-edit" title="修改时长" @click="editCartItem(item)">✎</span>
            <span class="cart-item-del" title="移除" @click="removeCartItem(item.name)">✕</span>
          </div>
        </div>
        <div class="cart-footer">
          <span v-if="cartTotal > 0" class="cart-footer-total">合计 ≈{{ cartTotal }} 千卡</span>
          <el-button type="primary" :disabled="cart.length === 0" @click="handleSubmit" :loading="submitting" class="cart-confirm-btn">确认选择</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { exerciseGroups, metMap, getExerciseIcon, HOT_EXERCISES, calKcal60 } from '@/data/exerciseCatalog'
import { useToday } from '@/composables/useToday'

const router = useRouter()
const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const { today } = useToday()

const RECENT_TYPES_KEY = 'exercise_recent_types'

// 编辑模式：query 带 recordId/type/dur，预填该运动
const editingId = route.query.recordId ? Number(route.query.recordId) : null
const selectedDate = ref(route.query.date || today.value)

// 左侧边栏状态
const activeTab = ref('hot')
const activeCat = ref(null)

// 「我的」最近运动类型
const recentTypes = ref([])
const loadingRecent = ref(false)

// 填写弹窗状态
const fillVisible = ref(false)
const fillForm = reactive({ name: '', met: 5, durationMin: 30, recordId: null })
const submitting = ref(false)

// 购物车 + 上滑面板
const cart = ref([])   // { name, met, durationMin, calories, recordId? }
const cartVisible = ref(false)

function openCart() {
  if (cart.value.length === 0) return
  cartVisible.value = true
}

const panelTitle = computed(() => {
  if (activeTab.value === 'mine') return '我的运动'
  if (activeTab.value === 'custom') return '自定义运动'
  if (activeTab.value === 'hot') return '热门运动'
  const g = exerciseGroups.find(x => x.label === activeCat.value)
  return g ? `${g.label.replace(/^\S+\s+/, '')}运动` : '选择运动'
})

const emptyText = computed(() => {
  if (activeTab.value === 'custom') return '暂无自定义运动（功能预留）'
  if (activeTab.value === 'mine') return '还没有运动记录，先记录一次吧 💪'
  if (activeTab.value === 'hot') return '暂无热门运动'
  return '该分类暂无运动'
})

const filteredExercises = computed(() => {
  if (activeTab.value === 'mine') {
    return recentTypes.value.map(name => {
      const met = metMap[name] || 5
      return { name, met, kcal60: calKcal60(met) }
    })
  }
  if (activeTab.value === 'custom') return []
  if (activeTab.value === 'hot') {
    return HOT_EXERCISES.map(name => {
      const met = metMap[name] || 5
      return { name, met, kcal60: calKcal60(met) }
    })
  }
  const group = exerciseGroups.find(g => g.label === activeCat.value)
  return group ? group.items : []
})

function switchTab(key) {
  activeTab.value = key
}

function switchCategory(label) {
  activeTab.value = 'cat'
  activeCat.value = label
}

// ===== 购物车管理 =====
function inCart(name) {
  return cart.value.some(i => i.name === name)
}

function openFillDialog(ex, editingItem = null) {
  if (editingItem) {
    fillForm.name = editingItem.name
    fillForm.met = editingItem.met
    fillForm.durationMin = editingItem.durationMin
    fillForm.recordId = editingItem.recordId
  } else {
    fillForm.name = ex.name
    fillForm.met = ex.met
    fillForm.durationMin = inCart(ex.name) ? (cart.value.find(i => i.name === ex.name) || {}).durationMin || 30 : 30
    fillForm.recordId = null
  }
  fillVisible.value = true
}

function confirmFill() {
  const dur = fillForm.durationMin
  if (!dur || dur <= 0) { ElMessage.warning('请填写运动时长'); return }
  const met = metMap[fillForm.name] || fillForm.met || 5
  const calories = Math.round(met * (user.weightKg || 65) * (dur / 60) * 1.05 * 10) / 10
  const idx = cart.value.findIndex(i => i.name === fillForm.name)
  if (idx >= 0) {
    cart.value[idx] = { ...cart.value[idx], durationMin: dur, calories, recordId: fillForm.recordId || cart.value[idx].recordId }
  } else {
    cart.value.push({ name: fillForm.name, met: fillForm.met, durationMin: dur, calories, recordId: fillForm.recordId })
  }
  fillVisible.value = false
}

function editCartItem(item) {
  openFillDialog(null, item)
}

function removeCartItem(name) {
  cart.value = cart.value.filter(i => i.name !== name)
}

const fillTitle = computed(() => fillForm.recordId ? '修改运动时长' : '填写运动时长')
const fillPreviewCal = computed(() => {
  const met = metMap[fillForm.name] || fillForm.met || 5
  const dur = fillForm.durationMin || 0
  return Math.round(met * (user.weightKg || 65) * (dur / 60) * 1.05 * 10) / 10
})

const cartTotal = computed(() =>
  cart.value.reduce((sum, item) => sum + (item.calories || 0), 0)
)

// ===== 提交 =====
async function handleSubmit() {
  // 3 天限制（与 Home.vue checkDateEditable 一致）
  const ago = new Date(today.value)
  ago.setDate(ago.getDate() - 3)
  const minDate = `${ago.getFullYear()}-${String(ago.getMonth() + 1).padStart(2, '0')}-${String(ago.getDate()).padStart(2, '0')}`
  if (selectedDate.value < minDate) { ElMessage.warning('只能修改三天内的记录'); return }

  submitting.value = true
  const weight = user.weightKg || 65
  const recordDate = selectedDate.value
  try {
    await Promise.all(cart.value.map(item => {
      const payload = {
        exerciseType: item.name,
        durationMin: item.durationMin,
        metValue: metMap[item.name] || item.met || 5,
        recordDate
      }
      if (item.recordId) return api.put(`/exercises/record/${item.recordId}?weightKg=${weight}`, payload)
      return api.post(`/exercises/${user.id}?weightKg=${weight}`, payload)
    }))
    saveRecentTypes(cart.value.map(i => i.name))
    ElMessage.success(editingId ? '运动记录已更新' : `已添加 ${cart.value.length} 项运动`)
    router.back()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

// ===== 「我的」最近类型 =====
function loadRecentTypes() {
  try { return JSON.parse(localStorage.getItem(RECENT_TYPES_KEY) || '[]') } catch (e) { return [] }
}

function saveRecentTypes(types) {
  const merged = [...new Set([...types, ...loadRecentTypes()])].slice(0, 10)
  localStorage.setItem(RECENT_TYPES_KEY, JSON.stringify(merged))
}

onMounted(async () => {
  // 编辑模式：预填该运动进购物车 + 定位到它所在分类
  if (editingId && route.query.type) {
    const name = route.query.type
    const met = metMap[name] || 5
    const dur = Number(route.query.dur) || 30
    const calories = Math.round(met * (user.weightKg || 65) * (dur / 60) * 1.05 * 10) / 10
    cart.value.push({ name, met, durationMin: dur, calories, recordId: editingId })
    const g = exerciseGroups.find(g => g.items.some(i => i.name === name))
    if (g) { activeCat.value = g.label; activeTab.value = 'cat' }
  }
  // 加载最近使用的运动类型（后端优先，本地缓存兜底）
  loadingRecent.value = true
  try {
    const res = await api.get(`/exercises/${user.id}/recent-types`)
    recentTypes.value = res || []
    saveRecentTypes(recentTypes.value)
  } catch (e) {
    recentTypes.value = loadRecentTypes()
  } finally {
    loadingRecent.value = false
  }
})
</script>

<style scoped>
.exr-page {
  display: flex;
  height: 100vh;
  background: transparent;
  animation: fade-in 0.35s ease forwards;
}

/* ===== 左侧边栏（复刻 Foods.vue） ===== */
.side-bar {
  width: 88px;
  min-width: 88px;
  background: var(--color-glass);
  overflow: hidden;
  padding: 6px 0;
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.side-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1 1 0;
  min-height: 0;
  width: 76px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}
.side-tab:hover { background: var(--color-primary-bg-light); }
.side-tab.active { background: var(--color-primary-bg); }
.side-tab.active .tab-label { color: var(--color-primary); font-weight: 600; }
.tab-label { font-size: 13px; color: var(--color-text-muted); white-space: nowrap; }
.side-divider {
  width: 48px;
  margin: 4px auto;
  border-top: 1.5px dashed var(--color-border);
  flex-shrink: 0;
}

/* ===== 右侧面板 ===== */
.exr-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 100px;
}
.panel-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 0.04em;
  margin-bottom: 12px;
  padding-left: 4px;
}
.picker-back {
  display: inline-block;
  margin-right: 6px;
  font-size: 18px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: color 0.2s ease;
}
.picker-back:hover { color: var(--color-primary); }
.loading-text,
.empty-text {
  text-align: center;
  color: var(--color-text-muted);
  padding: 40px 0;
  font-size: 14px;
}

/* ===== 运动卡片列表（行式 + 勾选框） ===== */
.exr-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.exr-card {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: var(--color-glass);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xs);
  cursor: pointer;
  transition: all 0.2s ease;
}
.exr-card:hover {
  background: var(--color-primary-bg-light);
  transform: translateX(3px);
}
.exr-card.selected {
  background: var(--color-primary-bg-light);
  box-shadow: 0 0 0 1px var(--color-primary-lighter);
}
.food-check { margin-right: 12px; flex-shrink: 0; }
.check-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background: var(--color-glass-strong);
}
.check-circle.checked {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.check-mark { color: white; font-size: 13px; font-weight: 700; line-height: 1; }
.exr-info { flex: 1; min-width: 0; }
.exr-name { font-weight: 600; color: var(--color-text); font-size: 15px; }
.exr-met { font-size: 13px; color: var(--color-text-muted); margin-left: 8px; }
.exr-kcal { font-size: 12px; color: var(--color-primary); margin-left: 8px; font-weight: 600; }

/* ===== 底部悬浮条（无 NavBar，left 覆盖内容区） ===== */
.picker-bar {
  position: fixed;
  bottom: 0;
  left: 88px;
  right: 0;
  height: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--color-border-light);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 60;
  animation: picker-bar-in 0.3s ease;
}
@keyframes picker-bar-in {
  from { transform: translateY(100%); }
  to   { transform: translateY(0); }
}
.picker-left { display: flex; align-items: center; gap: 8px; cursor: pointer; position: relative; padding: 8px 0; }
.cart-icon { font-size: 20px; line-height: 1; }
.cart-badge {
  position: absolute;
  top: 2px;
  left: 14px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
}
.picker-count { font-size: 15px; font-weight: 600; color: var(--color-text); }
.picker-confirm-btn {
  height: 38px !important;
  border-radius: 10px !important;
  padding: 0 24px !important;
  font-weight: 600 !important;
  font-size: 15px !important;
}

/* ===== 上滑购物车面板 ===== */
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 90;
  animation: fade-in 0.2s ease;
}
.sheet-panel {
  position: fixed;
  bottom: 0;
  left: 88px;
  right: 0;
  z-index: 95;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  border-radius: 16px 16px 0 0;
  border-top: 1px solid var(--color-border-light);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.12);
  max-height: 60vh;
  display: flex;
  flex-direction: column;
}
.sheet-handle {
  text-align: center;
  padding: 6px 0 2px;
  font-size: 18px;
  color: var(--color-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}
.sheet-up-enter-active { transition: transform 0.25s ease; }
.sheet-up-leave-active { transition: transform 0.2s ease; }
.sheet-up-enter-from, .sheet-up-leave-to { transform: translateY(100%); }
.sheet-up-enter-to, .sheet-up-leave-from { transform: translateY(0); }
.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}
.cart-title { font-size: 14px; font-weight: 700; color: var(--color-text); letter-spacing: 0.04em; }
.cart-title em { font-style: normal; color: var(--color-primary); }
.cart-total { font-size: 13px; color: var(--color-primary); font-weight: 600; }
.cart-empty { padding: 24px 16px; font-size: 13px; color: var(--color-text-muted); text-align: center; }
.cart-list { overflow-y: auto; padding: 4px 16px; flex: 1; }
.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
}
.cart-item:last-child { border-bottom: none; }
.cart-item-name { flex: 1; min-width: 0; font-size: 14px; font-weight: 600; color: var(--color-text); }
.cart-item-dur { font-size: 13px; color: var(--color-text-secondary); flex-shrink: 0; }
.cart-item-cal { font-size: 13px; color: var(--color-primary); font-weight: 600; flex-shrink: 0; }
.cart-item-edit, .cart-item-del {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;
  user-select: none;
}
.cart-item-edit { background: var(--color-primary-bg-light); color: var(--color-primary); }
.cart-item-edit:hover { background: var(--color-primary-bg); }
.cart-item-del { background: #fee2e2; color: #dc2626; }
.cart-item-del:hover { background: #fecaca; }
.cart-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.cart-footer-total { font-size: 15px; font-weight: 700; color: var(--color-primary); }
.cart-confirm-btn {
  height: 38px !important;
  border-radius: 10px !important;
  padding: 0 28px !important;
  font-weight: 600 !important;
  font-size: 15px !important;
}

/* ===== 填写时长弹窗 ===== */
.fill-form { text-align: center; padding: 8px 0 4px; }
.fill-name { font-size: 18px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.fill-meta { font-size: 13px; color: var(--color-text-muted); margin-bottom: 18px; }
.fill-input-row { display: flex; align-items: center; justify-content: center; gap: 8px; }
.fill-unit { font-size: 15px; color: var(--color-text-secondary); font-weight: 600; }
.fill-preview { margin-top: 16px; font-size: 14px; color: var(--color-text-secondary); }
.fill-preview strong { color: var(--color-primary); font-size: 16px; }
</style>
