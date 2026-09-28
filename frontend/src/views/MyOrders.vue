<template>
  <div>
    <h2>我的订单</h2>
    <a-table :data-source="orders" :columns="cols" row-key="orderNo" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'total'">¥{{ record.totalAmount }}</template>
        <template v-else-if="column.key === 'pay'">
          <a-tag :color="record.payStatus === 1 ? 'green' : 'orange'">{{ record.payStatus === 1 ? '已支付' : '未支付' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag>{{ statusMap[record.orderStatus] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'op'">
          <a-space>
            <a v-if="record.payStatus !== 1" @click="pay(record)">支付</a>
            <a v-if="record.payStatus !== 1" @click="cancel(record)" style="color:#999">取消</a>
          </a-space>
        </template>
      </template>
    </a-table>
    <a-empty v-if="!loading && orders.length === 0" description="还没有订单">
      <a-button type="primary" @click="$router.push('/mall')">去下单</a-button>
    </a-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { orderApi } from '../api'

const orders = ref([])
const loading = ref(false)
const statusMap = { 0: '待发货', 1: '已发货', 2: '已完成', 3: '已取消' }
const cols = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '金额', key: 'total', width: 120 },
  { title: '支付方式', dataIndex: 'payType', width: 120 },
  { title: '支付状态', key: 'pay', width: 110 },
  { title: '订单状态', key: 'status', width: 110 },
  { title: '操作', key: 'op', width: 120 }
]
async function load() { loading.value = true; try { orders.value = await orderApi.my() } finally { loading.value = false } }
async function pay(r) { await orderApi.pay(r.orderNo); message.success('支付成功'); load() }
async function cancel(r) { await orderApi.cancel(r.orderNo); message.success('已取消'); load() }
onMounted(load)
</script>
