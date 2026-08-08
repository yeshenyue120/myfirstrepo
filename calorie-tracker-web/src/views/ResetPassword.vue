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
            <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
          </svg>
        </div>
        <h1>重置密码</h1>
        <p class="brand-tagline">设置一个新密码</p>
      </div>

      <template v-if="hasToken">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="auth-form" @keyup.enter="handleSubmit">
          <el-form-item prop="newPassword">
            <el-input v-model="form.newPassword" type="password" placeholder="新密码（6-32 位）" show-password size="large">
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirm">
            <el-input v-model="form.confirm" type="password" placeholder="确认新密码" show-password size="large">
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button class="auth-btn" type="primary" size="large" :loading="loading" @click="handleSubmit">
              重置密码
            </el-button>
          </el-form-item>
        </el-form>

        <p class="switch-text">返回 <span @click="$router.push('/login')">登录</span></p>
      </template>

      <div v-else class="invalid-tip">
        <p>重置链接无效或已过期，请重新申请。</p>
        <el-button class="auth-btn" type="primary" size="large" @click="$router.push('/forgot-password')">
          重新发送重置链接
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const token = route.query.token || ''
const hasToken = computed(() => !!token)

const form = ref({ newPassword: '', confirm: '' })

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await api.post('/auth/reset-password', { token, newPassword: form.value.newPassword })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '重置失败，请重新申请')
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
.bg-blobs { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.blob { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.5; }
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
.auth-form :deep(.el-form-item) { margin-bottom: 22px; }
.auth-form :deep(.el-input__wrapper) {
  border-radius: 14px; padding: 6px 16px; box-shadow: var(--shadow-xs); transition: all var(--transition-base);
}
.auth-form :deep(.el-input__wrapper:hover) { box-shadow: var(--shadow-sm); }
.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.15); border-color: var(--color-primary);
}
.input-icon { width: 18px; height: 18px; margin-right: 10px; color: var(--color-text-placeholder); fill: currentColor; }
.auth-btn.el-button--primary {
  width: 100%; height: 48px; font-size: 17px; font-weight: 600; letter-spacing: 4px;
  border-radius: 14px; border: none;
  background: linear-gradient(135deg, var(--color-primary-lighter) 0%, var(--color-primary) 100%);
  box-shadow: 0 8px 24px rgba(5, 150, 105, 0.28);
  transition: all var(--transition-spring);
}
.auth-btn.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(5, 150, 105, 0.40);
}
.auth-btn.el-button--primary:active { transform: translateY(0); }
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
.invalid-tip { text-align: center; color: var(--color-text-muted); font-size: var(--text-sm); }
.auth-form :deep(.el-form-item) {
  opacity: 0;
  animation: auth-field-in 0.5s ease forwards;
}
.auth-form :deep(.el-form-item:nth-child(1)) { animation-delay: 0.15s; }
.auth-form :deep(.el-form-item:nth-child(2)) { animation-delay: 0.25s; }
.auth-form :deep(.el-form-item:nth-child(3)) { animation-delay: 0.35s; }
</style>
