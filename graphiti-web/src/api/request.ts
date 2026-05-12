import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import { getToken, clearToken, setToken, type LoginResult } from '@/utils/auth'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000 // 默认 10 秒超时，数据导入时可覆盖为 60 秒
})

// 是否正在刷新 token
let isRefreshing = false
// 刷新 token 的等待队列
interface QueuedRequest {
  config: AxiosRequestConfig
  resolve: (value: any) => void
  reject: (error: any) => void
}
let refreshQueue: QueuedRequest[] = []

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message: msg, data } = response.data

    if (code === 200) {
      return data
    } else if (code === 401) {
      // token 过期，尝试刷新
      return handleTokenRefresh(response.config, 0)
    } else {
      // 1002 等业务状态码不弹出错误提示，只抛出错误让调用方处理
      if (code !== 1002) {
        message.error(msg || '请求失败')
      }
      const error = new Error(msg)
      ;(error as any).code = code
      return Promise.reject(error)
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      return handleTokenRefresh(error.config, 0)
    }
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 处理 token 刷新（带重试计数，防止无限循环）
async function handleTokenRefresh(config: AxiosRequestConfig, retryCount: number = 0): Promise<any> {
  // 防止无限循环：最多重试 3 次
  if (retryCount >= 3) {
    clearToken()
    message.error('认证失败，请重新登录')
    window.location.href = '/login'
    return Promise.reject(new Error('Max retry attempts reached'))
  }
  
  if (!isRefreshing) {
    isRefreshing = true
    try {
      // 调用刷新 token 接口（设置 5 秒超时）
      const response = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
        {},
        {
          headers: {
            Authorization: `Bearer ${getToken()}`
          },
          timeout: 5000 // 刷新 token 请求设置较短超时
        }
      )
      const newToken: LoginResult = response.data
      setToken(newToken)
      
      // 执行队列中的请求（添加错误处理，防止单个失败影响其他）
      refreshQueue.forEach(({ config, resolve, reject }) => {
        try {
          config.headers!.Authorization = `Bearer ${newToken.token}`
          service(config).then(resolve).catch(reject)
        } catch (error) {
          console.error('Error executing queued request:', error)
          reject(error)
        }
      })
      refreshQueue = []
      
      // 重试当前请求
      config.headers!.Authorization = `Bearer ${newToken.token}`
      return service(config).catch(error => {
        // 如果重试失败且是 401 错误，增加重试计数
        if (error.response?.status === 401 || error.code === 401) {
          return handleTokenRefresh(config, retryCount + 1)
        }
        throw error
      })
    } catch (refreshError) {
      // 刷新失败，处理队列中的请求（防止内存泄漏）
      refreshQueue.forEach(({ reject }) => {
        try {
          reject(refreshError)
        } catch (error) {
          console.error('Error rejecting queued request:', error)
        }
      })
      refreshQueue = []
      
      clearToken()
      message.error('会话已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  } else {
    // 正在刷新中，将请求加入队列
    return new Promise((resolve, reject) => {
      refreshQueue.push({ config, resolve, reject })
    })
  }
}

export default service
