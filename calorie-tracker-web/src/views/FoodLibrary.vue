<template>
  <div class="library-page">
    <!-- 顶部：返回 + 搜索框 -->
    <div class="lib-header">
      <span class="lib-back" @click="router.back()">←</span>
      <el-input
        v-model="keyword"
        class="lib-search"
        placeholder="搜索全部食物，如：鸡胸肉、苹果、米饭"
        size="large"
        clearable
        :prefix-icon="'🔍'"
        @input="onSearchInput"
      />
    </div>

    <!-- 统计行 -->
    <div class="lib-meta">
      <span v-if="isPickerMode">为{{ mealLabel }}选择食物 · 点击食物填写克数，加入购物车</span>
      <span v-else>{{ keyword ? `搜索"${keyword}"的结果` : '全部食物' }}</span>
      <span v-if="total > 0">共 {{ total }} 种</span>
    </div>

    <!-- 食物列表 -->
    <div v-loading="loading && items.length === 0" class="lib-list">
      <div v-if="!loading && items.length === 0" class="lib-empty">
        {{ keyword ? '没有找到相关食物' : '暂无食物' }}
      </div>

      <!-- 每行：行 + 展开详情 -->
      <div v-for="food in items" :key="food.id" class="food-row-wrap">
        <div
          class="food-row"
          :class="{ selected: isPickerMode && inCart(food.id), expanded: expandedId === food.id }"
          @click="isPickerMode ? openFillDialog(food) : toggleExpand(food.id)"
        >
          <!-- 选餐模式：圆圈标识（已在购物车） -->
          <div v-if="isPickerMode" class="food-check">
            <div class="check-circle" :class="{ checked: inCart(food.id) }">
              <span v-if="inCart(food.id)" class="check-mark">✓</span>
            </div>
          </div>
          <div class="food-info">
            <div class="food-name">{{ food.name }}</div>
            <div class="food-meta">{{ food.caloriesPer100g }} 千卡/100g</div>
          </div>
          <div class="food-actions">
            <span
              v-if="!isPickerMode"
              class="lib-fav"
              :class="{ favorited: isFavorited(food.id) }"
              @click.stop="toggleFavorite(food)"
              title="收藏"
            >
              {{ isFavorited(food.id) ? '❤️' : '🤍' }}
            </span>
            <span
              class="food-expand"
              :class="{ active: expandedId === food.id }"
              @click.stop="toggleExpand(food.id)"
              title="查看营养详情"
            >›</span>
          </div>
        </div>

        <!-- 营养详情面板 -->
        <div v-if="expandedId === food.id" class="food-detail">
          <div class="nutrition-row">
            <span class="nutrition-item">
              <span class="nut-label">蛋白质</span>
              <span class="nut-value">{{ food.proteinPer100g || 0 }}<small>g</small></span>
            </span>
            <span class="nutrition-item">
              <span class="nut-label">脂肪</span>
              <span class="nut-value">{{ food.fatPer100g || 0 }}<small>g</small></span>
            </span>
            <span class="nutrition-item">
              <span class="nut-label">碳水</span>
              <span class="nut-value">{{ food.carbsPer100g || 0 }}<small>g</small></span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多状态 -->
    <div v-if="loadingMore" class="lib-loading-more">加载中...</div>
    <div v-else-if="hasMore" class="lib-load-more" @click="loadMore">点击加载更多</div>
    <div v-else-if="items.length > 0" class="lib-end">— 已经到底啦 —</div>

    <!-- 选餐模式：填写克数弹窗 -->
    <el-dialog v-model="fillVisible" title="填写食物克数" width="88%" :close-on-click-modal="false" align-center>
      <div class="fill-form">
        <div class="fill-name">{{ fillForm.name }}</div>
        <div class="fill-meta">{{ fillForm.caloriesPer100g }} 千卡/100g</div>
        <div class="fill-input-row">
          <el-input-number v-model="fillForm.grams" :min="1" :max="2000" :step="10" size="large" controls-position="right" style="width: 160px" />
          <span class="fill-unit">克</span>
        </div>
        <div class="fill-preview" v-if="fillForm.grams > 0">
          预估热量 <strong>≈ {{ fillPreviewCal }} 千卡</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="fillVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmFill">确认加入</el-button>
      </template>
    </el-dialog>

    <!-- 选餐模式：底部悬浮条 -->
    <div v-if="isPickerMode" class="picker-bar">
      <div class="picker-left" @click="openCart">
        <span class="cart-icon">🛒</span>
        <span v-if="cart.length > 0" class="cart-badge">{{ cart.length }}</span>
        <span class="picker-count">已选 {{ cart.length }} 项</span>
      </div>
      <el-button type="primary" :disabled="cart.length === 0" @click="handleBatchSubmit" :loading="submitting" class="picker-confirm-btn">确认选择</el-button>
    </div>

    <!-- 选餐模式：上滑购物车面板 -->
    <div v-if="cartVisible" class="sheet-mask" @click="cartVisible = false"></div>
    <transition name="sheet-up">
      <div v-if="cartVisible" class="sheet-panel">
        <div class="sheet-handle" @click="cartVisible = false">▾</div>
        <div class="cart-header">
          <span class="cart-title">已选购物车<em v-if="cart.length"> ({{ cart.length }})</em></span>
          <span v-if="cartTotal > 0" class="cart-total">合计 {{ cartTotal }} 千卡</span>
        </div>
        <div v-if="cart.length === 0" class="cart-empty">还没有添加任何食物</div>
        <div v-else class="cart-list">
          <div v-for="item in cart" :key="item.id" class="cart-item">
            <span class="cart-item-name">{{ item.name }}</span>
            <span class="cart-item-dur">{{ item.grams }} 克</span>
            <span class="cart-item-cal">{{ item.calories }} 千卡</span>
            <span class="cart-item-edit" title="修改克数" @click="editCartItem(item)">✎</span>
            <span class="cart-item-del" title="移除" @click="removeCartItem(item.id)">✕</span>
          </div>
        </div>
        <div class="cart-footer">
          <span v-if="cartTotal > 0" class="cart-footer-total">合计 {{ cartTotal }} 千卡</span>
          <el-button type="primary" :disabled="cart.length === 0" @click="handleBatchSubmit" :loading="submitting" class="cart-confirm-btn">确认选择</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const keyword = ref('')
const items = ref([])
const total = ref(0)
const page = ref(0)
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const expandedId = ref(null)

const favoriteIds = ref(new Set())

const PAGE_SIZE = 50

const route = useRoute()

// ===== 选餐（Picker）模式 =====
const isPickerMode = computed(() => !!route.query.mealType)
const mealType = computed(() => route.query.mealType || null)
const mealLabel = computed(() => getMealLabel(mealType.value))
// 填写弹窗状态
const fillVisible = ref(false)
const fillForm = reactive({ id: null, name: '', caloriesPer100g: 0, grams: 100 })
const submitting = ref(false)

// 购物车 + 上滑面板
const cart = ref([])   // { id, name, caloriesPer100g, grams, calories }
const cartVisible = ref(false)

function openCart() {
  if (cart.value.length === 0) return
  cartVisible.value = true
}

function getMealLabel(type) {
  const map = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
  return map[type] || type
}

// ===== 购物车管理 =====
function inCart(foodId) {
  return cart.value.some(f => f.id === foodId)
}

function openFillDialog(food, editingItem = null) {
  if (editingItem) {
    fillForm.id = editingItem.id
    fillForm.name = editingItem.name
    fillForm.caloriesPer100g = editingItem.caloriesPer100g
    fillForm.grams = editingItem.grams
  } else {
    fillForm.id = food.id
    fillForm.name = food.name
    fillForm.caloriesPer100g = food.caloriesPer100g
    fillForm.grams = inCart(food.id) ? (cart.value.find(f => f.id === food.id) || {}).grams || 100 : 100
  }
  fillVisible.value = true
}

function confirmFill() {
  const grams = fillForm.grams
  if (!grams || grams <= 0) { ElMessage.warning('请填写食物克数'); return }
  const calories = Math.round(fillForm.caloriesPer100g * grams / 100)
  const idx = cart.value.findIndex(f => f.id === fillForm.id)
  if (idx >= 0) {
    cart.value[idx] = { ...cart.value[idx], grams, calories }
  } else {
    cart.value.push({ id: fillForm.id, name: fillForm.name, caloriesPer100g: fillForm.caloriesPer100g, grams, calories })
  }
  fillVisible.value = false
}

function editCartItem(item) {
  openFillDialog(null, item)
}

function removeCartItem(id) {
  cart.value = cart.value.filter(f => f.id !== id)
}

const fillPreviewCal = computed(() => {
  const grams = fillForm.grams || 0
  return Math.round(fillForm.caloriesPer100g * grams / 100)
})

const cartTotal = computed(() =>
  cart.value.reduce((sum, item) => sum + (item.calories || 0), 0)
)

async function handleBatchSubmit() {
  const userId = user.id
  const mealDate = route.query.mealDate || (() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })()

  const payload = cart.value.map(item => ({
    foodId: item.id,
    grams: item.grams,
    mealType: mealType.value,
    mealDate,
    note: null
  }))

  submitting.value = true
  try {
    await api.post(`/records/batch/${userId}`, payload)
    ElMessage.success(`已添加 ${payload.length} 项食物到${getMealLabel(mealType.value)}`)
    cart.value = []
    router.back()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}

async function fetchPage(p, replace = false) {
  if (replace) loading.value = true
  else loadingMore.value = true
  try {
    const url = keyword.value.trim()
      ? `/foods/search/page?keyword=${encodeURIComponent(keyword.value.trim())}&page=${p}&size=${PAGE_SIZE}`
      : `/foods/library?page=${p}&size=${PAGE_SIZE}`
    const res = await api.get(url)
    const newItems = res.items || []
    if (replace) items.value = newItems
    else items.value = [...items.value, ...newItems]
    total.value = res.total || 0
    hasMore.value = res.hasMore
    page.value = p
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

let debounceTimer = null
function onSearchInput() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    expandedId.value = null
    fetchPage(0, true)
  }, 400)
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  fetchPage(page.value + 1, false)
}

// 滚动到底自动加载
function onScroll() {
  const doc = document.documentElement
  if (window.innerHeight + window.scrollY >= doc.scrollHeight - 200) {
    loadMore()
  }
}

async function loadFavoriteIds() {
  try {
    const ids = await api.get(`/foods/favorites/ids/${user.id}`)
    favoriteIds.value = new Set(ids)
  } catch (e) { /* ignore */ }
}

function isFavorited(foodId) {
  return favoriteIds.value.has(foodId)
}

async function toggleFavorite(food) {
  const foodId = food.id
  const wasFavorited = favoriteIds.value.has(foodId)
  // 乐观更新
  if (wasFavorited) favoriteIds.value.delete(foodId)
  else favoriteIds.value.add(foodId)
  try {
    if (wasFavorited) {
      await api.delete('/foods/favorite', { params: { userId: user.id, foodId } })
    } else {
      await api.post('/foods/favorite', null, { params: { userId: user.id, foodId } })
    }
  } catch (e) {
    // 回滚
    if (wasFavorited) favoriteIds.value.add(foodId)
    else favoriteIds.value.delete(foodId)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchPage(0, true)
  loadFavoriteIds()
  window.addEventListener('scroll', onScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  clearTimeout(debounceTimer)
})
</script>

<style scoped>
.library-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 16px 90px;
}

/* ===== 顶部搜索 ===== */
.lib-header {
  display: flex;
  align-items: center;
  gap: 10px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-bg, #f7f8f7);
  padding: 8px 0 12px;
}
.lib-back {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  color: var(--color-text, #374151);
}
.lib-search { flex: 1; }
.lib-search :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  background: var(--color-glass-strong);
  box-shadow: var(--shadow-xs);
}
.lib-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-muted, #9ca3af);
  padding: 4px 4px 12px;
}

/* ===== 食物列表 ===== */
.lib-list {
  min-height: 120px;
}
.food-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.food-row-wrap { opacity: 0; animation: fade-in 0.3s ease forwards; }

.food-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: var(--color-glass);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xs);
  cursor: pointer;
  transition: all 0.2s ease;
}
.food-row:hover {
  background: var(--color-primary-bg-light);
  transform: translateX(3px);
}
.food-row:active { transform: scale(0.98); }

.food-info { flex: 1; min-width: 0; }
.food-name {
  font-weight: 600;
  color: var(--color-text);
  font-size: 15px;
}
.food-meta {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 3px;
}

/* 行右侧：收藏 + 展开箭头 */
.food-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.lib-fav {
  font-size: 17px;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s ease;
}
.lib-fav:hover { transform: scale(1.15); }

.food-expand {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-bg-light);
  color: var(--color-primary);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}
.food-expand:hover {
  background: var(--color-primary-bg);
  transform: scale(1.08);
}
.food-expand:active { transform: scale(0.94); }
.food-expand.active {
  transform: rotate(90deg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

/* 展开状态行 */
.food-row.expanded {
  background: var(--color-primary-bg);
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.08);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

/* ===== 营养详情面板 ===== */
.food-detail {
  overflow: hidden;
  animation: detail-slide 0.25s ease;
  background: var(--color-primary-bg-light);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  margin-top: -2px;
}
@keyframes detail-slide {
  from { max-height: 0; opacity: 0; }
  to   { max-height: 80px; opacity: 1; }
}

.nutrition-row {
  display: flex;
  gap: 8px;
  padding: 12px 16px 14px;
}
.nutrition-item {
  flex: 1;
  text-align: center;
  background: var(--color-glass-strong);
  border-radius: var(--radius-sm);
  padding: 8px 6px;
}
.nut-label {
  display: block;
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}
.nut-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}
.nut-value small {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
}

/* ===== 空态 & 加载更多 ===== */
.lib-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 60px 0;
}
.lib-loading-more, .lib-load-more, .lib-end {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  padding: 12px 0;
}
.lib-load-more {
  cursor: pointer;
  color: var(--color-primary);
}

/* ===== 选餐（Picker）模式 ===== */
.food-check {
  margin-right: 12px;
  flex-shrink: 0;
}
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
.check-mark {
  color: white;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}
.food-row.selected {
  background: var(--color-primary-bg-light);
  box-shadow: 0 0 0 1px var(--color-primary-lighter);
}

/* 选餐底部悬浮条（FoodLibrary 无 NavBar/侧栏，全宽） */
.picker-bar {
  position: fixed;
  bottom: 0;
  left: max(0px, calc(50vw - 500px));
  right: max(0px, calc(50vw - 500px));
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
  left: max(0px, calc(50vw - 500px));
  right: max(0px, calc(50vw - 500px));
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

/* ===== 填写克数弹窗 ===== */
.fill-form { text-align: center; padding: 8px 0 4px; }
.fill-name { font-size: 18px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.fill-meta { font-size: 13px; color: var(--color-text-muted); margin-bottom: 18px; }
.fill-input-row { display: flex; align-items: center; justify-content: center; gap: 8px; }
.fill-unit { font-size: 15px; color: var(--color-text-secondary); font-weight: 600; }
.fill-preview { margin-top: 16px; font-size: 14px; color: var(--color-text-secondary); }
.fill-preview strong { color: var(--color-primary); font-size: 16px; }
</style>
