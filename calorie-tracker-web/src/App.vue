<template>
  <el-config-provider :locale="zhCn">
    <div class="app-shell" :class="{ 'has-nav': showNav }">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      <NavBar v-if="showNav" />
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import NavBar from '@/components/NavBar.vue'
import { startReminderScheduler, stopReminderScheduler, requestNotificationPermission } from '@/utils/reminders'

const route = useRoute()

// 只在需要导航的页面显示 NavBar
const navRoutes = ['/home', '/foods', '/report', '/profile', '/edit-profile']
const showNav = computed(() => navRoutes.includes(route.path))

// 启动每日提醒调度（打开 App 即生效）
onMounted(() => {
  startReminderScheduler()
  requestNotificationPermission()
})
onBeforeUnmount(() => {
  stopReminderScheduler()
})
</script>

<style>
/* 页面切换过渡 — 纯透明度渐变，无位移避免抖动 */
.page-enter-active {
  transition: opacity 0.2s ease;
}
.page-leave-active {
  transition: opacity 0.12s ease;
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
}

/* 只在有导航栏时留出顶部空间 */
.app-shell.has-nav {
  padding-top: 52px;
}

/* Element Plus 主题覆盖 */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-light);
  --el-button-hover-border-color: var(--color-primary-light);
  --el-button-active-bg-color: var(--color-primary-dark);
}

.el-slider__bar {
  background: linear-gradient(90deg, var(--color-primary-light), var(--color-primary));
}
.el-slider__button {
  border-color: var(--color-primary);
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}
.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.el-radio__input.is-checked .el-radio__inner {
  border-color: var(--color-primary);
  background: var(--color-primary);
}
.el-radio__input.is-checked + .el-radio__label {
  color: var(--color-primary);
}

.el-checkbox__input.is-checked .el-checkbox__inner {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.el-date-editor {
  --el-date-editor-active-color: var(--color-primary);
}

/* ===== 弹窗美化 ===== */

/* 遮罩层 — 毛玻璃模糊 */
.el-overlay {
  position: fixed !important;
  inset: 0;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  background: rgba(0, 0, 0, 0.35) !important;
}

.el-overlay-dialog {
  display: flex !important;
}

/* 弹窗面板 */
.el-overlay .el-dialog {
  margin: auto !important;
  max-height: 90vh;
  border-radius: 20px !important;
  overflow: visible;
  background: var(--color-glass);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow:
    0 4px 24px rgba(5, 150, 105, 0.10),
    0 12px 48px rgba(0, 0, 0, 0.12),
    0 24px 72px rgba(0, 0, 0, 0.06) !important;
  border-top: 3px solid var(--color-primary) !important;
  animation: dialog-enter 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

/* 入场动画 */
@keyframes dialog-enter {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(16px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 标题栏 */
.el-overlay .el-dialog__header {
  padding: 20px 20px 14px !important;
  margin: 0 !important;
}

.el-overlay .el-dialog__title {
  font-size: 18px !important;
  font-weight: 700 !important;
  color: var(--color-text) !important;
  letter-spacing: 0.01em;
}

/* 标题栏底部绿色渐变分割线 */
.el-overlay .el-dialog__header::after {
  content: '';
  display: block;
  width: 40px;
  height: 3px;
  margin-top: 12px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-lighter));
}

/* 关闭按钮 */
.el-overlay .el-dialog__headerbtn {
  top: 18px !important;
  right: 18px !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  background: var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.el-overlay .el-dialog__headerbtn:hover {
  background: var(--color-primary-bg);
}

.el-overlay .el-dialog__headerbtn .el-dialog__close {
  color: var(--color-text-muted) !important;
  font-size: 16px !important;
  transition: color 0.2s ease;
}

.el-overlay .el-dialog__headerbtn:hover .el-dialog__close {
  color: var(--color-primary) !important;
}

/* 内容区 */
.el-overlay .el-dialog__body {
  padding: 8px 20px 20px !important;
  overflow-y: auto;
  color: var(--color-text-secondary);
  font-size: 15px;
  line-height: 1.6;
}

/* 底部按钮区 */
.el-overlay .el-dialog__footer {
  padding: 0 20px 20px !important;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 底部按钮统一圆角 */
.el-overlay .el-dialog__footer .el-button {
  border-radius: 10px !important;
  padding: 10px 24px !important;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s ease;
}

/* 取消按钮 */
.el-overlay .el-dialog__footer .el-button:not(.el-button--primary) {
  background: var(--color-border-light);
  border: none;
  color: var(--color-text-secondary);
}

.el-overlay .el-dialog__footer .el-button:not(.el-button--primary):hover {
  background: var(--color-border);
  color: var(--color-text);
}

/* 确认按钮 — 绿色渐变 */
.el-overlay .el-dialog__footer .el-button--primary {
  background: linear-gradient(135deg, var(--color-primary-lighter), var(--color-primary)) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(5, 150, 105, 0.25);
}

.el-overlay .el-dialog__footer .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.35);
}

.el-overlay .el-dialog__footer .el-button--primary:active {
  transform: scale(0.97);
}

/* ===== 深色模式组件覆盖 ===== */
[data-theme="dark"] .el-input__wrapper,
[data-theme="dark"] .el-select__wrapper {
  background: var(--color-bg-alt);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}
[data-theme="dark"] .el-input__inner,
[data-theme="dark"] .el-select__placeholder,
[data-theme="dark"] .el-select__selected-item {
  color: var(--color-text);
}
[data-theme="dark"] .el-textarea__inner {
  background: var(--color-bg-alt);
  color: var(--color-text);
}
[data-theme="dark"] .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .el-overlay .el-dialog {
    animation: none;
  }
}
</style>