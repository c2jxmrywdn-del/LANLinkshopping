import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

function readUser() {
  try { return JSON.parse(localStorage.getItem('ll_user') || 'null') } catch (e) { return null }
}
/** 判断登录用户是否具备全部所需权限（identity.permissions 来自 /auth/me） */
function hasPerms(user, perms) {
  if (!perms || !perms.length) return true
  const owned = (user && user.identity && user.identity.permissions) || []
  return perms.every(p => owned.includes(p))
}

const routes = [
  // ===== 用户前台 =====
  { path: '/', component: MainLayout, children: [
    { path: '', name: 'home', component: () => import('../views/Home.vue') },
    { path: 'mall', name: 'mall', component: () => import('../views/Mall.vue') },
    { path: 'product/:id', name: 'product', component: () => import('../views/ProductDetail.vue') },
    { path: 'cart', name: 'cart', component: () => import('../views/Cart.vue'), meta: { auth: true, perms: ['cart:manage'] } },
    { path: 'checkout', name: 'checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true, perms: ['order:create'] } },
    { path: 'orders', name: 'orders', component: () => import('../views/MyOrders.vue'), meta: { auth: true, perms: ['order:view'] } },
    { path: 'wallet', name: 'wallet', component: () => import('../views/Wallet.vue'), meta: { auth: true } },
    { path: 'merchant', name: 'merchant', component: () => import('../views/MerchantApply.vue'), meta: { auth: true } },
    { path: 'merchant/products', name: 'merchant-products', component: () => import('../views/MerchantProducts.vue'), meta: { auth: true, perms: ['product:publish'] } },
    { path: 'merchant/traffic', name: 'merchant-traffic', component: () => import('../views/MerchantTraffic.vue'), meta: { auth: true, perms: ['merchant:manage'] } },
    { path: 'activity', name: 'activity', component: () => import('../views/ActivityCenter.vue'), meta: { auth: true } },
    { path: 'membership', name: 'membership', component: () => import('../views/MembershipCenter.vue'), meta: { auth: true } },
    { path: 'about', name: 'about', component: () => import('../views/AboutUs.vue') },
    { path: 'me', name: 'me', component: () => import('../views/Me.vue'), meta: { auth: true, perms: ['profile:manage'] } }
  ]},
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') },

  // ===== 管理员后台（与前台完全分离，无公开入口，靠隐形触发进入）=====
  { path: '/admin', component: () => import('../layouts/AdminLayout.vue'), meta: { admin: true }, children: [
    { path: '', name: 'admin-home', redirect: { name: 'admin-dash' } },
    { path: 'dashboard', name: 'admin-dash', component: () => import('../views/admin/AdminDashboard.vue') },
    { path: 'merchants', name: 'admin-merchants', component: () => import('../views/admin/MerchantReview.vue') },
    { path: 'products', name: 'admin-products', component: () => import('../views/admin/AdminProducts.vue') },
    { path: 'payments', name: 'admin-payments', component: () => import('../views/admin/AdminPayments.vue') },
    { path: 'audit', name: 'admin-audit', component: () => import('../views/admin/AdminAuditLog.vue') }
  ]}
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const user = readUser()
  // 管理员后台：仅 admin 身份
  if (to.meta.admin) {
    if (!user || user.roleCode !== 'admin') return { name: 'home' }
    return true
  }
  // 需登录：未登录跳登录页
  if (to.meta.auth && !user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 权限校验：已登录但缺少路由所需权限 → 送回首页（前端条件渲染兜底，后端另有强制校验）
  if (to.meta.perms && user && !hasPerms(user, to.meta.perms)) {
    return { name: 'home' }
  }
  return true
})

export default router
