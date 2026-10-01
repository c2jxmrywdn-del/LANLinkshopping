<template>
  <div>
    <BannerCarousel />

    <h2 class="sec">行业解决方案</h2>
    <a-row :gutter="16">
      <a-col :span="6" v-for="ind in industries" :key="ind.indId">
        <a-card hoverable class="ind-card" @click="$router.push({ name: 'mall', query: { indId: ind.indId } })">
          <div class="ind-name" :title="ind.name">{{ ind.name }}</div>
          <div class="ind-desc" :title="'查看'+ind.name+'专区商品'">查看{{ ind.name }}专区商品 →</div>
        </a-card>
      </a-col>
    </a-row>

    <h2 class="sec">热销推荐</h2>
    <a-row :gutter="16">
      <a-col :span="6" v-for="p in hot" :key="p.prodId">
        <a-card hoverable class="prod-card" :cover="null" @click="$router.push('/product/' + p.prodId)">
          <div class="thumb">
            <img v-if="p.coverUrl" :src="p.coverUrl" :alt="p.title" loading="lazy" decoding="async" />
            <span v-else>{{ p.title.slice(0, 2) }}</span>
          </div>
          <div class="ptitle">{{ p.title }}</div>
          <div class="pprice">¥{{ p.price }}</div>
          <div class="pmeta">销量 {{ p.sales }} · 库存 {{ p.stock }}</div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { homeApi } from '../api'
import BannerCarousel from '../components/BannerCarousel.vue'
const industries = ref([])
const hot = ref([])
onMounted(async () => {
  industries.value = await homeApi.industries()
  hot.value = await homeApi.hot(8)
})
</script>

<style scoped>
.sec { margin: 24px 0 12px; }
.ind-card { text-align: center; height: 116px; margin-bottom: 16px; }
.ind-card :deep(.ant-card-body) { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 12px 16px; }
.ind-name { font-size: 18px; font-weight: 700; color: var(--ll-navy); max-width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ind-desc { color: var(--ll-muted); font-size: 13px; margin-top: 6px; max-width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.prod-card { margin-bottom: 16px; }
.thumb { height: 120px; background: var(--ll-thumb-bg); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 40px; color: var(--ll-thumb-fg); margin-bottom: 8px; overflow: hidden; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.ptitle { font-size: 14px; height: 40px; overflow: hidden; }
.pprice { color: var(--ll-price); font-weight: 700; font-size: 18px; margin-top: 4px; }
.pmeta { color: var(--ll-gray); font-size: 12px; }
</style>
