<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <img src="/logo-256.png" alt="LANLinkshopping" class="logo-img" />
          <span class="logo-text ll-wordmark"><span class="ll-lan">LAN</span><span class="ll-link">Link</span><span class="ll-shopping">shopping</span></span>
        </div>
        <div class="nav-shell" ref="navShell" @mouseleave="scheduleCloseMallMenu">
          <a-menu v-model:selectedKeys="selectedKeys" mode="horizontal" class="nav" theme="dark"
               :ellipsis="false" @click="onNav">
            <a-menu-item key="home">首页</a-menu-item>
            <a-menu-item key="mall" @mouseenter="openMallMenu">商城</a-menu-item>
            <a-menu-item key="promotions">优惠</a-menu-item>
            <a-menu-item v-if="user.hasPerm('credit:view')" key="credit-term">账期</a-menu-item>
            <a-menu-item v-if="user.hasPerm('order:view')" key="orders">我的订单</a-menu-item>
            <!-- 商户模块：管理员直达后台「商户管理」；未入驻用户显示「商户入驻」；已入驻商户原位替换为流量管理 -->
            <a-menu-item v-if="user.isAdmin" key="admin/merchants">商户管理</a-menu-item>
            <a-menu-item v-else-if="user.logged && !user.isMerchant" key="merchant">商户入驻</a-menu-item>
            <a-menu-item v-if="user.hasPerm('merchant:manage')" key="merchant/traffic">流量管理</a-menu-item>
            <a-menu-item v-if="user.hasPerm('product:publish')" key="merchant/products">我的商品</a-menu-item>
            <a-menu-item key="about">关于我们</a-menu-item>
          </a-menu>

          <div v-show="mallMenuOpen" class="mall-mega" @mouseenter="openMallMenu">
            <div class="mega-column mega-industries">
              <div class="mega-heading">
                <span>行业</span><small>INDUSTRIES</small>
              </div>
              <button type="button" class="mega-all" :class="{active: !activeMallIndustry}" @click.stop="openMallAll">
                全部商品
              </button>
              <button
                v-for="ind in mallIndustries"
                :key="ind.indId"
                type="button"
                class="mega-item"
                :class="{active: activeMallIndustry?.indId === ind.indId}"
                @mouseenter="activeMallIndustry = ind"
                @focus="activeMallIndustry = ind"
                @click.stop="openMallIndustry(ind)"
              >
                <span>{{ ind.name }}</span>
                <span class="mega-arrow">›</span>
              </button>
            </div>

            <div class="mega-column mega-categories">
              <div class="mega-heading">
                <span>{{ activeMallIndustry?.name || '全部行业' }}</span>
                <small>PRODUCT CATEGORIES</small>
              </div>
              <template v-if="activeMallIndustry && activeMallCategories.length">
                <button
                  v-for="cat in activeMallCategories"
                  :key="cat.catId"
                  type="button"
                  class="mega-category"
                  @click.stop="openMallCategory(cat)"
                >
                  <span class="category-dot"></span>{{ cat.name }}
                  <em>进入</em>
                </button>
              </template>
              <div v-else class="mega-empty">
                <strong>综合商品中心</strong>
                <span>覆盖四大行业采购需求</span>
              </div>
            </div>

            <div class="mega-column mega-shortcuts">
              <div class="mega-heading">
                <span>采购入口</span><small>QUICK ACCESS</small>
              </div>
              <button type="button" class="shortcut-card" @click.stop="openMallAll">
                <b>全部商品</b><span>查看全平台供给</span><i>→</i>
              </button>
              <button type="button" class="shortcut-card" @click.stop="openMallSales">
                <b>热销采购</b><span>按销量快速筛选</span><i>→</i>
              </button>
              <button type="button" class="shortcut-card" @click.stop="openPromotions">
                <b>采购优惠</b><span>满减与行业折扣</span><i>→</i>
              </button>
            </div>
          </div>

          <span class="nav-indicator" :style="indicatorStyle" aria-hidden="true"></span>
        </div>
        <div class="right">
          <a-badge v-if="user.logged" :count="user.unread" :overflow-count="99">
            <a-button type="text" class="ll-tap" style="color:#fff" @pointerdown="onTap" @click.stop="goMessages">🔔 消息</a-button>
          </a-badge>
          <a-badge :count="cart.count" :overflow-count="99">
            <a-button type="text" class="ll-tap" style="color:#fff" @pointerdown="onTap" @click.stop="goCart">🛒 购物车</a-button>
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
                  <a-menu-item @click="$router.push('/wallet')">💰 我的钱包</a-menu-item><a-menu-item v-if="user.hasPerm('credit:view')" @click="$router.push('/credit-term')">企业账期</a-menu-item>
                  <a-menu-item @click="$router.push('/activity')">🎯 活动中心</a-menu-item>
                  <a-menu-item @click="$router.push('/membership')">💎 会员中心</a-menu-item>
                  <a-menu-item @click="$router.push('/me')">个人资料</a-menu-item><a-menu-item @click="$router.push('/me/messages')">消息中心</a-menu-item><a-menu-item @click="$router.push('/me/address')">收货地址</a-menu-item><a-menu-item @click="$router.push('/me/login-log')">登录安全</a-menu-item><a-menu-item @click="$router.push('/me/settings')">账户设置</a-menu-item>
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
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="footer-wordmark ll-wordmark"><span class="ll-lan">LAN</span><span class="ll-link">Link</span><span class="ll-shopping">shopping</span></div>
          <p>让每一次连接，都产生价值。</p>
          <small>CONNECTED COMMERCE · B2B DEMO PLATFORM</small>
        </div>
        <div class="footer-links">
          <button type="button" @click.stop="$router.push('/mall')">商城</button>
          <button type="button" @click.stop="$router.push('/about')">关于我们</button>
          <button type="button" @click.stop="$router.push({ path: '/about', hash: '#contact' })">联系我们</button>
        </div>
        <div class="footer-meta">LANLinkshopping · 一种聚合型一体多元化解决方案电商平台<br/><span>B2B 毕业设计演示 · © {{ new Date().getFullYear() }}</span></div>
      </div>
    </a-layout-footer>
  </a-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { homeApi } from '../api'
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
  { key: 'promotions', match: (p) => p.startsWith('/promotions') },
  { key: 'credit-term', match: (p) => p.startsWith('/credit-term') },
  { key: 'orders', match: (p) => p.startsWith('/orders') },
  { key: 'merchant/products', match: (p) => p.startsWith('/merchant/products') },
  { key: 'merchant/traffic', match: (p) => p.startsWith('/merchant/traffic') },
  { key: 'merchant', match: (p) => p === '/merchant' },
  { key: 'about', match: (p) => p === '/about' },
]
const selectedKeys = computed(() => {
  const hit = NAV_PATHS.find((n) => n.match(route.path))
  return hit ? [hit.key] : []
})

const mallMenuOpen = ref(false)
const mallIndustries = ref([])
const mallCategories = ref([])
const activeMallIndustry = ref(null)
let mallCloseTimer = null

const activeMallCategories = computed(() => {
  if (!activeMallIndustry.value) return []
  return mallCategories.value
    .filter((c) => Number(c.indId) === Number(activeMallIndustry.value.indId))
    .filter((c) => Number(c.level || 1) === 1)
    .sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0))
})

function openMallMenu() {
  if (mallCloseTimer) { clearTimeout(mallCloseTimer); mallCloseTimer = null }
  mallMenuOpen.value = true
  if (!activeMallIndustry.value && mallIndustries.value.length) activeMallIndustry.value = mallIndustries.value[0]
}

function scheduleCloseMallMenu() {
  if (mallCloseTimer) clearTimeout(mallCloseTimer)
  mallCloseTimer = window.setTimeout(() => { mallMenuOpen.value = false }, 90)
}

function openMallAll() {
  mallMenuOpen.value = false
  router.push({ name: 'mall' })
}

function openMallIndustry(ind) {
  activeMallIndustry.value = ind
  router.push({ name: 'mall', query: { indId: ind.indId } })
  mallMenuOpen.value = false
}

function openMallCategory(cat) {
  mallMenuOpen.value = false
  router.push({ name: 'mall', query: { indId: cat.indId, catId: cat.catId } })
}

function openMallSales() {
  mallMenuOpen.value = false
  router.push({ name: 'mall', query: { sort: 'sales' } })
}

function openPromotions() {
  mallMenuOpen.value = false
  router.push({ name: 'promotions' })
}

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

onMounted(async () => {
  if (!user.user) await user.fetchMe()
  if (user.logged) cart.load()
  try {
    const [inds, cats] = await Promise.all([homeApi.industries(), homeApi.categories()])
    mallIndustries.value = inds || []
    mallCategories.value = cats || []
    const activeId = Number(route.query.indId || 0)
    activeMallIndustry.value = mallIndustries.value.find((ind) => Number(ind.indId) === activeId) || mallIndustries.value[0] || null
  } catch (e) {
    // 导航菜单为增强交互，接口失败不影响主站其它功能。
  }
  nextTick(updateIndicator)
  window.addEventListener('resize', updateIndicator)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIndicator)
  if (mallCloseTimer) clearTimeout(mallCloseTimer)
})

function goMessages() {
  if (!user.logged) return router.push({ name: 'login', query: { redirect: '/me/messages' } })
  router.push({ name: 'account-messages' })
}
function goCart() {
  if (!user.logged) return router.push({ name: 'login', query: { redirect: '/cart' } })
  router.push({ name: 'cart' })
}

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
.logo { display: flex; align-items: center; gap: 9px; color: #fff; font-size: 21px; font-weight: 700; cursor: pointer; margin-right: 32px; white-space: nowrap; }
.logo-img { width: 36px; height: 36px; border-radius: 8px; display: block; }
.nav-shell { position: relative; flex: 1; min-width: 0; overflow: visible; }
.nav { background: transparent; border-bottom: none; }
/* 导航交互态：未选中柔白，选中文字用亮青 --ll-cyan（可读性更佳）+ 指示条 --ll-accent-gradient 天蓝→青，与后台激活色同源 */
.header :deep(.ant-menu-horizontal) { background: transparent; border-bottom: none; }
.header :deep(.ant-menu-horizontal .ant-menu-item) { color: rgba(255, 255, 255, 0.72); transition: color 0.25s ease; }
.header :deep(.ant-menu-horizontal .ant-menu-item:hover) { color: #fff; }
.header :deep(.ant-menu-horizontal .ant-menu-item-selected) { color: var(--ll-cyan, #22D3EE); font-weight: 600; }
/* 自定义滑动位置指示条：JS 定位到选中项下方，0.28s 平滑移动 */
.mall-mega {
  position: absolute;
  left: 0;
  top: 58px;
  width: min(860px, calc(100vw - 48px));
  display: grid;
  grid-template-columns: 190px minmax(250px, 1fr) 250px;
  background: rgba(255,255,255,.97);
  border: 1px solid rgba(148,163,184,.22);
  border-radius: 0 0 18px 18px;
  box-shadow: 0 18px 46px rgba(15,23,42,.20);
  backdrop-filter: blur(18px);
  overflow: hidden;
  z-index: 120;
  animation: mallMegaIn .16s ease-out;
}
.mega-column { padding: 18px 16px; min-height: 250px; }
.mega-industries { background: linear-gradient(180deg, rgba(19,35,58,.045), rgba(19,35,58,.015)); border-right: 1px solid rgba(148,163,184,.18); }
.mega-categories { border-right: 1px solid rgba(148,163,184,.18); }
.mega-heading { display:flex; align-items:flex-end; justify-content:space-between; gap:12px; margin-bottom:12px; }
.mega-heading span { color: var(--ll-navy,#13233A); font-weight: 800; font-size: 14px; }
.mega-heading small { color:#94a3b8; font-size:9px; letter-spacing:.12em; }
.mega-all,.mega-item,.mega-category,.shortcut-card {
  width:100%; border:0; cursor:pointer; text-align:left; font:inherit; background:transparent;
}
.mega-all,.mega-item { display:flex; align-items:center; justify-content:space-between; padding:10px 11px; border-radius:10px; color:#526070; margin-bottom:4px; transition:background .18s ease,color .18s ease,transform .18s ease; }
.mega-all:hover,.mega-item:hover,.mega-item.active { background:rgba(14,165,233,.10); color:var(--ll-navy,#13233A); transform:translateX(2px); }
.mega-arrow { color:#a0aec0; font-size:18px; line-height:1; }
.mega-category { display:flex; align-items:center; gap:9px; padding:12px 10px; border-bottom:1px solid rgba(148,163,184,.12); color:#334155; transition:.18s ease; }
.mega-category:hover { color:var(--ll-navy,#13233A); background:rgba(200,164,92,.09); padding-left:14px; }
.mega-category em { margin-left:auto; color:#a0aec0; font-style:normal; font-size:11px; }
.category-dot { width:6px; height:6px; border-radius:50%; background:var(--ll-amber,#C8A45C); flex:0 0 auto; }
.mega-empty { min-height:180px; display:flex; flex-direction:column; justify-content:center; gap:5px; color:#64748b; }
.mega-empty strong { color:var(--ll-navy,#13233A); font-size:15px; }
.mega-empty span { font-size:12px; }
.shortcut-card { position:relative; display:block; padding:13px 34px 13px 12px; margin-bottom:8px; border:1px solid rgba(148,163,184,.18); border-radius:12px; background:linear-gradient(135deg,rgba(255,255,255,.95),rgba(248,250,252,.75)); transition:.18s ease; }
.shortcut-card:hover { transform:translateY(-1px); border-color:rgba(14,165,233,.25); box-shadow:0 8px 18px rgba(15,23,42,.08); }
.shortcut-card b { display:block; color:var(--ll-navy,#13233A); font-size:13px; margin-bottom:3px; }
.shortcut-card span { display:block; color:#94a3b8; font-size:11px; }
.shortcut-card i { position:absolute; right:12px; top:50%; transform:translateY(-50%); color:var(--ll-amber-strong,#a97f2f); font-style:normal; }
@keyframes mallMegaIn { from { opacity:0; transform:translateY(-5px); } to { opacity:1; transform:translateY(0); } }
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
.footer{padding:0 16px!important;color:#4b5563!important;background:var(--ll-page);border-top:1px solid #D9D3C7}.footer-inner{max-width:1200px;margin:0 auto;min-height:150px;padding:30px 0 24px;display:grid;grid-template-columns:1.4fr .7fr 1.4fr;gap:30px;align-items:center}.footer-brand{border-left:3px solid var(--ll-amber);padding-left:16px}.footer-wordmark{font-size:24px;line-height:1}.footer-wordmark .ll-lan{color:var(--ll-navy)}.footer-wordmark .ll-link{color:var(--ll-amber-strong)}.footer-wordmark .ll-shopping{color:var(--ll-navy)}.footer-brand p{margin:9px 0 4px;color:var(--ll-navy);font-size:13px;font-weight:700}.footer-brand small{color:#7B8492;font-size:9px;letter-spacing:.13em}.footer-links{display:flex;justify-content:center;gap:8px;flex-wrap:wrap}.footer-links button{border:0;background:transparent;color:#667085;cursor:pointer;font:inherit;font-size:12px;padding:7px 9px;border-radius:8px;transition:.2s ease}.footer-links button:hover{color:var(--ll-navy);background:rgba(200,164,92,.12)}.footer-meta{text-align:right;color:#667085;font-size:11px;line-height:1.8}.footer-meta span{color:#98A2B3}

/* 响应式：平板/移动端收紧间距与字号，导航色变与滑动指示条在各尺寸均生效 */
@media (max-width: 991px) {
  .logo { margin-right: 16px; }
  .header-inner { gap: 8px; }
}
@media (max-width: 767px) {
  .mall-mega { display: none !important; }
  /* 移动端：头部两行布局——品牌+操作区在上，导航独占一行可横向滑动（防 flex 压缩塌缩为 0） */
  .header { height: auto; }
  .header-inner { flex-wrap: wrap; height: auto; padding: 6px 12px; }
  .nav-shell { flex-basis: 100%; order: 3; overflow-x: auto; scrollbar-width: none; }
  .nav-shell::-webkit-scrollbar { display: none; }
  .nav { white-space: nowrap; }
  .logo { margin-right: 10px; }
  .logo-text { display: none; }
  .header :deep(.ant-menu-horizontal .ant-menu-item) { font-size: 14px; padding-inline: 12px; }
   .right { gap: 10px; }
  .footer-inner{grid-template-columns:1fr;gap:18px;padding:24px 0}.footer-links{justify-content:flex-start}.footer-meta{text-align:left;grid-column:auto}
}
</style>
