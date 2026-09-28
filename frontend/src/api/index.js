import request from './request'

export const authApi = {
  register: (d) => request.post('/auth/register', d),
  login: (d) => request.post('/auth/login', d),
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

export const merchantApi = {
  apply: (d) => request.post('/merchant/apply', d),
  my: () => request.get('/merchant/my'),
  // 管理后台专用
  adminList: (reviewStatus) => request.get('/merchant/admin/list', { params: { reviewStatus } }),
  adminReview: (merId, reviewStatus, reason) => request.post(`/merchant/admin/review/${merId}`, null, { params: { reviewStatus, reason } })
}
