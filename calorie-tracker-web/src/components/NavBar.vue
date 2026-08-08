<template>
  <nav class="top-nav">
    <span class="nav-brand" @click="$router.push('/home')">神月健康</span>
    <div class="nav-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="nav-item"
        :class="{ active: activePath === tab.path }"
        @click="$router.push(tab.path)"
      >
        <span class="nav-label">{{ tab.label }}</span>
      </div>
    </div>
    <div class="nav-user" @click.stop="showDropdown = !showDropdown">
      <span class="avatar-dot">{{ userInitial }}</span>
    </div>

    <!-- 下拉面板 -->
    <Teleport to="body">
      <div v-if="showDropdown" class="dropdown-overlay" @click="showDropdown = false"></div>
    </Teleport>
    <Transition name="drop">
      <div v-if="showDropdown" class="user-dropdown">
        <div class="drop-header">
          <span class="drop-avatar">{{ userInitial }}</span>
          <div class="drop-user-info">
            <span class="drop-name">{{ user.username || '--' }}</span>
            <span class="drop-email">{{ displayEmail }}</span>
          </div>
        </div>
        <div class="drop-divider"></div>
        <div class="drop-stats">
          <div class="drop-stat">
            <span class="drop-stat-label">每日目标</span>
            <span class="drop-stat-value">{{ Math.round(user.dailyCalorieTarget || 0) }} 千卡</span>
          </div>
          <div class="drop-stat">
            <span class="drop-stat-label">当前体重</span>
              <span class="drop-stat-value">{{ latestWeight !== null ? latestWeight : (user.weightKg || '--') }} kg</span>
          </div>
        </div>
        <div class="drop-divider"></div>
        <div class="drop-action" @click="goProfile">进入个人中心 →</div>
      </div>
    </Transition>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const showDropdown = ref(false)
const latestWeight = ref(null)

// 用户信息
const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : {}
const userInitial = computed(() => (user.username || '?').charAt(0))

// 邮箱截断显示
const displayEmail = computed(() => {
  const email = user.email || ''
  if (email.length <= 18) return email
  return email.slice(0, 16) + '…'
})

const activePath = computed(() => route.path)

const tabs = [
  { path: '/home', label: '记录' },
  { path: '/foods', label: '食物' },
  { path: '/report', label: '报告' }
]

function goProfile() {
  showDropdown.value = false
  router.push('/profile')
}

// 加载最新体重
onMounted(async () => {
  if (!user.id) return
  try {
    const res = await api.get(`/weights/latest/${user.id}`)
    latestWeight.value = res?.weightKg ?? null
  } catch (e) {
    // 静默失败，显示 localStorage 中的初始体重兜底
  }
})
</script>

<style scoped>
.top-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 100;
  display: flex; justify-content: center; align-items: center;
  height: 52px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border-light);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.nav-brand {
  position: absolute; left: 20px;
  font-size: 18px; font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-primary);
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s ease;
}
.nav-brand:hover { opacity: 0.8; }

.nav-tabs {
  display: flex; gap: 48px;
}

.nav-item {
  padding: 8px 18px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}
.nav-item:hover { background: var(--color-primary-bg-light); }
.nav-item.active {
  border-bottom-color: var(--color-primary);
}
.nav-item.active .nav-label { color: var(--color-primary); font-weight: 700; }

.nav-label {
  font-size: 14px; color: var(--color-text-muted);
  transition: color 0.2s ease;
}

/* 用户头像 */
.nav-user {
  position: absolute; right: 20px;
  cursor: pointer;
  transition: transform 0.2s ease;
  z-index: 101;
}
.nav-user:hover { transform: scale(1.08); }

.avatar-dot {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: #fff;
  font-size: 14px; font-weight: 700;
}

/* 透明遮罩（Teleport 到 body，覆盖全屏） */
:global(.dropdown-overlay) {
  position: fixed; inset: 0; z-index: 99;
}

/* 下拉面板 */
.user-dropdown {
  position: fixed; top: 56px; right: 12px; z-index: 101;
  width: 220px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border-light);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: 16px;
}

.drop-header {
  display: flex; align-items: center; gap: 12px;
}

.drop-avatar {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; min-width: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary));
  color: #fff;
  font-size: 17px; font-weight: 700;
}

.drop-user-info {
  display: flex; flex-direction: column; gap: 2px;
  min-width: 0;
}
.drop-name {
  font-size: 15px; font-weight: 700; color: var(--color-text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.drop-email {
  font-size: 12px; color: var(--color-text-muted);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.drop-divider {
  height: 1px; background: var(--color-border-light);
  margin: 12px 0;
}

.drop-stats {
  display: flex; flex-direction: column; gap: 8px;
}
.drop-stat {
  display: flex; justify-content: space-between; align-items: center;
}
.drop-stat-label {
  font-size: 13px; color: var(--color-text-muted);
}
.drop-stat-value {
  font-size: 13px; font-weight: 600; color: var(--color-text);
}

.drop-action {
  font-size: 14px; font-weight: 600; color: var(--color-primary);
  cursor: pointer;
  text-align: center;
  padding: 6px 0;
  border-radius: 8px;
  transition: background 0.2s ease;
}
.drop-action:hover { background: var(--color-primary-bg-light); }

/* 下拉面板过渡动画 */
.drop-enter-active {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.drop-leave-active {
  transition: all 0.15s ease-in;
}
.drop-enter-from,
.drop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.96);
}
</style>
