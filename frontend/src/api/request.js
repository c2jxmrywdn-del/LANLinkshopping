import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 200) {
      // /auth/me 仅用于静默探测登录态：401 时不弹提示、不强制跳登录
      const isMe = (res.config.url || '').includes('/auth/me')
      if (!isMe) {
        message.error(body.message || '请求失败')
        if (body.code === 401) {
          router.push('/login')
        }
      }
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  (err) => {
    message.error('网络错误: ' + (err.message || ''))
    return Promise.reject(err)
  }
)

export default request
