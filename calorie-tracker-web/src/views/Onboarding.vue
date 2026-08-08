<template>
  <div class="onboarding-page">
    <!-- 顶部进度条 -->
    <div class="progress-bar-wrapper">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="progress-text">{{ currentStep }}/14</span>
    </div>

    <!-- 滑动页面区域 -->
    <div class="steps-container">
      <div class="steps-track" :style="{ transform: `translateX(-${(currentStep - 1) * 100}%)` }">
        <div v-for="step in 14" :key="step" class="step-page">
          <div class="step-scroll">
            <component :is="stepComponents[step - 1]" v-model="onboardingData" />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="bottom-bar">
      <el-button v-if="currentStep > 1" @click="prevStep" class="back-btn">上一步</el-button>
      <el-button
        type="primary"
        @click="nextStep"
        :loading="submitting"
        class="next-btn"
        :style="currentStep === 1 ? 'width:100%' : ''"
      >
        {{ currentStep === 14 ? '提交' : '下一步' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const DRAFT_KEY = 'onboarding_draft'

// 14 个子页面组件（逐个创建）
import Step1Gender from './Onboarding/Step1Gender.vue'
import Step2Height from './Onboarding/Step2Height.vue'
import Step3Weight from './Onboarding/Step3Weight.vue'
import Step4Birth from './Onboarding/Step4Birth.vue'
import Step5TargetWeight from './Onboarding/Step5TargetWeight.vue'
import Step6TargetDays from './Onboarding/Step6TargetDays.vue'
import Step7Summary from './Onboarding/Step7Summary.vue'
import Step8Intro from './Onboarding/Step8Intro.vue'
import Step9Drug from './Onboarding/Step9Drug.vue'
import Step10Disease from './Onboarding/Step10Disease.vue'
import Step11Eating from './Onboarding/Step11Eating.vue'
import Step12Social from './Onboarding/Step12Social.vue'
import Step13Hunger from './Onboarding/Step13Hunger.vue'
import Step14Scale from './Onboarding/Step14Scale.vue'


const router = useRouter()
const currentStep = ref(1)
const submitting = ref(false)

const progressPercent = computed(() => Math.round((currentStep.value / 14) * 100))

const stepComponents = [
  Step1Gender, Step2Height, Step3Weight, Step4Birth,
  Step5TargetWeight, Step6TargetDays, Step7Summary, Step8Intro,
  Step9Drug, Step10Disease, Step11Eating, Step12Social,
  Step13Hunger, Step14Scale
]

// 所有 14 页共享的数据
const onboardingData = reactive({
  // P1-P6（有默认值的字段自带可选状态）
  gender: '',
  heightCm: 170,
  weightKg: 65,
  age: null,
  targetWeightKg: 60,
  targetDays: 90,
  // P9-P14（null 表示未选，区分"否"和"没填"）
  usedWeightLossDrug: null,
  diseases: [],
  eatingHabit: '',
  socialEating: '',
  hungerLevel: '',
  hasBodyFatScale: null
})

const user = JSON.parse(localStorage.getItem('user') || '{}')

// ===== 中途暂存：每步变更写入 sessionStorage，刷新/退出后可恢复 =====
function saveDraft() {
  sessionStorage.setItem(DRAFT_KEY, JSON.stringify({
    data: { ...onboardingData },
    currentStep: currentStep.value
  }))
}

watch(onboardingData, saveDraft, { deep: true })
watch(currentStep, saveDraft)

function restoreDraft() {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft.data && typeof draft.data === 'object') {
      Object.keys(onboardingData).forEach(k => {
        if (draft.data[k] !== undefined) onboardingData[k] = draft.data[k]
      })
    }
    if (draft.currentStep) {
      currentStep.value = Math.min(Math.max(Number(draft.currentStep), 1), 14)
    }
  } catch (e) {
    sessionStorage.removeItem(DRAFT_KEY)
  }
}
restoreDraft()

function nextStep() {
  const msg = validateStep(currentStep.value)
  if (msg) {
    ElMessage.warning(msg)
    return
  }
  if (currentStep.value < 14) {
    currentStep.value++
  } else {
    handleSubmit()
  }
}

// 校验当前步骤必填项，返回错误文案；通过返回空字符串
function validateStep(step) {
  const d = onboardingData
  switch (step) {
    case 1:  return !d.gender ? '请选择性别' : ''
    case 4:  return d.age === null ? '请输入年龄' : ''
    case 9:  return d.usedWeightLossDrug === null ? '请选择一项' : ''
    case 10: return d.diseases.length === 0 ? '请选择一项' : ''
    case 11: return !d.eatingHabit ? '请选择一项' : ''
    case 12: return !d.socialEating ? '请选择一项' : ''
    case 13: return !d.hungerLevel ? '请选择一项' : ''
    case 14: return d.hasBodyFatScale === null ? '请选择一项' : ''
    default: return ''
  }
}

function prevStep() {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const body = {
      ...onboardingData,
      diseases: onboardingData.diseases.join(',')  // 数组转逗号分隔
    }
    const res = await api.put(`/auth/onboarding/${user.id}`, body)
    localStorage.setItem('user', JSON.stringify(res))
    sessionStorage.removeItem(DRAFT_KEY)
    ElMessage.success(`方案已生成！每日目标：${res.dailyCalorieTarget} 千卡`)
    router.push('/home')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.onboarding-page {
  display: flex; flex-direction: column;
  height: 100vh;
  background: linear-gradient(160deg, var(--color-primary-bg-light) 0%, #e0f2fe 60%, var(--color-primary-bg-light) 100%);
}

/* 进度条 */
.progress-bar-wrapper {
  display: flex; align-items: center; gap: 12px;
  padding: 24px 24px 0;
  animation: slide-up 0.4s ease forwards;
}
.progress-bar {
  flex: 1; height: 8px;
  background: var(--color-border); border-radius: var(--radius-full);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary-lighter), var(--color-primary));
  border-radius: var(--radius-full);
  transition: width 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}
.progress-fill::after {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}
.progress-text {
  font-size: 13px; color: var(--color-text-muted); font-weight: 600;
  min-width: 42px; text-align: right;
}

/* 滑动区域 */
.steps-container {
  flex: 1; overflow: hidden;
}
.steps-track {
  display: flex; height: 100%;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.step-page {
  min-width: 100%; height: 100%;
  overflow-y: auto;
}
.step-scroll {
  min-height: 100%;
  display: flex; align-items: center; justify-content: center;
}

/* 底部按钮 */
.bottom-bar {
  display: flex; gap: 12px;
  padding: 20px 24px 32px;
}
.back-btn {
  border-radius: var(--radius-md); height: 50px; min-width: 100px;
  font-weight: 500;
  transition: all var(--transition-base);
}
.back-btn:hover { transform: translateX(-2px); }
.next-btn {
  flex: 1; border-radius: var(--radius-md); height: 50px;
  font-size: var(--text-lg); font-weight: 600;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  border: none;
  box-shadow: 0 8px 24px rgba(5, 150, 105, 0.28);
  transition: all var(--transition-spring);
}
.next-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(5, 150, 105, 0.38);
}
.next-btn:active { transform: translateY(0); }
</style>
