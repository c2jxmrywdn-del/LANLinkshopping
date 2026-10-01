import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

// CSRF 令牌缓存（模块级，刷新页面后重新获取）
let csrfToken = null

/** 主动拉取并缓存 CSRF 令牌（登录后进入账号中心时调用，避免首次变更被 403） */
export async function fetchCsrfToken() {
  const data = await request.get('/user/csrf-token')
  csrfToken = data.token
  return csrfToken
}

// 是否需要携带 CSRF 令牌：非 GET 且 url 以 /user 开头
function isUserMutation(config) {
  const method = (config.method || 'get').toLowerCase()
  return method !== 'get' && (config.url || '').startsWith('/user')
}

// 是否为 CSRF 失效响应：403 且 message 含 CSRF
function isCsrfFailure(status, body) {
  return status === 403 && /csrf/i.test((body && body.message) || '')
}

// 换新令牌并重放一次原请求（config._retried 防循环）
async function retryWithCsrf(config) {
  await fetchCsrfToken()
  config._retried = true
  config.headers = config.headers || {}
  config.headers['X-CSRF-TOKEN'] = csrfToken
  return request(config)
}

request.interceptors.request.use((config) => {
  if (isUserMutation(config) && csrfToken) {
    config.headers = config.headers || {}
    config.headers['X-CSRF-TOKEN'] = csrfToken
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 200) {
      // CSRF 令牌失效：换新后重放一次
      if (isCsrfFailure(body.code, body) && !res.config._retried && isUserMutation(res.config)) {
        return retryWithCsrf(res.config)
      }
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
    // HTTP 层 403 的 CSRF 失效（后端直接以状态码返回时）
    const resp = err.response
    if (resp && err.config && isCsrfFailure(resp.status, resp.data) && !err.config._retried && isUserMutation(err.config)) {
      return retryWithCsrf(err.config)
    }
    message.error('网络错误: ' + (err.message || ''))
    return Promise.reject(err)
  }
)

export default request
