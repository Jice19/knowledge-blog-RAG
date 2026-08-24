import axios from 'axios'

/**
 * 统一 Axios 实例：
 * - 请求拦截器：自动携带 JWT
 * - 响应拦截器：统一解包 Result{code,message,data}，401 自动跳登录
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('akb_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      if (res.code === 401) {
        localStorage.removeItem('akb_token')
        localStorage.removeItem('akb_user')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 直接返回 data，调用方无需再 .data.data
    return res.data
  },
  (error) => Promise.reject(error),
)

export default http
