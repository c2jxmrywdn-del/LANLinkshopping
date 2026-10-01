import request, { fetchCsrfToken } from './request'

export const authApi = {
  register: (d) => request.post('/auth/register', d),
  login: (d) => request.post('/auth/login', d),
  // 两步验证登录：登录返回 require2fa 后，用一次性票据 + 动态码完成登录
  login2fa: (d) => request.post('/auth/login/2fa', d),
  logout: () => request.post('/auth/logout'),
  me: () => request.get('/auth/me')
}

export const homeApi = {
  industries: () => request.get('/home/industries'),
  hot: (limit = 8) => request.get('/home/hot', { params: { limit } }),
  categories: (indId) => request.get('/category/list', { params: { indId } })
}

export const productApi = {
  page: (params) => request.get('/product/page', { params }),
  detail: (id) => request.get(`/product/detail/${id}`),
  publish: (d) => request.post('/product/publish', d),
  // 管理后台专用
  adminList: (params) => request.get('/product/admin/list', { params }),
  adminStatus: (prodId, status) => request.post(`/product/admin/status/${prodId}`, null, { params: { status } })
}

export const cartApi = {
  add: (d) => request.post('/cart/add', d),
  list: () => request.get('/cart/list'),
  updateQty: (cartId, quantity) => request.post(`/cart/quantity/${cartId}`, null, { params: { quantity } }),
  remove: (cartId) => request.delete(`/cart/${cartId}`)
}

export const orderApi = {
  checkout: (d) => request.post('/order/checkout', d),
  my: () => request.get('/order/my'),
  detail: (orderNo) => request.get(`/order/detail/${orderNo}`),
  pay: (orderNo) => request.post(`/order/pay/${orderNo}`),
  cancel: (orderNo) => request.post(`/order/cancel/${orderNo}`)
}

// ===== 账号中心（/user/**，非 GET 请求自动携带 CSRF 令牌，见 request.js） =====
export const userApi = {
  // 个人资料
  getProfile: () => request.get('/user/profile'),
  putProfile: (patch) => request.put('/user/profile', patch),
  // 头像上传（FormData，支持 onUploadProgress 进度回调）
  uploadAvatar: (file, onProgress) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/user/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => { if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100)) }
    })
  },
  // 联系方式换绑（演示环境验证码由接口直接返回 devCode）
  sendPhoneCode: (phone) => request.post('/user/phone-code', { phone }),
  bindPhone: (phone, code) => request.post('/user/phone-bind', { phone, code }),
  sendEmailCode: (email) => request.post('/user/email-code', { email }),
  bindEmail: (email, code) => request.post('/user/email-bind', { email, code }),
  // 账户安全
  changePassword: (oldPassword, newPassword) => request.post('/user/password', { oldPassword, newPassword }),
  getSecurity: () => request.get('/user/security'),
  totpSetup: () => request.post('/user/2fa/setup'),
  totpEnable: (code) => request.post('/user/2fa/enable', { code }),
  totpDisable: (code) => request.post('/user/2fa/disable', { code }),
  // 系统设置
  getSettings: () => request.get('/user/settings'),
  putSettings: (patch) => request.put('/user/settings', patch),
  resetSettings: () => request.post('/user/settings/reset'),
  // 第三方授权
  thirdAuthList: () => request.get('/user/third-auth'),
  revokeThirdAuth: (id) => request.delete(`/user/third-auth/${id}`),
  // 审计：客户端侧事件上报（如 cache.clear）
  audit: (action, detail) => request.post('/user/audit', { action, detail })
}

export const merchantApi = {
  apply: (d) => request.post('/merchant/apply', d),
  my: () => request.get('/merchant/my'),
  // 管理后台专用
  adminList: (reviewStatus) => request.get('/merchant/admin/list', { params: { reviewStatus } }),
  adminReview: (merId, reviewStatus, reason) => request.post(`/merchant/admin/review/${merId}`, null, { params: { reviewStatus, reason } })
}

// ===== 管理后台审计（/admin/**，AuthInterceptor 已限定 admin 角色） =====
export const adminApi = {
  auditPage: (params) => request.get('/admin/audit/page', { params }),
  auditExport: (params) => request.get('/admin/audit/export', { params, responseType: 'blob' })
}