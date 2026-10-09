import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'
import { showBusyLoading, hideBusyLoading } from '../utils/busy'

// Production Vercel builds use VITE_API_BASE_URL to reach the Railway backend (/api context path).
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
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

function isCsrfMutation(config) {
  const method = (config.method || 'get').toLowerCase()
  const url = (config.url || '').split('?')[0]
  if (['get', 'head', 'options'].includes(method)) return false
  return !['/auth/login', '/auth/register', '/auth/login/2fa'].includes(url) && !url.startsWith('/payment/notify/')
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

���q�^