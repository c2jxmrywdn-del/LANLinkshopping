<template>
  <div class="me-wrap">
    <!-- 加载失败：重试 -->
    <a-result v-if="!(profile.loaded && settings.loaded) && profile.loadError" status="warning"
      :title="t('acct.loadFail')" :sub-title="t('acct.loadFailDesc')">
      <template #extra><a-button type="primary" class="touch" @click="reload">{{ t('acct.retry') }}</a-button></template>
    </a-result>

    <template v-else>
      <div class="grid">
        <!-- 左栏：用户卡 + 快捷入口 -->
        <aside class="col-left">
          <a-card :bordered="false" class="card">
            <div class="user-row">
              <a-avatar :size="56" :src="profile.profile?.avatar" class="avatar">{{ (user.user?.nickname || 'U')[0] }}</a-avatar>
              <div>
                <div class="uname">{{ user.user?.nickname }}</div>
                <div class="urole">{{ user.user?.roleName }}</div>
              </div>
            </div>
            <a-divider style="margin: 12px 0" />
            <div class="quick">{{ t('acct.card.quick') }}</div>
            <a-space direction="vertical" style="width: 100%" :size="8">
              <a-button block class="touch" @click="$router.push('/orders')">{{ t('acct.card.orders') }}</a-button>
              <a-button block class="touch" @click="$router.push('/cart')">{{ t('acct.card.cart') }}</a-button>
              <a-button block class="touch" @click="$router.push('/merchant')">{{ t('acct.card.merchant') }}</a-button>
            </a-space>
            <a-divider style="margin: 12px 0" />
            <a-button block class="touch" :loading="clearingCache" @click="clearCache">{{ t('acct.card.clearCache') }}</a-button>
          </a-card>
        </aside>

        <!-- 主内容：个人信息 / 系统设置 -->
        <section class="col-main">
          <a-card :bordered="false" class="main-card">
            <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
              <a-tab-pane key="profile" :tab="t('acct.nav.profile')">
                <ProfilePanel ref="profileRef" @dirty-change="dirty = $event" />
              </a-tab-pane>
              <a-tab-pane key="messages" tab="消息中心">
                <MessagePanel />
              </a-tab-pane>
              <a-tab-pane key="address" tab="收货地址">
                <AddressPanel />
              </a-tab-pane>
              <a-tab-pane key="loginlog" tab="登录记录">
                <LoginLogPanel />
              </a-tab-pane>
              <a-tab-pane key="settings" :tab="t('acct.nav.settings')">
                <SettingsPanel />
              </a-tab-pane>
            </a-tabs>
          </a-card>
        </section>

        <!-- 右栏：账号信息（≥1200 可见） -->
        <aside class="col-right">
          <a-card :bordered="false" class="card">
            <div class="right-title">{{ t('acct.card.accountInfo') }}</div>
            <div class="info-row"><span class="k">{{ t('acct.card.userId') }}</span><span class="v">{{ user.user?.userId }}</span></div>
            <div class="info-row"><span class="k">{{ t('acct.card.role') }}</span><span class="v">{{ user.user?.roleName }}</span></div>
            <div class="info-row"><span class="k">{{ t('acct.card.phone') }}</span><span class="v">{{ profile.profile?.phone }}</span></div>
            <div class="info-row"><span class="k">{{ t('acct.card.member') }}</span><span class="v">-</span></div>
          </a-card>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import ProfilePanel from '../components/account/ProfilePanel.vue'
import MessagePanel from '../components/account/MessagePanel.vue'
import AddressPanel from '../components/account/AddressPanel.vue'
import LoginLogPanel from '../components/account/LoginLogPanel.vue'
import SettingsPanel from '../components/account/SettingsPanel.vue'
import { useProfileStore } from '../store/profile'
import { useSettingsStore } from '../store/settings'
import { useUserStore } from '../store/user'
import { userApi } from '../api'
import { t } from '../i18n'
import { idbDel } from '../utils/idb'

const router = useRouter()
const route = useRoute()
const profile = useProfileStore()
const settings = useSettingsStore()
const user = useUserStore()

const VALID_TABS = ['profile', 'messages', 'address', 'loginlog', 'settings']
const activeTab = ref(VALID_TABS.includes(route.query.tab) ? route.query.tab : 'profile')
const profileRef = ref(null)
const dirty = ref(false)
const clearingCache = ref(false)

// 未保存修改守卫：切 tab / 离开页面
function confirmDiscard(nextTab) {
  Modal.confirm({
    title: t('acct.discardTitle'),
    content: t('acct.discardContent'),
    okText: t('acct.ok'),
    cancelText: t('acct.cancelText'),
    onOk: () => {
      profileRef.value?.discardChanges()
      dirty.value = false
      if (nextTab) activeTab.value = nextTab
    }
  })
}
function onTabChange(key) {
  if (key !== activeTab.value && dirty.value && key !== 'profile') {
    // 从个人信息切走且存在未保存修改
    confirmDiscard(key)
    if (activeTab.value !== key) activeTab.value = activeTab.value // 未确认则不切换
    return
  }
}

onBeforeRouteLeave(() => {
  if (dirty.value) {
    return new Promise((resolve) => {
      Modal.confirm({
        title: t('acct.discardTitle'),
        content: t('acct.leaveConfirm'),
        okText: t('acct.ok'),
        cancelText: t('acct.cancelText'),
        onOk: () => { profileRef.value?.discardChanges(); resolve(true) },
        onCancel: () => resolve(false)
      })
    })
  }
  return true
})

// 清空本地缓存（个人信息/设置/草稿），并上报审计
async function clearCache() {
  clearingCache.value = true
  try {
    await Promise.all([
      idbDel('profile').catch(() => {}),
      idbDel('settings').catch(() => {}),
      idbDel('ll_aes_key').catch(() => {})
    ])
    localStorage.removeItem('ll_draft_profile')
    userApi.audit('CACHE_CLEAR', '清除本地缓存').catch(() => {})
    message.success(t('acct.card.cacheCleared'), 3)
    reload()
  } finally { clearingCache.value = false }
}

function reload() {
  profile.load()
  settings.load().then(() => {}).catch(() => {})
}

onMounted(reload)
</script>

<style scoped>
.me-wrap { max-width: 1200px; margin: 0 auto; }
.avatar { background: var(--ll-ring-gradient); font-size: 22px; flex-shrink: 0; }
.user-row { display: flex; align-items: center; gap: 12px; }
.uname { font-size: 17px; font-weight: 700; color: var(--ll-ink); }
.urole { font-size: 13px; color: var(--ll-muted); margin-top: 2px; }
.quick { font-size: 13px; color: var(--ll-gray2); margin-bottom: 8px; }
.right-title { font-size: 14px; font-weight: 700; color: var(--ll-ink); margin-bottom: 10px; }
.info-row { display: flex; justify-content: space-between; font-size: 13px; padding: 5px 0; }
.info-row .k { color: var(--ll-muted); }
.info-row .v { color: var(--ll-ink); word-break: break-all; }

/* 响应式三栏：桌面 ≥1200 三栏 / 平板两栏 / 移动单栏 */
.grid { display: grid; gap: 16px; grid-template-columns: 280px 1fr 220px; align-items: start; }
.main-card { border-radius: 12px; }
.card { border-radius: 12px; }
.touch { min-height: 44px; }
@media (max-width: 1199px) and (min-width: 768px) {
  .grid { grid-template-columns: 280px 1fr; }
  .col-right { display: none; }
}
@media (max-width: 767px) {
  .grid { grid-template-columns: 1fr; }
  .col-right { display: none; }
}
</style>