<template>
  <div>
    <div class="toolbar">
      <a-input-search v-model:value="keyword" placeholder="搜索商品名称" style="width:320px"
                      allow-clear @search="reload" />
      <a-select v-model:value="indId" style="width:160px" placeholder="全部行业" allow-clear @change="reload">
        <a-select-option v-for="i in industries" :key="i.indId" :value="i.indId">{{ i.name }}</a-select-option>
      </a-select>
      <a-select v-model:value="sort" style="width:160px" @change="reload">
        <a-select-option value="">综合排序</a-select-option>
        <a-select-option value="sales">销量优先</a-select-option>
        <a-select-option value="priceAsc">价格升序</a-select-option>
        <a-select-option value="priceDesc">价格降序</a-select-option>
      </a-select>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <a-col :span="6" v-for="p in list" :key="p.prodId">
          <a-card hoverable class="prod-card" @click="$router.push('/product/' + p.prodId)">
            <div class="thumb">
              <img v-if="p.coverUrl" :src="p.coverUrl" :alt="p.title" loading="lazy" decoding="async" />
              <span v-else>{{ p.title.slice(0, 2) }}</span>
            </div>
            <div class="ptitle">{{ p.title }}</div>
            <div class="pprice">¥{{ p.price }}</div>
            <div class="pmeta">{{ p.brand }} · 销量 {{ p.sales }}</div>
            <a-button class="ll-add-cart" type="primary" size="small" block @click.stop="addCart(p)">加入购物车</a-button>
          </a-card>
        </a-col>
      </a-row>
      <a-empty v-if="!loading && list.length === 0" description="暂无商品" />
    </a-spin>

    <div class="pager">
      <a-pagination v-model:current="current" :total="total" :page-size="size" @change="reload" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { productApi, homeApi } from '../api'
import { useCartStore } from '../store/cart'
import { useUserStore } from '../store/user'

const route = useRoute()
const cart = useCartStore()
const user = useUserStore()

const industries = ref([])
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const indId = ref(route.query.indId ? Number(route.query.indId) : undefined)
const sort = ref('')
const current = ref(1)
const size = 12
const total = ref(0)

async function reload() {
  loading.value = true
  try {
    const page = await productApi.page({ current: current.value, size, keyword: keyword.value, indId: indId.value, sort: sort.value })
    list.value = page.records
    total.value = page.total
  } finally { loading.value = false }
}
async function addCart(p) {
  if (!user.logged) { message.warning('请先登录'); return }
  await cart.add(p.prodId, 1)
  message.success('已加入购物车')
}
onMounted(async () => {
  industries.value = await homeApi.industries()
  reload()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.prod-card { margin-bottom: 16px; }
.thumb { height: 120px; background: var(--ll-thumb-bg); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 40px; color: var(--ll-thumb-fg); margin-bottom: 8px; overflow: hidden; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.ptitle { font-size: 14px; height: 40px; overflow: hidden; }
.pprice { color: #e4393c; font-weight: 700; font-size: 18px; margin: 4px 0; }
.pmeta { color: #999; font-size: 12px; margin-bottom: 8px; }
.pager { text-align: center; margin: 24px 0; }
</style>
