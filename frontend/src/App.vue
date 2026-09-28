<template>
  <router-view />
  <SplashScreen v-if="showSplash" @done="showSplash = false" />
</template>

<script setup>
import { ref, watch } from 'vue'
import SplashScreen from './components/SplashScreen.vue'

const showSplash = ref(true)
// 动画期间锁定页面滚动
watch(showSplash, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
}, { immediate: true })
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, "Microsoft YaHei", "PingFang SC", sans-serif; background: #f5f6f8; }
a { text-decoration: none; color: inherit; }
</style>
