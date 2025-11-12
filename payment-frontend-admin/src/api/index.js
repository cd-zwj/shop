import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 使用环境变量配置 API 地址
// 开发环境：直连后端
// 生产环境：通过 Nginx 代理
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

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
  response => response,
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('未登录或登录已过期')
      localStorage.removeItem('token')
      router.push('/login')
    } else if (error.response?.status === 403) {
      ElMessage.error('无权限访问')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器错误')
    }
    return Promise.reject(error)
  }
)

export default api

