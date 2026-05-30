import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default request
