<template>
  <div>
    <h2>我的购物车</h2>
    <a-table :data-source="cart.items" :columns="cols" row-key="cartId" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'prod'">
          <a @click="$router.push('/product/' + record.prodId)">{{ record.title }}</a>
        </template>
        <template v-else-if="column.key === 'price'">¥{{ record.price }}</template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number :value="record.quantity" :min="1" :max="record.stock"
                          @change="(v) => cart.setQty(record.cartId, v)" />
        </template>
        <template v-else-if="column.key === 'sub'">¥{{ record.subtotal }}</template>
        <template v-else-if="column.key === 'op'">
          <a @click="cart.remove(record.cartId)">删除</a>
        </template>
      </template>
    </a-table>
    <div class="bar" v-if="cart.items.length">
      <span class="total">已选 {{ cart.checkedItems.length }} 件，合计 <b>¥{{ cart.total }}</b></span>
      <a-button type="primary" size="large" @click="$router.push('/checkout')">去结算</a-button>
    </div>
    <a-empty v-else description="购物车是空的">
      <a-button type="primary" @click="$router.push('/mall')">去逛逛</a-button>
    </a-empty>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useCartStore } from '../store/cart'
const cart = useCartStore()
const cols = [
  { title: '商品', key: 'prod' },
  { title: '单价', key: 'price', width: 120 },
  { title: '数量', key: 'qty', width: 140 },
  { title: '小计', key: 'sub', width: 120 },
  { title: '操作', key: 'op', width: 80 }
]
onMounted(() => cart.load())
</script>

<style scoped>
.bar { display: flex; justify-content: flex-end; align-items: center; gap: 24px; margin-top: 20px; padding: 16px; background: #fff; border-radius: 8px; }
.total { font-size: 16px; }
.total b { color: #e4393c; font-size: 22px; }
</style>
