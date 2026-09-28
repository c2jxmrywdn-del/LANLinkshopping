<template>
  <div class="wrap">
    <a-card title="个人中心" style="width:480px">
      <a-descriptions :column="1" bordered v-if="user.user">
        <a-descriptions-item label="昵称">{{ user.user.nickname }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ user.user.phone }}</a-descriptions-item>
        <a-descriptions-item label="身份">{{ user.user.roleName }}</a-descriptions-item>
      </a-descriptions>
      <a-space direction="vertical" style="width:100%;margin-top:16px">
        <a-button block @click="$router.push('/orders')">我的订单</a-button>
        <a-button block @click="$router.push('/cart')">我的购物车</a-button>
        <a-button block @click="$router.push('/merchant')">商户入驻</a-button>
        <a-button block danger @click="logout">退出登录</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
const router = useRouter()
const user = useUserStore()
async function logout() { await user.logout(); router.push('/login') }
</script>

<style scoped>
.wrap { display: flex; justify-content: center; padding-top: 20px; }
</style>
