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
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
            />
          </svg>
        </div>
        <h1>创建账号</h1>
        <p class="brand-tagline">开启你的健康之旅</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="rules"
        label-width="0"
        class="auth-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名（2-20个字符）"
            size="large"
            class="auth-input"
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱"
            size="large"
            class="auth-input"
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"
                />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码（至少6位）"
            show-password
            size="large"
            class="auth-input"
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
                />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            class="auth-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="switch-text">
        已有账号？<span @click="$router.push('/login')">去登录</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const registerFormRef = ref(null)
const router = useRouter()

const registerForm = ref({
  username: '',
  email: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await api.post('/auth/register', registerForm.value)
    // 注册即登录：存 token + user，直接进引导流程（解决"注册后二次登录"）
    if (res.token) localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(res))
    ElMessage.success('注册成功！')
    if (!res.gender || res.age === 0) {
      router.push('/onboarding')
    } else {
      router.push('/home')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '注册失败')
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
.blob-1 { width: 340px; height: 340px; background: #86efac; top: -80px; right: -80px; }
.blob-2 { width: 300px; height: 300px; background: #6ee7b7; bottom: 10%; left: -60px; animation-delay: 2.5s; }
.blob-3 { width: 240px; height: 240px; background: #93c5fd; top: 40%; right: 28%; animation-delay: 5s; }

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

.auth-form :deep(.el-form-item) {
  margin-bottom: 22px;
  opacity: 0;
  animation: auth-field-in 0.5s ease forwards;
}
.auth-form :deep(.el-form-item:nth-child(1)) { animation-delay: 0.15s; }
.auth-form :deep(.el-form-item:nth-child(2)) { animation-delay: 0.25s; }
.auth-form :deep(.el-form-item:nth-child(3)) { animation-delay: 0.35s; }
.auth-form :deep(.el-form-item:nth-child(4)) { animation-delay: 0.45s; }

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

.input-icon {
  width: 18px; height: 18px; margin-right: 10px;
  color: var(--color-text-placeholder); fill: currentColor;
  transition: color var(--transition-base);
}
.auth-form :deep(.el-input__wrapper.is-focus) .input-icon {
  color: var(--color-primary);
}

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

.switch-text { margin-top: 24px; text-align: center; font-size: var(--text-sm); color: var(--color-text-muted); }
.switch-text span {
  position: relative;
  color: var(--color-primary); font-weight: 600; cursor: pointer;
}
.switch-text span::after {
  content: ''; position: absolute; left: 0; bottom: -2px;
  width: 0; height: 2px; background: var(--color-primary); border-radius: 2px;
  transition: width 0.3s ease;
}
.switch-text span:hover::after { width: 100%; }
</style>
