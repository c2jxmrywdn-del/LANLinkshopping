<template>
  <div class="wrap">
    <a-card title="确认订单" class="left">
      <a-form :model="form" layout="vertical" @finish="submit">
        <a-form-item label="收货人" :rules="[{ required: true }]"><a-input v-model:value="form.receiver" /></a-form-item>
        <a-form-item label="联系电话" :rules="[{ required: true }]"><a-input v-model:value="form.phone" /></a-form-item>
        <a-form-item label="收货地址" :rules="[{ required: true }]"><a-textarea v-model:value="form.address" :rows="2" /></a-form-item>
        <a-form-item label="支付方式">
          <a-radio-group v-model:value="form.payType">
            <a-radio value="corporate">对公转账</a-radio>
            <a-radio value="balance">企业钱包</a-radio>
            <a-radio value="term">账期/尾款</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注"><a-input v-model:value="form.remark" /></a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">提交订单</a-button>
      </a-form>
    </a-card>
    <a-card title="订单商品" class="right">
      <div v-for="i in cart.checkedItems" :key="i.cartId" class="row">
        <span>{{ i.title }} × {{ i.quantity }}</span><span>¥{{ i.subtotal }}</span>
      </div>
      <a-divider />
      <div class="row total"><span>应付合计</span><b>¥{{ cart.total }}</b></div>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { orderApi } from '../api'
import { useCartStore } from '../store/cart'

const router = useRouter()
const cart = useCartStore()
const loading = ref(false)
const form = reactive({ receiver: '', phone: '', address: '', remark: '', payType: 'corporate' })

async function submit() {
  if (!cart.checkedItems.length) { message.warning('没有勾选的商品'); return }
  loading.value = true
  try {
    const order = await orderApi.checkout({
      cartIds: cart.checkedItems.map(i => i.cartId),
      receiver: form.receiver, phone: form.phone, address: form.address,
      remark: form.remark, payType: form.payType
    })
    await cart.load()
    Modal.confirm({
      title: '下单成功',
      content: `订单号 ${order.orderNo}，应付 ¥${order.totalAmount}。是否立即支付？`,
      okText: '立即支付', cancelText: '稍后支付',
      onOk: async () => { await orderApi.pay(order.orderNo); message.success('支付成功'); router.push('/orders') },
      onCancel: () => router.push('/orders')
    })
  } finally { loading.value = false }
}
</script>

<style scoped>
.wrap { display: flex; gap: 24px; align-items: flex-start; }
.left { flex: 1; } .right { width: 360px; }
.row { display: flex; justify-content: space-between; padding: 6px 0; }
.total b { color: #e4393c; font-size: 20px; }
</style>
