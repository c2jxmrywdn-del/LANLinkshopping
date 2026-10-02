<template>
  <div class="wrap">
    <a-card title="确认订单" class="left">
      <!-- 收货地址选择：来自个人中心，默认地址自动带入 -->
      <div class="addr-section" v-if="addrList.length">
        <div class="addr-title">收货地址 <span class="addr-sub">来自个人中心</span></div>
        <div class="addr-grid">
          <div v-for="a in addrList" :key="a.id"
               class="addr-card" :class="{ active: selectedAddrId === a.id }"
               @click="pickAddr(a)">
            <div class="addr-head">
              <span class="addr-name">{{ a.receiver }}</span>
              <span class="addr-phone">{{ a.phone }}</span>
              <a-tag v-if="a.isDefault === 1" color="red" class="addr-dtag">默认</a-tag>
            </div>
            <div class="addr-line">{{ a.region ? a.region + ' ' : '' }}{{ a.detail }}</div>
          </div>
        </div>
      </div>
      <a-alert v-else-if="addrLoaded" type="info" show-icon class="addr-empty"
               message="还没有收货地址"
               description="可前往个人中心 → 收货地址 添加，或在下方直接填写。" />

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
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { orderApi, userApi } from '../api'
import { useCartStore } from '../store/cart'

const router = useRouter()
const cart = useCartStore()
const loading = ref(false)
const form = reactive({ receiver: '', phone: '', address: '', remark: '', payType: 'corporate' })

// ===== 个人中心地址联动：默认地址自动带入，点击卡片一键填充 =====
const addrList = ref([])
const addrLoaded = ref(false)
const selectedAddrId = ref(null)

onMounted(async () => {
  try {
    addrList.value = (await userApi.addressList()) || []
  } catch (e) {
    /* 拦截器已提示；未登录等场景保持手工填写 */
  } finally {
    addrLoaded.value = true
  }
  const def = addrList.value.find(a => a.isDefault === 1) || addrList.value[0]
  if (def) applyAddr(def)
})

function applyAddr(a) {
  selectedAddrId.value = a.id
  form.receiver = a.receiver
  form.phone = a.phone
  form.address = (a.region ? a.region + ' ' : '') + a.detail
}

function pickAddr(a) {
  applyAddr(a)
  message.success(`已选用 ${a.receiver} 的地址`, 2)
}

async function submit() {
  if (!cart.checkedItems.length) { message.warning('没有勾选的商品'); return }
  if (!form.receiver.trim() || !form.phone.trim() || !form.address.trim()) {
    message.warning('请完整填写收货人、联系电话与收货地址'); return
  }
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
/* 地址选择区 */
.addr-section { margin-bottom: 20px; }
.addr-title { font-weight: 600; margin-bottom: 10px; }
.addr-sub { font-size: 12px; font-weight: 400; color: var(--ll-muted, #64748b); margin-left: 8px; }
.addr-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 10px; }
.addr-card { border: 1.5px solid #e5e7eb; border-radius: 10px; padding: 10px 12px; cursor: pointer;
             transition: border-color .2s ease, box-shadow .2s ease, background-color .2s ease; }
.addr-card:hover { border-color: var(--ll-skyblue-border, #5b8fb5); }
.addr-card.active { border-color: var(--ll-primary, #1e6eb8); background: rgba(30, 110, 184, .06);
                    box-shadow: 0 0 0 1px var(--ll-primary, #1e6eb8) inset; }
.addr-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.addr-name { font-weight: 600; }
.addr-phone { color: var(--ll-muted, #64748b); font-size: 13px; }
.addr-dtag { margin-left: auto; margin-right: 0; }
.addr-line { font-size: 13px; color: var(--ll-gray, #4b5563); }
.addr-empty { margin-bottom: 16px; }
</style>
