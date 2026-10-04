<template>
  <div class="wallet-wrap">
    <!-- 余额卡 -->
    <a-card :bordered="false" class="w-balance">
      <div class="w-balance-inner">
        <div>
          <div class="w-label">钱包余额（元）</div>
          <div class="w-amount">¥{{ balanceText }}</div>
          <div class="w-hint">钱包支付实时到账 · 订单退款原路返回钱包</div>
        </div>
        <div class="w-actions">
          <a-button type="primary" size="large" @click="openRecharge">充值</a-button>
          <a-button size="large" @click="$router.push('/orders')">去支付订单</a-button>
        </div>
      </div>
    </a-card>

    <!-- 流水 -->
    <a-card :bordered="false" title="钱包流水" class="w-logs">
      <a-empty v-if="!loading && !logs.length" description="暂无流水记录" />
      <a-table v-else :data-source="logs" :columns="cols" row-key="id" :loading="loading"
               :pagination="{ pageSize: 10, showTotal: (t) => `共 ${t} 条` }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColor[record.changeType] || 'default'">{{ typeText[record.changeType] || record.changeType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span :class="record.amount >= 0 ? 'in' : 'out'">{{ record.amount >= 0 ? '+' : '' }}{{ record.amount }}</span>
          </template>
          <template v-else-if="column.key === 'after'">¥{{ record.balanceAfter }}</template>
          <template v-else-if="column.key === 'time'">{{ (record.createTime || '').replace('T', ' ') }}</template>
        </template>
      </a-table>
    </a-card>

    <!-- 充值弹窗 -->
    <a-modal v-model:open="rechargeOpen" title="钱包充值" @ok="doRecharge" :confirm-loading="recharging">
      <a-form layout="vertical">
        <a-form-item label="充值金额（元）" required>
          <a-input-number v-model:value="rechargeAmount" :min="0.01" :max="50000" :precision="2"
                          :step="100" style="width: 100%" placeholder="0.01 - 50000" />
          <div class="r-quick">
            <a-button size="small" v-for="q in [100, 500, 1000, 5000]" :key="q" @click="rechargeAmount = q">¥{{ q }}</a-button>
          </div>
        </a-form-item>
        <a-form-item label="备注（可选）">
          <a-input v-model:value="rechargeRemark" :maxlength="50" placeholder="例如：企业采购备用金" />
        </a-form-item>
        <a-alert type="info" show-icon message="演示环境：充值经模拟网关即时到账；真实渠道（微信/支付宝）需配置商户凭证后启用。" />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { walletApi } from '../api'

const balance = ref(0)
const logs = ref([])
const loading = ref(false)
const rechargeOpen = ref(false)
const rechargeAmount = ref(null)
const rechargeRemark = ref('')
const recharging = ref(false)

const balanceText = computed(() => Number(balance.value || 0).toFixed(2))
const typeText = { recharge: '充值', pay: '消费', refund: '退款入账' }
const typeColor = { recharge: 'blue', pay: 'orange', refund: 'green' }
const cols = [
  { title: '类型', key: 'type', width: 110 },
  { title: '金额（元）', key: 'amount', width: 120 },
  { title: '变动后余额', key: 'after', width: 120 },
  { title: '关联订单', dataIndex: 'refOrderNo', width: 180 },
  { title: '备注', dataIndex: 'remark' },
  { title: '时间', key: 'time', width: 170 }
]

async function load() {
  loading.value = true
  try {
    const d = await walletApi.my()
    balance.value = d?.balance || 0
    logs.value = d?.logs || []
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

function openRecharge() {
  rechargeAmount.value = null
  rechargeRemark.value = ''
  rechargeOpen.value = true
}

async function doRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    message.warning('请输入有效的充值金额'); return
  }
  recharging.value = true
  try {
    const d = await walletApi.recharge(rechargeAmount.value, rechargeRemark.value)
    balance.value = d?.balance ?? balance.value
    message.success(`充值成功，当前余额 ¥${Number(balance.value).toFixed(2)}`)
    rechargeOpen.value = false
    await load()
  } finally { recharging.value = false }
}

onMounted(load)
</script>

<style scoped>
.wallet-wrap { max-width: 900px; margin: 0 auto; }
.w-balance { border-radius: 12px; margin-bottom: 16px;
             background: linear-gradient(135deg, rgba(30, 110, 184, .08), rgba(34, 211, 238, .08)); }
.w-balance-inner { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.w-label { font-size: 13px; color: var(--ll-muted, #64748b); }
.w-amount { font-size: 38px; font-weight: 800; color: var(--ll-primary, #1e6eb8); margin: 6px 0; }
.w-hint { font-size: 12px; color: var(--ll-muted, #64748b); }
.w-actions { display: flex; gap: 12px; }
.w-logs { border-radius: 12px; }
.in { color: #10b981; font-weight: 700; }
.out { color: #e4393c; font-weight: 700; }
.r-quick { margin-top: 8px; display: flex; gap: 8px; }
</style>
