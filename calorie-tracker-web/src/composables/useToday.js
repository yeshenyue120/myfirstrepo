import { ref, onMounted, onBeforeUnmount } from 'vue'

function toDateStr(d = new Date()) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * 响应式"今天"日期字符串（yyyy-MM-dd）。
 *
 * 页面常驻跨零点后自动更新，避免 actualToday / threeDaysAgoStr 挂载时算一次导致错位：
 * - 标签页从后台切回（visibilitychange）时刷新；
 * - 每 30 秒轮询一次（仅当天日期变化才触发更新，平时零开销）。
 */
export function useToday() {
  const today = ref(toDateStr())
  let timer = null

  const refresh = () => {
    const s = toDateStr()
    if (today.value !== s) today.value = s
  }

  const onVisibility = () => {
    if (document.visibilityState === 'visible') refresh()
  }

  onMounted(() => {
    refresh()
    document.addEventListener('visibilitychange', onVisibility)
    timer = setInterval(refresh, 30 * 1000)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', onVisibility)
    if (timer) clearInterval(timer)
  })

  return { today }
}
