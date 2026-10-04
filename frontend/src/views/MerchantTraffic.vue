<template>
  <div class="traffic-wrap">
    <!-- 概览指标（含环比/客单价/动销率） -->
    <a-row :gutter="16">
      <a-col :xs="12" :md="8" :lg="6" v-for="m in metrics" :key="m.key">
        <a-card :bordered="false" class="t-metric">
          <div class="t-label">{{ m.label }}</div>
          <div class="t-num">{{ m.value }}</div>
          <div v-if="m.sub" class="t-sub" :class="m.subClass">{{ m.sub }}</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 近 N 天趋势：销售额/订单量切换 + 补零连续 -->
    <a-card :bordered="false" title="销售趋势" class="t-card">
      <template #extra>
        <a-space>
          <a-radio-group v-model:value="metric" button-style="solid" size="small">
            <a-radio-button value="amount">销售额</a-radio-button>
            <a-radio-button value="orders">订单量</a-radio-button>
          </a-radio-group>
          <a-radio-group v-model:value="days" button-style="solid" size="small" @change="loadTrend">
            <a-radio-button :value="7">7 天</a-radio-button>
            <a-radio-button :value="30">30 天</a-radio-button>
            <a-radio-button :value="90">90 天</a-radio-button>
          </a-radio-group>
        </a-space>
      </template>
      <a-empty v-if="!trend.length" description="暂无销售数据" />
      <div v-else class="t-chart" @mouseleave="hoverIdx = -1">
        <div v-for="(d, i) in trend" :key="d.date" class="t-bar-col"
             @mouseenter="hoverIdx = i">
          <div class="t-tip" v-if="hoverIdx === i">
            <div>{{ d.date }}</div>
            <div>¥{{ fmt(d.amount) }} · {{ d.orders }} 单</div>
          </div>
          <div class="t-bar" :class="{ dim: hoverIdx !== -1 && hoverIdx !== i }"
               :style="{ height: barHeight(d) }"></div>
        </div>
      </div>
      <div v-if="trend.length" class="t-axis">
        <span>{{ trend[0].date }}</span>
        <span v-if="hoverIdx >= 0" class="t-axis-hover">{{ trend[hoverIdx].date }}：¥{{ fmt(trend[hoverIdx].amount) }} / {{ trend[hoverIdx].orders }} 单</span>
        <span v-else>窗口峰值：{{ metric === 'amount' ? '¥' + fmt(maxAmount) : maxOrders + ' 单' }}</span>
        <span>{{ trend[trend.length - 1].date }}</span>
      </div>
    </a-card>

    <!-- 支付渠道分布（渠道管理 / 转化优化） -->
    <a-card :bordered="false" title="支付渠道分布（近 30 天成交）" class="t-card">
      <a-empty v-if="!channels.length" description="暂无成交数据" />
      <div v-else class="ch-list">
        <div v-for="c in channels" :key="c.channel" class="ch-row">
          <a-tag :color="channelColor[c.channel] || 'default'" class="ch-tag">{{ channelText[c.channel] || c.channel }}</a-tag>
          <div class="ch-bar-wrap">
            <div class="ch-bar" :style="{ width: chPct(c.amount) }"></div>
          </div>
          <div class="ch-num">{{ c.orders }} 单 · ¥{{ fmt(c.amount) }}（{{ chPctVal(c.amount) }}%）</div>
        </div>
      </div>
      <div v-if="channels.length" class="ch-tip">
        转化建议：占比过高的单一渠道意味着支付方式集中风险，可在结算页引导开通其他渠道分散资金路径。
      </div>
    </a-card>

    <!-- 商品排行（动销标记） -->
    <a-card :bordered="false" title="商品销量排行" class="t-card">
      <template #extra>
        <a-tag>动销率 {{ ov?.activeRatePct ?? 0 }}%（动销 {{ ov?.activeProducts ?? 0 }} / 在售 {{ ov?.onSaleProducts ?? 0 }}）</a-tag>
      </template>
      <a-empty v-if="!products.length" description="还没有商品数据" />
      <a-table v-else :data-source="products" :columns="cols" row-key="prodId" :loading="loading"
               :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rank'">
            <span :class="['t-rank', { 't-rank-1': products.indexOf(record) === 0 }]">#{{ products.indexOf(record) + 1 }}</span>
          </template>
          <template v-else-if="column.key === 'prod'">
            <div class="t-prod">
              <img v-if="record.coverUrl" :src="record.coverUrl" class="t-cover" alt="" loading="lazy" />
              <span>{{ record.title }}</span>
              <a-tag v-if="Number(record.sold) === 0" color="default" style="margin-left:auto">滞销</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'price'">¥{{ record.price }}</template>
          <template v-else-if="column.key === 'amount'">
            <b class="t-amount">¥{{ fmt(record.amount) }}</b>
            <div class="t-bar-mini" :style="{ width: amountPct(record.amount) }"></div>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { trafficApi } from '../api'

const ov = ref(null)
const trend = ref([])
const channels = ref([])
const products = ref([])
const loading = ref(false)
const days = ref(30)
const metric = ref('amount')
const hoverIdx = ref(-1)

const fmt = (n) => Number(n || 0).toFixed(2)

const chain = computed(() => ov.value?.chainGrowthPct)
const metrics = computed(() => [
  {
    key: 'recentAmount', label: '近30天销售额', value: '¥' + fmt(ov.value?.recentAmount),
    sub: chain.value === null || chain.value === undefined ? '环比 —' :
      `环比 ${chain.value >= 0 ? '↑' : '↓'} ${Math.abs(Number(chain.value)).toFixed(1)}%`,
    subClass: chain.value >= 0 ? 'up' : 'down'
  },
  { key: 'recentOrders', label: '近30天订单', value: ov.value?.recentOrders ?? 0 },
  { key: 'avgOrder', label: '客单价', value: '¥' + fmt(ov.value?.avgOrderValue) },
  {
    key: 'activeRate', label: '动销率', value: (ov.value?.activeRatePct ?? 0) + '%',
    sub: `动销 ${ov.value?.activeProducts ?? 0} / 在售 ${ov.value?.onSaleProducts ?? 0}`
  },
  { key: 'totalAmount', label: '累计销售额', value: '¥' + fmt(ov.value?.totalAmount) },
  { key: 'totalSold', label: '累计销量', value: ov.value?.totalSold ?? 0 }
])

const cols = [
  { title: '排名', key: 'rank', width: 70 },
  { title: '商品', key: 'prod' },
  { title: '单价', key: 'price', width: 110 },
  { title: '销量', dataIndex: 'sold', width: 90 },
  { title: '销售额', key: 'amount', width: 200 },
  { title: '库存', dataIndex: 'stock', width: 90 }
]

const channelText = { wallet: '钱包', mock: '模拟', wechat: '微信', alipay: '支付宝', unknown: '其他' }
const channelColor = { wallet: 'blue', mock: 'default', wechat: 'green', alipay: 'blue', unknown: 'default' }

const maxAmount = computed(() => fmt(Math.max(...trend.value.map(d => Number(d.amount || 0)), 0)))
const maxOrders = computed(() => Math.max(...trend.value.map(d => Number(d.orders || 0)), 0))
const maxMetric = computed(() => metric.value === 'amount'
  ? Math.max(...trend.value.map(d => Number(d.amount || 0)), 0.01)
  : Math.max(...trend.value.map(d => Number(d.orders || 0)), 1))

function barHeight(d) {
  const v = metric.value === 'amount' ? Number(d.amount || 0) : Number(d.orders || 0)
  return Math.max(4, (v / maxMetric.value) * 100) + '%'
}

const chTotal = computed(() => channels.value.reduce((s, c) => s + Number(c.amount || 0), 0))
function chPctVal(amount) {
  if (!chTotal.value) return '0.0'
  return ((Number(amount || 0) / chTotal.value) * 100).toFixed(1)
}
function chPct(amount) { return Math.max(2, Number(chPctVal(amount))) + '%' }

function amountPct(amount) {
  const max = Math.max(...products.value.map(p => Number(p.amount || 0)), 0.01)
  return Math.max(3, (Number(amount || 0) / max) * 100) + '%'
}

async function loadAll() {
  loading.value = true
  try {
    const [o, pr] = await Promise.all([trafficApi.overview(), trafficApi.products()])
    ov.value = o
    products.value = pr || []
    await Promise.all([loadTrend(), loadChannels()])
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

async function loadTrend() {
  try { trend.value = await trafficApi.trend(days.value) || [] } catch (e) { trend.value = [] }
}
async function loadChannels() {
  try { channels.value = await trafficApi.channels(30) || [] } catch (e) { channels.value = [] }
}

onMounted(loadAll)
</script>

<style scoped>
.traffic-wrap { max-width: 1100px; margin: 0 auto; }
.t-metric { border-radius: 12px; margin-bottom: 16px; }
.t-label { font-size: 12px; color: var(--ll-muted, #64748b); }
.t-num { font-size: 24px; font-weight: 800; color: var(--ll-primary, #1e6eb8); margin-top: 4px; }
.t-sub { font-size: 12px; margin-top: 2px; }
.t-sub.up { color: #10b981; }
.t-sub.down { color: #e4393c; }
.t-card { border-radius: 12px; margin-bottom: 16px; }
/* 趋势条形图：hover 明细 + 其余柱淡出 */
.t-chart { display: flex; align-items: flex-end; gap: 3px; height: 170px; padding: 8px 4px 0;
           border-bottom: 1px solid #e5e7eb; }
.t-bar-col { flex: 1; display: flex; align-items: flex-end; height: 100%; position: relative; }
.t-bar { width: 100%; border-radius: 3px 3px 0 0; min-height: 4px;
         background: var(--ll-accent-gradient, linear-gradient(180deg, #22d3ee, #1e6eb8));
         transition: height .3s ease, opacity .2s ease; }
.t-bar.dim { opacity: .35; }
.t-tip { position: absolute; bottom: calc(100% + 6px); left: 50%; transform: translateX(-50%);
         background: #0f172a; color: #fff; font-size: 12px; padding: 6px 10px; border-radius: 6px;
         white-space: nowrap; z-index: 5; pointer-events: none; }
.t-axis { display: flex; justify-content: space-between; font-size: 12px; color: var(--ll-muted, #64748b); padding-top: 6px; min-height: 22px; }
.t-axis-hover { color: var(--ll-primary, #1e6eb8); font-weight: 600; }
/* 渠道分布 */
.ch-list { display: flex; flex-direction: column; gap: 10px; }
.ch-row { display: flex; align-items: center; gap: 12px; }
.ch-tag { width: 64px; text-align: center; flex-shrink: 0; }
.ch-bar-wrap { flex: 1; height: 12px; background: var(--ll-page, #f0f2f5); border-radius: 6px; overflow: hidden; }
.ch-bar { height: 100%; border-radius: 6px; background: var(--ll-accent-gradient, linear-gradient(90deg, #1e6eb8, #22d3ee));
          transition: width .4s ease; min-width: 4px; }
.ch-num { font-size: 12px; color: var(--ll-muted, #64748b); width: 240px; text-align: right; flex-shrink: 0; }
.ch-tip { margin-top: 12px; font-size: 12px; color: var(--ll-muted, #64748b);
          background: rgba(30, 110, 184, .05); border-radius: 8px; padding: 8px 12px; }
/* 排行 */
.t-rank { font-weight: 800; color: var(--ll-muted, #64748b); }
.t-rank-1 { color: #b45309; }
.t-prod { display: flex; align-items: center; gap: 8px; }
.t-cover { width: 36px; height: 36px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.t-amount { color: var(--ll-ink, #0f172a); }
.t-bar-mini { height: 4px; border-radius: 2px; margin-top: 4px;
              background: var(--ll-accent-gradient, linear-gradient(90deg, #1e6eb8, #22d3ee)); }
@media (max-width: 767px) { .ch-num { width: auto; } }
</style>
