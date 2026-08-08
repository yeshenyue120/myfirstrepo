import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动带上 JWT
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

api.interceptors.response.use(
  response => response.data,
  error => {
    // 401：登录态失效，统一跳转登录页（用 location 避免与 router 循环依赖）
    if (error.response?.status === 401) {
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // 统一错误消息格式：后端 message / error / 网络错误
    const msg = error.response?.data?.message
      || error.response?.data?.error
      || (error.code === 'ECONNABORTED' ? '请求超时' : '')
      || error.message
      || '网络错误'
    error.message = msg
    console.error('请求失败:', error)
    return Promise.reject(error)
  }
)

export default api