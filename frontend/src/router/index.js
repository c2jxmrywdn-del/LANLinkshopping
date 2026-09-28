import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

function readUser() {
  try { return JSON.parse(localStorage.getItem('ll_user') || 'null') } catch (e) { return null }
}

const routes = [
  // ===== 用户前台 =====
  { path: '/', component: MainLayout, children: [
    { path: '', name: 'home', component: () => import('../views/Home.vue') },
    { path: 'mall', name: 'mall', component: () => import('../views/Mall.vue') },
    { path: 'product/:id', name: 'product', component: () => import('../views/ProductDetail.vue') },
    { path: 'cart', name: 'cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
    { path: 'checkout', name: 'checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true } },
    { path: 'orders', name: 'orders', component: () => import('../views/MyOrders.vue'), meta: { auth: true } },
    { path: 'merchant', name: 'merchant', component: () => import('../views/MerchantApply.vue'), meta: { auth: true } },
    { path: 'me', name: 'me', component: () => import('../views/Me.vue'), meta: { auth: true } }
  ]},
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') },

  // ===== 管理员后台（与前台完全分离，无公开入口，靠隐形触发进入）=====
  { path: '/admin', component: () => import('../layouts/AdminLayout.vue'), meta: { admin: true }, children: [
    { path: '', name: 'admin-home', redirect: { name: 'admin-dash' } },
    { path: 'dashboard', name: 'admin-dash', component: () => import('../views/admin/AdminDashboard.vue') },
    { path: 'merchants', name: 'admin-merchants', component: () => import('../views/admin/MerchantReview.vue') },
    { path: 'products', name: 'admin-products', component: () => import('../views/admin/AdminProducts.vue') }
  ]}
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const user = readUser()
  if (to.meta.admin) {
    // 非管理员访问后台：静默送回前台首页，不暴露后台存在
    if (!user || user.roleCode !== 'admin') return { name: 'home' }
    return true
  }
  if (to.meta.auth && !user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
