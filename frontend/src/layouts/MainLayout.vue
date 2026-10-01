<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <img src="/logo-256.png" alt="LANLinkshopping" class="logo-img" />
          <span class="logo-text">LAN<b>Link</b>shopping</span>
        </div>
        <a-menu v-model:selectedKeys="selectedKeys" mode="horizontal" class="nav" theme="dark"
               @click="onNav">
          <a-menu-item key="home">首页</a-menu-item>
          <a-menu-item key="mall">商城</a-menu-item>
          <a-menu-item key="orders">我的订单</a-menu-item>
          <a-menu-item key="merchant">商户入驻</a-menu-item>
        </a-menu>
        <div class="right">
          <a-badge :count="cart.count" :overflow-count="99">
            <a-button type="text" style="color:#fff" @click="$router.push('/cart')">🛒 购物车</a-button>
          </a-badge>
          <template v-if="user.logged">
            <a-dropdown>
              <a style="color:#fff">{{ user.user.nickname }} ▾</a>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="$router.push('/me')">个人中心</a-menu-item>
                  <a-menu-item @click="doLogout">退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <a-button type="primary" ghost @click="$router.push('/login')">登录 / 注册</a-button>
          </template>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>
    <a-layout-footer class="footer" @click="secretTap">
      LANLinkshopping · 一种聚合型一体多元化解决方案电商平台（B2B 毕业设计演示）
    </a-layout-footer>
  </a-layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const cart = useCartStore()

const selectedKeys = ref(['home'])
onMounted(() => {
  if (!user.user) user.fetchMe()
  if (user.logged) cart.load()
})

function onNav({ key }) {
  if (key === 'home') router.push('/')
  else router.push('/' + key)
}
async function doLogout() {
  await user.logout()
  router.push('/login')
}

// 隐形管理入口：连续点击页脚 10 次（2.5 秒内）且当前为平台运营才进入后台。
// 对普通用户完全无提示——非管理员点了没有任何反应。
let taps = 0
let lastTap = 0
function secretTap() {
  const now = Date.now()
  if (now - lastTap > 2500) taps = 0
  lastTap = now
  taps++
  if (taps >= 10) {
    taps = 0
    if (user.isAdmin) router.push({ name: 'admin-dash' })
  }
}
</script>

<style scoped>
.header { background: var(--ll-navy); padding: 0; position: sticky; top: 0; z-index: 100; }
.header-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; height: 64px; }
.logo { display: flex; align-items: center; gap: 8px; color: #fff; font-size: 20px; font-weight: 700; cursor: pointer; margin-right: 32px; white-space: nowrap; }
.logo-img { width: 34px; height: 34px; border-radius: 8px; display: block; }
.logo-text b { color: var(--ll-amber); font-weight: 700; }
.nav { flex: 1; background: transparent; border-bottom: none; }
.right { display: flex; align-items: center; gap: 16px; }
.content { max-width: 1200px; margin: 0 auto; width: 100%; padding: 24px 16px; }
.footer { text-align: center; color: #888; background: var(--ll-page); }
</style>
