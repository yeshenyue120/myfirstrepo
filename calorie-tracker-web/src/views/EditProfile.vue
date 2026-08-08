<template>
  <div class="edit-page">
    <div class="header">
      <span class="back" @click="$router.back()">‹ 返回</span>
      <h2>编辑资料</h2>
    </div>

    <el-form ref="formRef" :model="form" label-position="top" class="edit-form">
      <h3 class="section-title">基础数据</h3>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="性别">
            <el-select v-model="form.gender" style="width:100%" @change="previewCalories">
              <el-option label="男" value="MALE" />
              <el-option label="女" value="FEMALE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" @change="previewCalories" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="身高(cm)">
            <el-input-number v-model="form.heightCm" :min="100" :max="250" style="width:100%" @change="previewCalories" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="体重(kg)">
            <el-input-number v-model="form.weightKg" :min="20" :max="300" :step="0.5" style="width:100%" @change="previewCalories" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="目标体重(kg)">
            <el-input-number v-model="form.targetWeightKg" :min="20" :max="300" :step="0.5" style="width:100%" @change="previewCalories" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="目标天数">
            <el-input-number v-model="form.targetDays" :min="minDays || 7" :max="730" :step="7" style="width:100%" @change="previewCalories" />
            <div class="days-hint">
              <span class="hint-text" v-if="recommendedDays > 0 && !atMin">
                推荐 <strong>{{ recommendedDays }}</strong> 天（温和减重）
              </span>
              <span class="hint-warn" v-if="atMin">
                ⚡ 已是最快健康速度
              </span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 实时预览 -->
      <div class="preview-box" v-if="previewTarget !== null">
        <span>📊 预估每日摄入</span>
        <span class="preview-cal">{{ previewTarget }} 千卡</span>
      </div>

      <h3 class="section-title">减重评估</h3>
      <el-form-item label="是否用过减重处方药">
        <el-radio-group v-model="form.usedWeightLossDrug">
          <el-radio :value="true">是</el-radio>
          <el-radio :value="false">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="饮食习惯">
        <el-radio-group v-model="form.eatingHabit">
          <el-radio value="REGULAR">规律</el-radio>
          <el-radio value="ADJUSTABLE">不规律但可调整</el-radio>
          <el-radio value="HARD">很难调整</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="应酬频率">
        <el-radio-group v-model="form.socialEating">
          <el-radio value="RARE">很少</el-radio>
          <el-radio value="SOMETIMES">偶尔</el-radio>
          <el-radio value="OFTEN">经常</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="饥饿感">
        <el-radio-group v-model="form.hungerLevel">
          <el-radio value="RARE">很少饿</el-radio>
          <el-radio value="SOMETIMES">偶尔饿</el-radio>
          <el-radio value="OFTEN">经常饿</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="是否有体脂秤">
        <el-radio-group v-model="form.hasBodyFatScale">
          <el-radio :value="true">有</el-radio>
          <el-radio :value="false">没有</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting" style="width:100%">
          保存修改
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()
const submitting = ref(false)
const previewTarget = ref(null)

const user = JSON.parse(localStorage.getItem('user') || '{}')

const form = ref({
  gender: '',
  heightCm: 170,
  weightKg: 65,
  birthDate: '',
  targetWeightKg: 60,
  targetDays: 90,
  usedWeightLossDrug: false,
  diseases: '',
  eatingHabit: '',
  socialEating: '',
  hungerLevel: '',
  hasBodyFatScale: false
})

// ---- 从表单数据实时估算年龄 ----
function getAge() {
  if (!form.value.birthDate) return null
  const birth = new Date(form.value.birthDate)
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const m = now.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) age--
  return age
}

// ---- 实时 BMR（Mifflin-St Jeor）----
function calcBMR() {
  const { gender, heightCm, weightKg } = form.value
  const age = getAge()
  if (!gender || !heightCm || !weightKg || !age) return null
  if (gender === 'MALE') {
    return 10 * weightKg + 6.25 * heightCm - 5 * age + 5
  } else {
    return 10 * weightKg + 6.25 * heightCm - 5 * age - 161
  }
}

// ---- 活动系数（基准 1.2 + 评估加成，与后端 estimateActivityFactor 一致）----
function calcActivityFactor() {
  let factor = 1.2
  const { socialEating, hungerLevel, hasBodyFatScale } = form.value
  if (socialEating === 'OFTEN') factor += 0.15
  else if (socialEating === 'SOMETIMES') factor += 0.075
  if (hungerLevel === 'OFTEN') factor += 0.1
  else if (hungerLevel === 'SOMETIMES') factor += 0.05
  if (hasBodyFatScale) factor += 0.05
  return Math.min(factor, 1.725)
}

// ---- 实时 TDEE（活动系数 × BMR）----
function calcTDEE() {
  const bmr = calcBMR()
  if (!bmr) return null
  return bmr * calcActivityFactor()
}

// ---- 最早达标天数（25% TDEE 最大缺口）----
const minDays = computed(() => {
  const tdee = calcTDEE()
  const { weightKg, targetWeightKg } = form.value
  if (!tdee || !weightKg || !targetWeightKg || weightKg === targetWeightKg) return 0
  const maxDeficit = tdee * 0.25
  if (maxDeficit <= 0) return 0
  const deltaKg = Math.abs(weightKg - targetWeightKg)
  return Math.ceil((deltaKg * 7700) / maxDeficit)
})

// 如果当前天数 < 最早达标天数，自动修正
watch(minDays, (min) => {
  if (min > 0 && form.value.targetDays < min) {
    form.value.targetDays = min
  }
})

// ---- 是否为保守减脂对象（与后端 isConservativeCase 一致）----
function isConservativeCase() {
  const f = form.value
  if (f.usedWeightLossDrug) return true
  if (f.diseases && f.diseases.includes('甲减')) return true
  if (f.eatingHabit === 'HARD') return true
  if (f.socialEating === 'OFTEN') return true
  if (f.hungerLevel === 'OFTEN') return true
  return false
}

// ---- 推荐天数（保守对象 10% 缺口，其余 15%，与后端 calculateRecommendedDays 一致）----
const recommendedDays = computed(() => {
  const tdee = calcTDEE()
  const { weightKg, targetWeightKg } = form.value
  if (!tdee || !weightKg || !targetWeightKg || weightKg === targetWeightKg) return 0
  const percent = isConservativeCase() ? 0.10 : 0.15
  const deficit = tdee * percent
  const deltaKg = Math.abs(weightKg - targetWeightKg)
  return Math.max(Math.round((deltaKg * 7700) / deficit), 14)
})

const atMin = computed(() => minDays.value > 0 && form.value.targetDays <= minDays.value)

// ---- 实时预览（百分比缺口，与后台公式一致）----
function previewCalories() {
  const tdee = calcTDEE()
  const { weightKg, targetWeightKg, targetDays, gender } = form.value
  if (!tdee || !weightKg || !targetWeightKg || !targetDays || targetDays <= 0) {
    previewTarget.value = null
    return
  }

  const delta = Math.abs(weightKg - targetWeightKg)
  const losing = weightKg > targetWeightKg
  const minCalories = gender === 'MALE' ? 1500 : 1200

  if (delta === 0) {
    previewTarget.value = Math.round(tdee)
    return
  }

  // 百分比缺口（与后台 CalorieCalculator 一致）
  const requiredPercent = (delta * 7700) / targetDays / tdee

  let target
  if (losing) {
    const deficitPercent = Math.max(0.10, Math.min(0.25, requiredPercent))
    target = tdee * (1 - deficitPercent)
  } else {
    const surplusPercent = Math.max(0.10, Math.min(0.20, requiredPercent))
    const surplus = Math.min(tdee * surplusPercent, 500)
    target = tdee + surplus
  }

  target = Math.max(minCalories, Math.min(target, tdee + 500))
  previewTarget.value = Math.round(target * 10) / 10
}

onMounted(() => {
  // 用 localStorage 里的数据填充
  const u = JSON.parse(localStorage.getItem('user') || '{}')
  Object.keys(form.value).forEach(k => {
    if (u[k] !== undefined && u[k] !== null) form.value[k] = u[k]
  })
  if (!form.value.diseases) form.value.diseases = ''
  previewCalories()
})

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await api.put(`/auth/onboarding/${user.id}`, form.value)
    localStorage.setItem('user', JSON.stringify(res))
    ElMessage.success('修改成功！新目标：' + res.dailyCalorieTarget + ' 千卡')
    router.back()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.edit-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--space-lg);
  background: transparent;
  min-height: 100vh;
  animation: page-fade-in 0.35s ease;
}
.header { display: flex; align-items: center; gap: 12px; margin-bottom: var(--space-lg); }
.back {
  font-size: 18px; color: var(--color-primary); cursor: pointer;
  font-weight: 600; transition: transform var(--transition-fast);
}
.back:hover { transform: translateX(-3px); }
.header h2 { margin: 0; color: var(--color-primary-dark); font-size: var(--text-xl); font-weight: 700; }
.section-title {
  color: var(--color-primary-dark); margin: var(--space-lg) 0 var(--space-sm);
  font-size: var(--text-lg); font-weight: 700;
  letter-spacing: 0.04em;
}
.edit-form {
  background: var(--color-glass); padding: var(--space-lg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.days-hint { height: 20px; line-height: 20px; margin-top: 4px; }
.hint-text { font-size: 12px; color: var(--color-text-muted); }
.hint-text strong { color: var(--color-text-secondary); font-weight: 600; }
.hint-warn { font-size: 12px; color: var(--color-warning); font-weight: 500; }
.preview-box {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px; background: var(--color-primary-bg-light);
  border-radius: var(--radius-md); margin-bottom: 10px;
  border: 1px dashed var(--color-primary-lighter);
  transition: all var(--transition-base);
}
.preview-box:hover { background: var(--color-primary-bg); }
.preview-cal {
  font-size: var(--text-xl); font-weight: 700;
  color: var(--color-primary); font-variant-numeric: tabular-nums;
}
</style>
