<template>
  <div class="foods-page">
    <!-- 左侧分类导航（全部等分类数据加载完再统一渲染，保证从上往下渐进式动画） -->
    <div class="side-bar">
      <template v-if="categoriesReady">
        <!-- 上部：功能标签 -->
        <div
          v-for="tab in topTabs"
          :key="tab.key"
          class="side-tab"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
        </div>

        <!-- 虚线分隔 -->
        <div class="side-divider"></div>

        <!-- 下部：食物分类 -->
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="side-tab"
          :class="{ active: activeTab === 'category' && activeCatId === cat.id }"
          @click="switchCategory(cat.id)"
        >
          <span class="tab-icon">{{ cat.icon }}</span>
          <span class="tab-label">{{ cat.name }}</span>
        </div>
      </template>
    </div>

    <!-- 右侧食物列表 -->
    <div class="food-panel" :class="{ 'picker-active': isPickerMode }">
      <div class="panel-title">
        <span v-if="isPickerMode" class="picker-back" @click="router.back()">←</span>
        {{ panelTitle }}
      </div>
      <!-- 搜索框：Picker 选餐模式过滤当前列表；普通浏览点击进入全库搜索页 -->
      <div class="panel-search">
        <el-input
          v-if="isPickerMode"
          v-model="searchText"
          placeholder="搜索当前分类内的食物"
          size="small"
          clearable
        />
        <el-input
          v-else
          :model-value="searchText"
          placeholder="搜索全部食物库食物 ›"
          size="small"
          clearable
          readonly
          @click="router.push('/food-library')"
        />
      </div>
      <!-- 创建按钮（仅"自定义" tab 显示） -->
      <div v-if="activeTab === 'custom' && !isPickerMode" class="create-bar">
        <el-button class="create-btn" @click="openCreateDialog">＋ 创建自定义食物</el-button>
      </div>
      <div v-if="loading" class="loading-text">加载中...</div>
      <div v-else-if="filteredFoods.length === 0" class="empty-text">
        {{ searchText ? '没有匹配的食物' : '暂无食物' }}
      </div>
      <div v-else class="food-list">
        <div
          v-for="food in filteredFoods"
          :key="food.id"
          class="food-row-wrap"
        >
          <div
            class="food-row"
            :class="{
              selected: isPickerMode && isSelected(food),
              expanded: expandedIds.has(food.id)
            }"
            @click="isPickerMode ? toggleSelect(food) : toggleExpand(food.id)"
          >
            <!-- Picker 模式：勾选框 -->
            <div v-if="isPickerMode" class="food-check">
              <div class="check-circle" :class="{ checked: isSelected(food) }">
                <span v-if="isSelected(food)" class="check-mark">✓</span>
              </div>
            </div>
            <div class="food-info">
              <div class="food-name-row">
                <div class="food-name">{{ food.name }}</div>
              </div>
              <div class="food-meta">{{ food.caloriesPer100g }} 千卡/100g</div>
            </div>
            <span
              class="food-expand"
              :class="{ active: expandedIds.has(food.id) }"
              @click.stop="toggleExpand(food.id)"
              title="查看营养详情"
            >›</span>
          </div>
          <!-- 营养详情面板 -->
          <div v-if="expandedIds.has(food.id)" class="food-detail">
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
            <!-- 操作按钮行：收藏 + 删除（Picker 模式只展示营养，隐藏操作） -->
            <div class="detail-actions" v-if="!isPickerMode">
              <span
                class="fav-btn"
                :class="{ favorited: isFavorited(food.id) }"
                @click.stop="toggleFavorite(food)"
              >
                <span class="fav-icon">{{ isFavorited(food.id) ? '❤️' : '🤍' }}</span>
                <span class="fav-text">{{ isFavorited(food.id) ? '已收藏' : '收藏' }}</span>
              </span>
              <span
                v-if="activeTab === 'custom'"
                class="delete-btn"
                @click.stop="handleDeleteFood(food)"
              >
                <span class="delete-icon">🗑️</span>
                <span class="delete-text">删除</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Picker 模式：底部操作栏 -->
      <div v-if="isPickerMode" class="picker-bar">
        <span class="picker-count">已选 {{ selectedFoods.length }} 项</span>
        <el-button
          type="primary"
          :disabled="selectedFoods.length === 0"
          @click="openConfirmDialog"
          class="picker-confirm-btn"
        >
          确认选择
        </el-button>
      </div>

      <!-- 克数确认对话框 -->
      <el-dialog v-model="showConfirmDialog" title="确认食物克数" width="88%" :close-on-click-modal="false" align-center>
        <div class="confirm-food-list">
          <div v-for="food in selectedFoods" :key="food.id" class="confirm-food-row">
            <div class="confirm-food-info">
              <span class="confirm-food-name">{{ food.name }}</span>
              <span class="confirm-food-cal">{{ food.caloriesPer100g }} 千卡/100g</span>
            </div>
            <div class="confirm-grams-input">
              <el-input-number
                v-model="gramsMap[food.id]"
                :min="1"
                :max="2000"
                :step="10"
                size="small"
                controls-position="right"
                style="width: 120px"
              />
              <span class="grams-unit">克</span>
            </div>
            <div class="confirm-food-preview" v-if="gramsMap[food.id]">
              ≈ {{ Math.round(food.caloriesPer100g * gramsMap[food.id] / 100) }} 千卡
            </div>
          </div>
        </div>
        <div class="confirm-total-preview" v-if="batchPreviewTotal > 0">
          <span class="total-label">合计预估热量</span>
          <span class="total-value">{{ batchPreviewTotal }} 千卡</span>
        </div>
        <template #footer>
          <el-button @click="showConfirmDialog = false">返回调整</el-button>
          <el-button type="primary" @click="handleBatchSubmit" :loading="submitting">
            确认添加 {{ selectedFoods.length }} 项
          </el-button>
        </template>
      </el-dialog>

      <!-- 创建自定义食物对话框 -->
      <el-dialog v-model="showCreateDialog" title="创建自定义食物" width="88%" :close-on-click-modal="false" align-center>
        <el-form :model="createForm" label-position="top">
          <el-form-item label="食物名称" required>
            <el-input v-model="createForm.name" placeholder="例如：妈妈的红烧肉" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="createForm.categoryId" placeholder="选择食物分类" style="width:100%">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="每100g热量（千卡）" required>
            <el-input-number v-model="createForm.caloriesPer100g" :min="0" :max="900" :step="1" placeholder="千卡/100g" style="width:100%" size="large" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="蛋白质（g）">
                <el-input-number v-model="createForm.proteinPer100g" :min="0" :max="100" :step="0.5" placeholder="可选" style="width:100%" size="large" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="脂肪（g）">
                <el-input-number v-model="createForm.fatPer100g" :min="0" :max="100" :step="0.5" placeholder="可选" style="width:100%" size="large" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="碳水（g）">
                <el-input-number v-model="createForm.carbsPer100g" :min="0" :max="100" :step="0.5" placeholder="可选" style="width:100%" size="large" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateFood" :loading="creating">创建</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

// ===== Picker 模式 =====
const isPickerMode = computed(() => !!route.query.mealType)
const mealType = computed(() => route.query.mealType || null)
const selectedFoods = ref([])
const expandedIds = reactive(new Set())

function toggleExpand(foodId) {
  if (expandedIds.has(foodId)) {
    expandedIds.delete(foodId)
  } else {
    expandedIds.add(foodId)
  }
}

const showConfirmDialog = ref(false)
const submitting = ref(false)
const gramsMap = reactive({})

function toggleSelect(food) {
  const idx = selectedFoods.value.findIndex(f => f.id === food.id)
  if (idx >= 0) {
    selectedFoods.value.splice(idx, 1)
  } else {
    selectedFoods.value.push(food)
  }
}

function isSelected(food) {
  return selectedFoods.value.some(f => f.id === food.id)
}

function openConfirmDialog() {
  selectedFoods.value.forEach(food => {
    if (!(food.id in gramsMap)) {
      gramsMap[food.id] = 100
    }
  })
  showConfirmDialog.value = true
}

const batchPreviewTotal = computed(() => {
  return selectedFoods.value.reduce((sum, food) => {
    const grams = gramsMap[food.id] || 0
    return sum + Math.round(food.caloriesPer100g * grams / 100)
  }, 0)
})

function getMealLabel(type) {
  const map = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
  return map[type] || type
}

async function handleBatchSubmit() {
  const userId = user.value.id
  const mealDate = route.query.mealDate || (() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })()

  const invalidItems = selectedFoods.value.filter(f => !gramsMap[f.id] || gramsMap[f.id] <= 0)
  if (invalidItems.length > 0) {
    ElMessage.warning(`请为「${invalidItems[0].name}」填写克数`)
    return
  }

  const payload = selectedFoods.value.map(food => ({
    foodId: food.id,
    grams: gramsMap[food.id],
    mealType: mealType.value,
    mealDate,
    note: null
  }))

  submitting.value = true
  try {
    await api.post(`/records/batch/${userId}`, payload)
    ElMessage.success(`已添加 ${payload.length} 项食物到${getMealLabel(mealType.value)}`)
    showConfirmDialog.value = false
    selectedFoods.value = []
    router.back()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// 上部功能标签
const topTabs = [
  { key: 'common', label: '常见', icon: '⭐' },
  { key: 'custom', label: '自定义', icon: '✏️' },
  { key: 'favorites', label: '收藏', icon: '❤️' },
]

// 下部分类（从后端加载）
const categories = ref([])
const categoriesReady = ref(false)

const activeTab = ref('common')
const activeCatId = ref(null)
const foods = ref([])
const loading = ref(false)
const searchText = ref('')

// ===== 收藏状态 =====
const favoriteIds = reactive(new Set())   // 当前用户收藏的食物 ID 集合

// ===== 自定义食物创建 =====
const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = reactive({
  name: '',
  categoryId: null,
  caloriesPer100g: null,
  proteinPer100g: null,
  fatPer100g: null,
  carbsPer100g: null
})

const filteredFoods = computed(() => {
  if (!searchText.value.trim()) return foods.value
  const kw = searchText.value.trim().toLowerCase()
  return foods.value.filter(f => f.name.toLowerCase().includes(kw))
})

const panelTitle = computed(() => {
  if (isPickerMode.value) {
    const labels = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
    return `选择${labels[mealType.value] || ''}食物`
  }
  if (activeTab.value === 'common') return '常见食物'
  if (activeTab.value === 'custom') return '自定义食物'
  if (activeTab.value === 'favorites') return '我的收藏'
  const cat = categories.value.find(c => c.id === activeCatId.value)
  return cat ? cat.name : '食物列表'
})

// 加载分类列表
async function loadCategories() {
  try {
    const res = await api.get('/foods/categories')
    categories.value = res
  } catch (e) { /* ignore */ }
  categoriesReady.value = true
}

// 请求竞态守卫：标签/分类切换共用一个序号，旧响应晚到直接丢弃
let tabSeq = 0

// 切换到功能标签
async function switchTab(key) {
  const seq = ++tabSeq
  activeTab.value = key
  activeCatId.value = null
  searchText.value = ''
  loading.value = true
  try {
    if (key === 'common') {
      const res = await api.get('/foods/common')
      if (seq !== tabSeq) return
      foods.value = res
    } else if (key === 'favorites') {
      const res = await api.get(`/foods/favorites/${user.value.id}`)
      if (seq !== tabSeq) return
      foods.value = res
    } else if (key === 'custom') {
      const res = await api.get(`/foods/creator/${user.value.id}`)
      if (seq !== tabSeq) return
      foods.value = res
    }
  } catch (e) {
    if (seq !== tabSeq) return
    foods.value = []
  } finally {
    if (seq === tabSeq) loading.value = false
  }
}

// 切换到分类
async function switchCategory(catId) {
  const seq = ++tabSeq
  activeTab.value = 'category'
  activeCatId.value = catId
  searchText.value = ''
  loading.value = true
  try {
    // Picker 选餐模式：显示该分类全部食物（否则选不到非常见的菜）
    // 普通浏览模式：只显示该分类常见食物
    const url = isPickerMode.value
      ? `/foods/category/${catId}`
      : `/foods/category/${catId}/common`
    const res = await api.get(url)
    if (seq !== tabSeq) return
    foods.value = res
  } catch (e) {
    if (seq !== tabSeq) return
    foods.value = []
  } finally {
    if (seq === tabSeq) loading.value = false
  }
}

// ===== 收藏功能 =====

// 加载当前用户收藏的食物 ID 列表
async function loadFavoriteIds() {
  try {
    const ids = await api.get(`/foods/favorites/ids/${user.value.id}`)
    favoriteIds.clear()
    ids.forEach(id => favoriteIds.add(id))
  } catch (e) { /* 静默失败，收藏显示为未收藏状态 */ }
}

function isFavorited(foodId) {
  return favoriteIds.has(foodId)
}

// 收藏/取消收藏（乐观更新，失败回滚）
async function toggleFavorite(food) {
  const foodId = food.id
  const wasFavorited = favoriteIds.has(foodId)

  // 乐观更新
  if (wasFavorited) {
    favoriteIds.delete(foodId)
  } else {
    favoriteIds.add(foodId)
  }

  try {
    if (wasFavorited) {
      await api.delete('/foods/favorite', { params: { userId: user.value.id, foodId } })
      ElMessage.success('已取消收藏')
      // 如果在收藏 tab，从列表移除
      if (activeTab.value === 'favorites') {
        const idx = foods.value.findIndex(f => f.id === foodId)
        if (idx >= 0) foods.value.splice(idx, 1)
      }
    } else {
      await api.post('/foods/favorite', null, { params: { userId: user.value.id, foodId } })
      ElMessage.success('已收藏')
    }
  } catch (e) {
    // 回滚
    if (wasFavorited) {
      favoriteIds.add(foodId)
    } else {
      favoriteIds.delete(foodId)
    }
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

// ===== 自定义食物 =====

// 重置创建表单
function resetCreateForm() {
  Object.assign(createForm, {
    name: '',
    categoryId: null,
    caloriesPer100g: null,
    proteinPer100g: null,
    fatPer100g: null,
    carbsPer100g: null
  })
}

// 打开创建对话框（私有自定义食物）
function openCreateDialog() {
  resetCreateForm()
  showCreateDialog.value = true
}

// 创建自定义食物
async function handleCreateFood() {
  if (!createForm.name || !createForm.name.trim()) {
    ElMessage.warning('请输入食物名称')
    return
  }
  if (!createForm.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (!createForm.caloriesPer100g || createForm.caloriesPer100g <= 0) {
    ElMessage.warning('请输入每100g热量')
    return
  }

  creating.value = true
  try {
    const payload = {
      name: createForm.name.trim(),
      categoryId: createForm.categoryId,
      caloriesPer100g: createForm.caloriesPer100g,
      proteinPer100g: createForm.proteinPer100g || 0,
      fatPer100g: createForm.fatPer100g || 0,
      carbsPer100g: createForm.carbsPer100g || 0
    }
    const res = await api.post('/foods/custom', payload, { params: { userId: user.value.id } })
    ElMessage.success(`已创建「${payload.name}」`)
    showCreateDialog.value = false
    // 在自定义 tab 时把新食物加到列表顶部
    if (activeTab.value === 'custom') {
      foods.value.unshift(res)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '操作失败')
  } finally {
    creating.value = false
  }
}

// 删除自定义食物
async function handleDeleteFood(food) {
  try {
    await ElMessageBox.confirm(`确定删除「${food.name}」吗？删除后不可恢复`, '删除确认', { type: 'warning' })
  } catch (e) { return }
  try {
    await api.delete(`/foods/custom/${food.id}`, { params: { userId: user.value.id } })
    ElMessage.success(`已删除「${food.name}」`)
    const idx = foods.value.findIndex(f => f.id === food.id)
    if (idx >= 0) foods.value.splice(idx, 1)
    expandedIds.delete(food.id)
    favoriteIds.delete(food.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '删除失败')
  }
}

onMounted(() => {
  loadCategories()
  // 支持从 query 指定初始标签（如 /foods?tab=favorites）
  const tabParam = route.query.tab
  if (tabParam === 'favorites' || tabParam === 'custom' || tabParam === 'common') {
    switchTab(tabParam)
  } else {
    switchTab('common')
  }
  loadFavoriteIds()
})
</script>

<style scoped>
.foods-page {
  display: flex;
  height: calc(100vh - 52px); /* 减去顶部导航栏高度 */
  background: transparent;
  opacity: 0;
  animation: fade-in 0.35s ease forwards;
}

/* ===== 左侧导航栏 ===== */
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
  opacity: 0;
  animation: fade-in 0.3s ease forwards;
}
/* 全部分类项 + 分隔线，从上往下渐进式淡入 */
.side-bar .side-tab:nth-child(1)  { animation-delay: 0.04s; }
.side-bar .side-tab:nth-child(2)  { animation-delay: 0.08s; }
.side-bar .side-tab:nth-child(3)  { animation-delay: 0.12s; }
.side-bar .side-tab:nth-child(4)  { animation-delay: 0.16s; }
/* child(5) 是分隔线，单独在 .side-divider 定义 */
.side-bar .side-tab:nth-child(6)  { animation-delay: 0.24s; }
.side-bar .side-tab:nth-child(7)  { animation-delay: 0.28s; }
.side-bar .side-tab:nth-child(8)  { animation-delay: 0.32s; }
.side-bar .side-tab:nth-child(9)  { animation-delay: 0.36s; }
.side-bar .side-tab:nth-child(10) { animation-delay: 0.40s; }
.side-bar .side-tab:nth-child(11) { animation-delay: 0.44s; }

.side-tab:hover {
  background: var(--color-primary-bg-light);
}

.side-tab.active {
  background: var(--color-primary-bg);
}

.side-tab.active .tab-label {
  color: var(--color-primary);
  font-weight: 600;
}

.tab-icon {
  font-size: 20px;
}

.tab-label {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* 虚线分隔 */
.side-divider {
  width: 48px;
  margin: 4px auto;
  border-top: 1.5px dashed var(--color-border);
  flex-shrink: 0;
  opacity: 0;
  animation: fade-in 0.3s ease forwards;
  animation-delay: 0.20s;
}

/* ===== 右侧食物面板 ===== */
.food-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.panel-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 0.04em;
  margin-bottom: 12px;
  padding-left: 4px;
}

.panel-search {
  margin-bottom: 12px;
}

.panel-search :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  background: var(--color-glass-strong);
  box-shadow: var(--shadow-xs);
}

.loading-text,
.empty-text {
  text-align: center;
  color: var(--color-text-muted);
  padding: 40px 0;
  font-size: 14px;
}

.food-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

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
  opacity: 0;
  animation: fade-in 0.3s ease forwards;
}

.food-row:hover {
  background: var(--color-primary-bg-light);
  transform: translateX(3px);
}

.food-row:active {
  transform: scale(0.98);
}

.food-info {
  flex: 1;
  min-width: 0;
}

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
  flex-shrink: 0;
  user-select: none;
  transition: all 0.2s ease;
}

.food-expand:hover {
  background: var(--color-primary-bg);
  transform: scale(1.08);
}

.food-expand:active {
  transform: scale(0.94);
}

.food-expand.active {
  transform: rotate(90deg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

/* 展开状态行（仅普通模式） */
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

/* ===== Picker 模式：面板底部留白 ===== */
.food-panel.picker-active {
  padding-bottom: 72px;
}

/* ===== Picker 返回箭头 ===== */
.picker-back {
  display: inline-block;
  margin-right: 6px;
  font-size: 18px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: color 0.2s ease;
}
.picker-back:hover {
  color: var(--color-primary);
}

/* ===== Picker 勾选框 ===== */
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

/* ===== Picker 底部操作栏 ===== */
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
.picker-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.picker-confirm-btn {
  height: 38px !important;
  border-radius: 10px !important;
  padding: 0 24px !important;
  font-weight: 600 !important;
  font-size: 15px !important;
}

/* ===== 克数确认对话框 ===== */
.confirm-food-list {
  max-height: 50vh;
  overflow-y: auto;
}
.confirm-food-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-border-light);
  gap: 10px;
}
.confirm-food-row:last-child {
  border-bottom: none;
}
.confirm-food-info {
  flex: 1;
  min-width: 0;
}
.confirm-food-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
  display: block;
}
.confirm-food-cal {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}
.confirm-grams-input {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.grams-unit {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}
.confirm-food-preview {
  width: 100%;
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 600;
  text-align: right;
  padding-right: 4px;
}
.confirm-total-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 4px 4px;
  margin-top: 12px;
  border-top: 2px dashed var(--color-border);
}
.total-label {
  font-size: 15px;
  color: var(--color-text-secondary);
  font-weight: 600;
}
.total-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-primary);
}

/* ===== 展开面板操作按钮行（收藏 + 删除） ===== */
.detail-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 0 16px 14px;
}

.fav-btn,
.delete-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: var(--color-glass-strong);
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-base);
  user-select: none;
  box-shadow: var(--shadow-xs);
}

.fav-btn:hover,
.delete-btn:hover {
  transform: scale(1.06);
}

.fav-btn:active,
.delete-btn:active {
  transform: scale(0.94);
}

.fav-btn.favorited {
  background: #fef2f2;
  box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.15);
}

.fav-icon {
  font-size: 16px;
  transition: transform var(--transition-spring);
}

.fav-btn.favorited .fav-icon {
  animation: heart-pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes heart-pop {
  0%   { transform: scale(1); }
  30%  { transform: scale(1.35); }
  60%  { transform: scale(0.9); }
  100% { transform: scale(1); }
}

.fav-text {
  font-weight: 500;
  color: var(--color-text-muted);
}

.fav-btn.favorited .fav-text {
  color: #ef4444;
  font-weight: 600;
}

.delete-icon {
  font-size: 14px;
}

.delete-text {
  font-weight: 500;
  color: var(--color-text-muted);
}

.delete-btn:hover .delete-text {
  color: var(--color-danger);
}

/* ===== 创建/上传食物按钮栏 ===== */
.create-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.create-btn {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary)) !important;
  color: var(--color-text-inverse) !important;
  border: none !important;
  font-weight: 700 !important;
  font-size: 15px !important;
  box-shadow: 0 4px 14px rgba(5, 150, 105, 0.22);
  transition: all var(--transition-spring) !important;
}

.create-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.32);
}

.create-btn:active {
  transform: scale(0.97);
}

.food-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
