<template>
  <a-spin :spinning="!p">
    <a-row :gutter="32" v-if="p">
      <a-col :span="10">
        <div class="bigthumb">
          <img v-if="p.coverUrl" :src="p.coverUrl" :alt="p.title" loading="lazy" decoding="async" />
          <span v-else>{{ p.title.slice(0, 2) }}</span>
        </div>
      </a-col>
      <a-col :span="14">
        <h1 class="title">{{ p.title }}</h1>
        <div class="price">¥{{ p.price }}</div>
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="品牌">{{ p.brand }}</a-descriptions-item>
          <a-descriptions-item label="规格">{{ p.spec }}</a-descriptions-item>
          <a-descriptions-item label="库存">{{ p.stock }}</a-descriptions-item>
          <a-descriptions-item label="累计销量">{{ p.sales }}</a-descriptions-item>
        </a-descriptions>
        <div class="qty">
          数量：<a-input-number v-model:value="qty" :min="1" :max="p.stock" />
        </div>
        <a-space>
          <a-button class="ll-add-cart" size="large" @click="addCart">加入购物车</a-button>
          <a-button size="large" type="primary" @click="buyNow">立即购买</a-button>
        </a-space>
        <div class="detail"><h3>商品详情</h3><p>{{ p.detail }}</p></div>
      </a-col>
    </a-row>
  </a-spin>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { productApi } from '../api'
import { useCartStore } from '../store/cart'
import { useUserStore } from '../store/user'

const route = useRoute(); const router = useRouter()
const cart = useCartStore(); const user = useUserStore()
const p = ref(null)
const qty = ref(1)

onMounted(async () => { p.value = await productApi.detail(route.params.id) })

function needLogin() { if (!user.logged) { message.warning('请先登录'); router.push('/login'); return true } return false }
async function addCart() { if (needLogin()) return; await cart.add(p.value.prodId, qty.value); message.success('已加入购物车') }
async function buyNow() { if (needLogin()) return; await cart.add(p.value.prodId, qty.value); router.push('/cart') }
</script>

<style scoped>
.bigthumb { height: 360px; background: var(--ll-thumb-bg); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 90px; color: var(--ll-thumb-fg); overflow: hidden; }
.bigthumb img { width: 100%; height: 100%; object-fit: contain; }
.title { font-size: 22px; }
.price { color: #e4393c; font-size: 32px; font-weight: 700; margin: 8px 0 16px; }
.qty { margin: 16px 0; }
.detail { margin-top: 24px; color: #555; }
</style>
