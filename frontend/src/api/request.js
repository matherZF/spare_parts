import axios from 'axios'
import { ElMessage } from 'element-plus'

// axios 实例：统一 baseURL 与超时
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器：统一处理 Result 结构 { code, msg, data }
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        // 成功：直接返回 data 字段，调用侧无需再拆 Result
        return body.data
      } else {
        // 业务失败：统一 toast 并 reject
        ElMessage.error(body.msg || '请求失败')
        return Promise.reject(body)
      }
    }
    // 非标准 Result 直接返回
    return body
  },
  (error) => {
    // HTTP 层错误
    const msg =
      error.response?.data?.msg ||
      error.message ||
      '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
