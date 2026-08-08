<template>
  <div class="auth-page">
    <div class="bg-blobs" aria-hidden="true">
      <div class="blob blob-1 auth-animate-float" />
      <div class="blob blob-2 auth-animate-float" />
      <div class="blob blob-3 auth-animate-float" />
    </div>

    <div class="auth-card auth-animate-card">
      <div class="brand">
        <div class="brand-icon auth-animate-icon-pulse">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M17,8C8,10 5.9,16.17 3.82,21.34L5.71,22L6.66,19.7C7.14,19.87 7.64,20 8,20C19,20 22,3 22,3C21,5 14,5.25 9,6.25C4,7.25 2,11.5 2,13.5C2,15.5 3.75,17.25 3.75,17.25C7,8 17,8 17,8Z"
            />
          </svg>
        </div>
        <h1>卡路里健康管理</h1>
        <p class="brand-tagline">管理你的每日饮食</p>
      </div>

      <!-- ===== 已保存的账号快捷登录 ===== -->
      <div v-if="savedAccounts.length > 0 && !showManualLogin" class="saved-accounts">
        <div
          v-for="(account, index) in savedAccounts"
          :key="account.email"
          class="account-item auth-animate-field"
        >
          <div class="account-info">
            <div class="account-avatar">{{ account.username?.charAt(0) }}</div>
            <div class="account-meta">
              <div class="account-name">{{ account.username }}</div>
              <div class="account-email">{{ account.email }}</div>
            </div>
          </div>
          <div class="account-actions">
            <el-button type="primary" round @click="selectAccount(account)">
              登录
            </el-button>
            <button class="logout-link" @click="removeAccount(index)" title="移除账号">登出</button>
          </div>
        </div>

        <el-divider></el-divider>
        <el-button type="primary" text @click="showManualLogin = true" style="width:100%">
          使用其他账号登录
        </el-button>
      </div>

      <!-- ===== 手动登录表单 ===== -->
      <div v-if="savedAccounts.length === 0 || showManualLogin">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          label-width="0"
          class="auth-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="account">
            <el-input
              v-model="loginForm.account"
              placeholder="邮箱 / 用户名"
              size="large"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              show-password
              size="large"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <div class="form-extra">
              <el-checkbox v-model="savePassword">记住账号</el-checkbox>
              <span class="forgot-link" @click="$router.push('/forgot-password')">忘记密码？</span>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              class="auth-btn"
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <p class="switch-text">
          还没有账号？<span @click="$router.push('/register')">去注册</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()
const loading = ref(false)
const loginFormRef = ref(null)
const savePassword = ref(false)
const showManualLogin = ref(false)

const loginForm = ref({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入邮箱或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ===== 已保存的账号列表 =====
const savedAccounts = ref([])

onMounted(() => {
  const saved = localStorage.getItem('savedAccounts')
  if (saved) {
    const accounts = JSON.parse(saved)
    // 过滤历史遗留的明文密码字段，只保留身份信息
    const cleaned = accounts.map(({ savedPassword, ...rest }) => rest)
    savedAccounts.value = cleaned
    localStorage.setItem('savedAccounts', JSON.stringify(cleaned))
  }
})

// 保存账号到本地（只存身份信息，绝不存密码）
const saveAccountToLocal = (user) => {
  const accounts = savedAccounts.value.filter(a => a.email !== user.email)
  accounts.unshift({
    id: user.id,
    email: user.email,
    username: user.username
  })
  savedAccounts.value = accounts
  localStorage.setItem('savedAccounts', JSON.stringify(accounts))
}

// 登录成功后的跳转
const goAfterLogin = (res) => {
  if (res.token) localStorage.setItem('token', res.token)
  localStorage.setItem('user', JSON.stringify(res))
  if (!res.gender || res.age === 0) {
    router.push('/onboarding')
  } else {
    router.push('/home')
  }
}

// 选择账号（填入账号，手输密码）
const selectAccount = (account) => {
  loginForm.value.account = account.email
  showManualLogin.value = true
}

// 移除账号
const removeAccount = (index) => {
  savedAccounts.value.splice(index, 1)
  localStorage.setItem('savedAccounts', JSON.stringify(savedAccounts.value))
}

// 手动登录
const handleLogin = async () => {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await api.post('/auth/login', {
      email: loginForm.value.account,
      password: loginForm.value.password
    })
    if (savePassword.value) saveAccountToLocal(res)
    ElMessage.success('登录成功！')
    loginForm.value = { account: '', password: '' }
    goAfterLogin(res)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-primary-bg-light) 0%, #e0f2fe 50%, var(--color-primary-bg-light) 100%);
}

.bg-blobs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}
.blob-1 { width: 340px; height: 340px; background: #86efac; top: -80px; left: -80px; }
.blob-2 { width: 300px; height: 300px; background: #6ee7b7; bottom: 10%; right: -60px; animation-delay: 2.5s; }
.blob-3 { width: 240px; height: 240px; background: #93c5fd; top: 45%; left: 30%; animation-delay: 5s; }

.auth-card {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: 92%;
  padding: 40px 32px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(20px);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.auth-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 72px rgba(5, 150, 105, 0.16);
}

.brand { text-align: center; margin-bottom: 28px; }
.brand-icon {
  width: 56px; height: 56px; margin: 0 auto 14px;
  display: flex; align-items: center; justify-content: center; border-radius: 50%;
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  box-shadow: 0 10px 28px rgba(5, 150, 105, 0.25);
}
.brand-icon svg { width: 28px; height: 28px; fill: currentColor; }
.brand h1 { margin: 0 0 6px; font-size: 22px; font-weight: 700; color: var(--color-primary-dark); letter-spacing: 1px; }
.brand-tagline { margin: 0; font-size: var(--text-sm); color: var(--color-text-muted); }

/* 已保存账号 */
.saved-accounts { margin-bottom: var(--space-sm); }
.account-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 18px 16px; margin-bottom: 12px;
  background: var(--color-primary-bg-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
}
.account-item:hover { background: #dcfce7; transform: translateX(4px); }
.account-info { display: flex; align-items: center; gap: 12px; flex: 0 1 auto; min-width: 0; }
.account-meta { min-width: 0; }
.account-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: var(--color-text-inverse); font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.account-name {
  font-weight: 600; color: var(--color-primary-dark); font-size: 15px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.account-email {
  font-size: 12px; color: var(--color-text-muted); margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.account-actions {
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}

/* 登出 — 低调文字链接 */
.logout-link {
  background: none; border: none; padding: 4px 0;
  font-size: 12px; color: var(--color-text-muted); cursor: pointer;
  transition: color var(--transition-fast);
}
.logout-link:hover { color: var(--color-danger); }

/* 表单输入框 */
.auth-form :deep(.el-form-item) {
  margin-bottom: 22px;
}
.auth-form :deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 6px 16px;
  box-shadow: var(--shadow-xs);
  transition: all var(--transition-base);
}
.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: var(--shadow-sm);
}
.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.15);
  border-color: var(--color-primary);
}

.input-icon { width: 18px; height: 18px; margin-right: 10px; color: var(--color-text-placeholder); fill: currentColor; }

/* 复选框行 — 紧凑 */
.auth-form :deep(.el-form-item:has(.el-checkbox)) {
  margin-bottom: 18px;
}

/* 记住账号 + 忘记密码 两端布局 */
.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.forgot-link {
  font-size: 12px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: color var(--transition-fast);
}
.forgot-link:hover { color: var(--color-primary); }

/* 登录按钮 */
.auth-btn.el-button--primary {
  width: 100%; height: 48px; font-size: 17px; font-weight: 600; letter-spacing: 6px;
  border-radius: 14px; border: none;
  background: linear-gradient(135deg, var(--color-primary-lighter) 0%, var(--color-primary) 100%);
  box-shadow: 0 8px 24px rgba(5, 150, 105, 0.28);
  transition: all var(--transition-spring);
}
.auth-btn.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(5, 150, 105, 0.40);
}
.auth-btn.el-button--primary:active {
  transform: translateY(0);
}

/* 底部切换链接 */
.switch-text { text-align: center; font-size: var(--text-sm); color: var(--color-text-muted); margin-top: 24px; }
.switch-text span {
  position: relative;
  color: var(--color-primary); font-weight: 600; cursor: pointer;
  transition: color var(--transition-fast);
}
.switch-text span::after {
  content: ''; position: absolute; left: 0; bottom: -2px;
  width: 0; height: 2px; background: var(--color-primary); border-radius: 2px;
  transition: width 0.3s ease;
}
.switch-text span:hover::after { width: 100%; }
.switch-text span:active { color: var(--color-primary-dark); }

/* 表单项入场 */
.auth-form :deep(.el-form-item) {
  opacity: 0;
  animation: auth-field-in 0.5s ease forwards;
}
.auth-form :deep(.el-form-item:nth-child(1)) { animation-delay: 0.15s; }
.auth-form :deep(.el-form-item:nth-child(2)) { animation-delay: 0.25s; }
.auth-form :deep(.el-form-item:nth-child(3)) { animation-delay: 0.35s; }
.auth-form :deep(.el-form-item:nth-child(4)) { animation-delay: 0.45s; }
.auth-form :deep(.el-form-item:nth-child(5)) { animation-delay: 0.55s; }
</style>