// 每日提醒调度：基于浏览器 Notification API + 每分钟检查
// 提醒类型：喝水提醒（多个时间点）、晚间记录提醒（单时间点）
// 配置存 localStorage('reminders')，App.vue 启动时调度

const DEFAULTS = {
  enabled: false,
  water: { enabled: true, times: ['10:00', '14:00', '16:00', '20:00'] },
  record: { enabled: true, time: '21:00' }
}

let timer = null
// 已触发过的提醒 key（"日期:时间"），避免同一时间点重复弹
const firedKeys = new Set()

export function loadReminderConfig() {
  let raw = null
  try {
    raw = JSON.parse(localStorage.getItem('reminders'))
  } catch (e) { raw = null }
  return {
    ...DEFAULTS,
    ...(raw || {}),
    water: { ...DEFAULTS.water, ...(raw?.water || {}) },
    record: { ...DEFAULTS.record, ...(raw?.record || {}) }
  }
}

export function saveReminderConfig(cfg) {
  localStorage.setItem('reminders', JSON.stringify(cfg))
}

// 首次开启时请求通知权限
export function requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().catch(() => {})
  }
}

export function startReminderScheduler() {
  if (timer) return
  timer = setInterval(check, 60 * 1000)
  check()
}

export function stopReminderScheduler() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function check() {
  const cfg = loadReminderConfig()
  if (!cfg.enabled) return
  if (!('Notification' in window) || Notification.permission !== 'granted') return

  const now = new Date()
  const todayKey = `${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`
  const hm = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const key = `${todayKey}:${hm}`

  if (cfg.water.enabled && (cfg.water.times || []).includes(hm)) {
    fire(key, '喝水时间到 💧', '喝一杯水，保持水分摄入哦～')
  }
  if (cfg.record.enabled && cfg.record.time === hm) {
    fire(key, '记录今日饮食 📝', '睡前回顾一下今天吃了什么吧')
  }

  // 清理 7 天前的触发记录，避免无限增长
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  // Set 存的是字符串，按日期粗略清理即可（低频，量小）
  if (firedKeys.size > 1000) {
    for (const k of [...firedKeys]) {
      if (!k.startsWith(`${todayKey.split('-').slice(0, 2).join('-')}`)) firedKeys.delete(k)
    }
  }
}

function fire(key, title, body) {
  if (firedKeys.has(key)) return
  firedKeys.add(key)
  try {
    new Notification(title, { body })
  } catch (e) { /* 权限不足时静默 */ }
}
