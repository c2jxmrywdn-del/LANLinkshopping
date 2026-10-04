<template>
  <a-config-provider :theme="{ algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm, token: { colorPrimary: '#1E6EB8', colorLink: '#1E6EB8' } }">
    <router-view />
    <SplashScreen v-if="showSplash" @done="showSplash = false" />
  </a-config-provider>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { theme } from 'ant-design-vue'
import SplashScreen from './components/SplashScreen.vue'
import { useUserStore } from './store/user'
import { useSettingsStore } from './store/settings'
import { fetchCsrfToken } from './api/request'

const showSplash = ref(true)
// 动画期间锁定页面滚动
watch(showSplash, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
}, { immediate: true })

const user = useUserStore()
const settings = useSettingsStore()
const isDark = ref(false)
watch(() => settings.isDark, (v) => { isDark.value = !!v }, { immediate: true })

// 已登录时：预取 CSRF 令牌 + 加载系统设置（主题/字号/语言实时应用）
async function bootstrapLogged() {
  if (!user.logged) return
  try { await fetchCsrfToken() } catch (e) { /* 令牌获取失败不影响浏览 */ }
  settings.load().catch(() => {})
}
watch(() => user.logged, (v) => { if (v) bootstrapLogged() })
onMounted(() => { if (user.logged) bootstrapLogged() })
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, "Microsoft YaHei", "PingFang SC", sans-serif; background: #f5f6f8; overflow-x: hidden; }
a { text-decoration: none; color: inherit; }
</style>