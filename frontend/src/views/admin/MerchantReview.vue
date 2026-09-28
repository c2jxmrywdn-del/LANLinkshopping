<template>
  <a-card title="商户入驻审核">
    <template #extra>
      <a-radio-group v-model:value="filter" button-style="solid" @change="load">
        <a-radio-button :value="null">全部</a-radio-button>
        <a-radio-button :value="0">待审核</a-radio-button>
        <a-radio-button :value="1">已通过</a-radio-button>
        <a-radio-button :value="2">已驳回</a-radio-button>
      </a-radio-group>
    </template>
    <a-table :data-source="list" :columns="cols" row-key="merId" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cap'">¥{{ record.regCapital }}</template>
        <template v-else-if="column.key === 'tax'">
          <a-tag :color="record.taxStatus === 1 ? 'green' : 'default'">{{ record.taxStatus === 1 ? '稳定' : '无' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'st'">
          <a-tag :color="stColor[record.reviewStatus]">{{ stText[record.reviewStatus] }}</a-tag>
          <div v-if="record.rejectReason" class="reason">{{ record.rejectReason }}</div>
        </template>
        <template v-else-if="column.key === 'op'">
          <a-space v-if="record.reviewStatus !== 1">
            <a-button size="small" type="primary" @click="review(record, 1)">通过</a-button>
          </a-space>
          <a-space v-if="record.reviewStatus !== 2">
            <a-button size="small" danger @click="review(record, 2)">驳回</a-button>
          </a-space>
          <span v-if="record.reviewStatus === 1" style="color:#999">已入驻</span>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { merchantApi } from '../../api'

const list = ref([])
const loading = ref(false)
const filter = ref(null)
const stText = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const stColor = { 0: 'orange', 1: 'green', 2: 'red' }
const cols = [
  { title: '商户ID', dataIndex: 'merId', width: 80 },
  { title: '企业ID', dataIndex: 'entId', width: 80 },
  { title: '主体类型', dataIndex: 'regType', width: 110 },
  { title: '注册资本', key: 'cap', width: 130 },
  { title: '纳税', key: 'tax', width: 90 },
  { title: '方式', dataIndex: 'joinType', width: 80 },
  { title: '状态', key: 'st', width: 200 },
  { title: '操作', key: 'op', width: 160 }
]

async function load() {
  loading.value = true
  try { list.value = await merchantApi.adminList(filter.value) || [] }
  finally { loading.value = false }
}
async function review(record, status) {
  const reason = status === 2 ? '人工复核未通过' : ''
  await merchantApi.adminReview(record.merId, status, reason)
  message.success(status === 1 ? '已通过' : '已驳回')
  load()
}
onMounted(load)
</script>

<style scoped>
.reason { color: #e4393c; font-size: 12px; margin-top: 2px; }
</style>
