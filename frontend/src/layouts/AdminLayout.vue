<template>
  <a-layout class="admin-root">
    <a-layout-sider :width="220" theme="dark" class="sider">
      <div class="brand">
        <div class="brand-txt wordmark">LAN<span class="ll-link">Link</span></div>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline" @click="onNav">
        <a-menu-item key="admin-dash">数据概览</a-menu-item>
        <a-menu-item key="admin-merchants">商户管理</a-menu-item>
        <a-menu-item key="admin-products">商品管理</a-menu-item>
        <a-menu-item key="admin-payments">交易管理</a-menu-item>
        <a-menu-item key="admin-audit">审计日志</a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="admin-header">
        <span class="title"><span class="wordmark">LAN<span class="ll-link">Link</span>shopping</span> 管理后台</span>
        <a-space>
          <span class="who">{{ user.user && user.user.nickname }}（平台运营）</span>
          <a-button size="small" @click="backFront">返回前台</a-button>
          <a-button size="small" danger @click="logout">退出</a-button>
        </a-space>
      </a-layout-header>
      <a-layout-content class="admin-content">
        <main><router-view /></main>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const selectedKeys = ref(['admin-dash'])

watch(() => route.name, (n) => { if (n) selectedKeys.value = [n] }, { immediate: true })

function onNav({ key }) { router.push({ name: key }) }
function backFront() { router.push('/') }
async function logout() { await user.logout(); router.push('/login') }
</script>

<style scoped>
.admin-root { min-height: 100vh; }
.sider { box-shadow: 2px 0 8px rgba(0,0,0,.2); }
.brand { padding: 20px 24px 16px; }
.brand-txt {
  font-size: 22px; font-weight: 800; letter-spacing: .5px; line-height: 1.2;
  color: #f1f5f9;
}
:deep(.ant-menu-dark .ant-menu-item) { letter-spacing: 1px; font-size: 14px; }
:deep(.ant-menu-dark .ant-menu-item-selected) { background: var(--ll-accent-gradient); }
.admin-header { background: #fff; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,21,41,.08); }
.admin-header .title { font-size: 16px; font-weight: 600; }
.admin-header .who { color: #666; }
.admin-content { padding: 24px; background: #f0f2f5; }
</style>
