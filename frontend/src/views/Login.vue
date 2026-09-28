<template>
  <div class="wrap">
    <a-card class="box">
      <template #title>登录 <span class="wordmark">LAN<span class="ll-link">Link</span>shopping</span></template>
      <a-form :model="form" layout="vertical" @finish="submit">
        <a-form-item label="手机号" name="phone" :rules="[{ required: true, message: '请输入手机号' }]">
          <a-input v-model:value="form.phone" placeholder="演示: 13900000001" size="large" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" placeholder="演示密码: 123456" size="large" />
        </a-form-item>
        <a-button type="primary" html-type="submit" block size="large" :loading="loading">登录</a-button>
        <div class="tip">还没有账号？<a @click="$router.push('/register')">立即注册</a></div>
      </a-form>
      <a-divider>演示账号（密码均 123456）</a-divider>
      <a-space direction="vertical" style="width:100%">
        <a-button block @click="quick('13800000000')">平台运营 13800000000</a-button>
        <a-button block @click="quick('13900000001')">采购方 13900000001</a-button>
        <a-button block @click="quick('13700000002')">商户 13700000002</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'

const router = useRouter(); const route = useRoute()
const user = useUserStore(); const cart = useCartStore()
const loading = ref(false)
const form = reactive({ phone: '', password: '123456' })

function quick(p) { form.phone = p; form.password = '123456'; submit() }
async function submit() {
  if (!form.phone) return
  loading.value = true
  try {
    await user.login({ phone: form.phone, password: form.password })
    await cart.load()
    message.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally { loading.value = false }
}
</script>

<style scoped>
.wrap { display: flex; justify-content: center; align-items: center; min-height: 70vh; }
.box { width: 380px; }
.tip { text-align: center; margin-top: 12px; color: var(--ll-gray2); }
</style>
