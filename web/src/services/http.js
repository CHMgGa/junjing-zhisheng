import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

export const aiHttp = axios.create({
  baseURL: '/api/v1',
  timeout: 300000
})

const attachToken = (config) => {
  const token = localStorage.getItem('greenhouse_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

const unwrapResponse = (response) => {
  const result = response.data
  if (result && result.code === 0) {
    return result.data
  }
  const message = result?.message || '请求失败'
  ElMessage.error(message)
  return Promise.reject(new Error(message))
}

const handleError = (error) => {
  const timeout = error.code === 'ECONNABORTED' || String(error.message || '').includes('timeout')
  const message = timeout
    ? 'AI 服务响应超时，请稍后重试或检查 AI 服务状态'
    : error.response?.data?.message || error.message || '网络异常，请稍后重试'
  ElMessage.error(message)
  if (error.response?.status === 401 || error.response?.data?.code === 401) {
    localStorage.removeItem('greenhouse_token')
    localStorage.removeItem('greenhouse_profile')
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }
  return Promise.reject(new Error(message))
}

http.interceptors.request.use(attachToken)
aiHttp.interceptors.request.use(attachToken)
http.interceptors.response.use(unwrapResponse, handleError)
aiHttp.interceptors.response.use(unwrapResponse, handleError)
