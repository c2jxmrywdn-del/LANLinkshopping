<template>
  <a-card title="商品管理">
    <template #extra>
      <a-space>
        <a-input-search v-model:value="keyword" placeholder="搜索商品名" style="width:200px" @search="load" allow-clear />
        <a-select v-model:value="status" style="width:130px" placeholder="全部状态" allow-clear @change="load">
          <a-select-option :value="1">已上架</a-select-option>
          <a-select-option :value="0">已下架</a-select-option>
        </a-select>
      </a-space>
    </template>
    <a-table :data-source="rows" :columns="cols" row-key="prodId" :loading="loading"
             :pagination="{ current, pageSize: 20, total, onChange: (p) => { current = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">¥{{ record.price }}</template>
        <template v-else-if="column.key === 'st'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '已上架' : '已下架' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'op'">
          <a-button v-if="record.status === 1" size="small" danger @click="toggle(record, 0)">下架</a-button>
          <a-button v-else size="small" type="primary" @click="toggle(record, 1)">上架</a-button>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { productApi } from '../../api'

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const status = ref(null)
const current = ref(1)
const total = ref(0)
const cols = [
  { title: 'ID', dataIndex: 'prodId', width: 70 },
  { title: '商品名称', dataIndex: 'title' },
  { title: '品牌', dataIndex: 'brand', width: 90 },
  { title: '价格', key: 'price', width: 100 },
  { title: '库存', dataIndex: 'stock', width: 90 },
  { title: '销量', dataIndex: 'sales', width: 90 },
  { title: '状态', key: 'st', width: 100 },
  { title: '操作', key: 'op', width: 100 }
]

async function load() {
  loading.value = true
  try {
    const page = await productApi.adminList({ current: current.value, size: 20, status: status.value, keyword: keyword.value })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally { loading.value = false }
}
async function toggle(record, st) {
  await productApi.adminStatus(record.prodId, st)
  message.success(st === 1 ? '已上架' : '已下架')
  load()
}
onMounted(load)
</script>
