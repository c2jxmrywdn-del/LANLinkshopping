<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <img src="/logo-256.png" alt="LANLinkshopping" class="logo-img" />
          <span class="logo-text">LAN<b>Link</b>shopping</span>
        </div>
        <div class="nav-shell" ref="navShell">
          <a-menu :selected-keys="selectedKeys" mode="horizontal" class="nav" theme="dark"
                 @click="onNav">
            <a-menu-item key="home">首页</a-menu-item>
            <a-menu-item key="mall">商城</a-menu-item>
            <a-menu-item key="orders">我的订单</a-menu-item>
            <a-menu-item key="merchant">商户入驻</a-menu-item>
            <a-menu-item key="merchant/products">我的商品</a-menu-item>
          </a-menu>
          <span class="nav-indicator" :style="indicatorStyle" aria-hidden="true"></span>
        </div>
        <div class="right">
          <a-badge v-if="user.logged" :count="user.unread" :overflow-count="99">
            <a-button type="text" class="ll-tap" style="color:#fff" @pointerdown="onTap" @click="$router.push({ path: '/me', query: { tab: 'messages' } })">🔔 消息</a-button>
          </a-badge>
          <a-badge :count="cart.count" :overflow-count="99">
            <a-button type="text" class="ll-tap" style="color:#fff" @pointerdown="onTap" @click="$router.push('/cart')">🛒 购物车</a-button>
          </a-badge>
          <template v-if="user.logged">
            <a-dropdown>
              <a class="ll-ripple-host ll-tap user-trigger" style="color:#fff" @pointerdown="onTap">
                <span v-if="user.isVip" class="id-badge id-vip" title="VIP 用户">VIP</span>
                <span v-else-if="user.isAdmin" class="id-badge id-admin" title="平台管理员">ADMIN</span>
                <span v-else-if="user.isMerchant" class="id-badge id-merchant" title="入驻商户">商户</span>
                {{ user.user.nickname }} ▾
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item disabled>
                    <div class="id-scope">当前身份：<b>{{ user.identityName }}</b><br/>{{ user.identityScope }}</div>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="$router.push('/me')">个人中心</a-menu-item>
                  <a-menu-item @click="doLogout">退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <!-- 深色顶栏上使用白字白边（WCAG 对比度达标） -->
            <a-button type="primary" ghost style="color:#fff;border-color:#fff" @click="$router.push('/login')">登录 / 注册</a-button>
          </template>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content class="content">
      <main><router-view /></main>
    </a-layout-content>
    <a-layout-footer class="footer" @click="secretTap">
      LANLinkshopping · 一种聚合型一体多元化解决方案电商平台（B2B 毕业设计演示）
    </a-layout-footer>
  </a-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import { useSettingsStore } from '../store/settings'
import { tapFeedback } from '../utils/feedback'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const cart = useCartStore()
const settings = useSettingsStore()

const NAV_PATHS = [
  { key: 'home', match: (p) => p === '/' },
  { key: 'mall', match: (p) => p === '/mall' || p.startsWith('/product') },
  { key: 'orders', match: (p) => p.startsWith('/orders') },
  { key: 'merchant/products', match: (p) => p.startsWith('/merchant/products') },
  { key: 'merchant', match: (p) => p === '/merchant' },
]
const selectedKeys = computed(() => {
  const hit = NAV_PATHS.find((n) => n.match(route.path))
  return hit ? [hit.key] : []
})

// 自定义滑动位置指示条：JS 定位到选中项下方，随路由/窗口尺寸平滑移动
const navShell = ref(null)
const indicatorStyle = ref({ left: '0px', width: '0px', opacity: 0 })
function updateIndicator() {
  const shell = navShell.value
  if (!shell) return
  const sel = shell.querySelector('.ant-menu-item-selected')
  if (!sel) { indicatorStyle.value = { ...indicatorStyle.value, opacity: 0 }; return }
  const sRect = shell.getBoundingClientRect()
  const iRect = sel.getBoundingClientRect()
  indicatorStyle.value = { left: `${iRect.left - sRect.left}px`, width: `${iRect.width}px`, opacity: 1 }
}
watch(selectedKeys, () => nextTick(updateIndicator))

onMounted(() => {
  if (!user.user) user.fetchMe()
  if (user.logged) cart.load()
  nextTick(updateIndicator)
  window.addEventListener('resize', updateIndicator)
})
onBeforeUnmount(() => window.removeEventListener('resize', updateIndicator))

function onNav({ key }) {
  if (key === 'home') router.push('/')
  else router.push('/' + key)
}

// 导航模块触达反馈：150ms 缩放+高亮 + 移动端震动（消息/购物车/我的）
function onTap(e) {
  tapFeedback(e.currentTarget)
}
async function doLogout() {
  await user.logout()
  settings.clear() // 退出后恢复默认外观
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
.nav-shell { position: relative; flex: 1; min-width: 0; }
.nav { background: transparent; border-bottom: none; }
/* 导航交互态：未选中柔白，选中色与后台激活色一致（主色 --ll-azure + 指示条 --ll-accent-gradient 天蓝→青） */
.header :deep(.ant-menu-horizontal) { background: transparent; border-bottom: none; }
.header :deep(.ant-menu-horizontal .ant-menu-item) { color: rgba(255, 255, 255, 0.72); transition: color 0.25s ease; }
.header :deep(.ant-menu-horizontal .ant-menu-item:hover) { color: #fff; }
.header :deep(.ant-menu-horizontal .ant-menu-item-selected) { color: var(--ll-azure, #0EA5E9); font-weight: 600; }
/* 自定义滑动位置指示条：JS 定位到选中项下方，0.28s 平滑移动 */
.nav-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 3px;
  background: var(--ll-accent-gradient);
  border-radius: 3px 3px 0 0;
  pointer-events: none;
  transition: left 0.28s cubic-bezier(0.4, 0, 0.2, 1), width 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;
}
.right { display: flex; align-items: center; gap: 16px; }
/* 身份徽章 */
.id-badge { display: inline-block; font-size: 11px; font-weight: 700; line-height: 1;
            padding: 3px 6px; border-radius: 4px; margin-right: 6px; vertical-align: 1px; }
.id-vip { background: #b45309; color: #fff; }       /* 琥珀：VIP */
.id-admin { background: #1e6eb8; color: #fff; }     /* 天蓝：管理员 */
.id-merchant { background: #0f766e; color: #fff; }  /* 青绿：商户 */
.id-scope { font-size: 12px; color: var(--ll-gray, #4b5563); line-height: 1.6; white-space: normal; }
.id-scope b { color: var(--ll-ink, #0f172a); }
.content { max-width: 1200px; margin: 0 auto; width: 100%; padding: 24px 16px; }
.footer { text-align: center; color: #4b5563 !important; background: var(--ll-page); }
.footer .ant-layout-footer { color: inherit; }

/* 响应式：平板/移动端收紧间距与字号，导航色变与滑动指示条在各尺寸均生效 */
@media (max-width: 991px) {
  .logo { margin-right: 16px; }
  .header-inner { gap: 8px; }
}
@media (max-width: 767px) {
  .logo { margin-right: 10px; }
  .logo-text { display: none; }
  .header :deep(.ant-menu-horizontal .ant-menu-item) { font-size: 14px; padding-inline: 12px; }
  .right { gap: 10px; }
}
</style>
