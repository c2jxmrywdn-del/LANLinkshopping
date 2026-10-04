<template>
  <div class="traffic-wrap">
    <div class="head"><div><h2>流量管理</h2><span>统一查看来源、商品行为、转化与异常诊断</span></div><a-radio-group v-model:value="days" button-style="solid" @change="reload"><a-radio-button :value="7">7天</a-radio-button><a-radio-button :value="30">30天</a-radio-button><a-radio-button :value="90">90天</a-radio-button></a-radio-group></div>
    <a-tabs v-model:activeKey="tab">
      <a-tab-pane key="overview" tab="流量总览">
        <a-row :gutter="16"><a-col v-for="m in metrics" :key="m.label" :xs="12" :lg="6"><a-card :bordered="false" class="metric"><div>{{m.label}}</div><strong>{{m.value}}</strong><small :class="m.cls">{{m.sub}}</small></a-card></a-col></a-row>
        <a-card :bordered="false" title="销售趋势" class="card"><div class="bars"><div v-for="d in trend" :key="d.date" class="bar" :style="{height:barHeight(d)}" :title="`${d.date} ¥${fmt(d.amount)} / ${d.orders}单`"></div></div><div class="axis"><span>{{trend[0]?.date}}</span><span>{{trend[trend.length-1]?.date}}</span></div></a-card>
        <a-row :gutter="16"><a-col :lg="12"><a-card :bordered="false" title="流量来源" class="card"><SourceList :rows="sources"/></a-card></a-col><a-col :lg="12"><a-card :bordered="false" title="转化漏斗" class="card"><Funnel :data="conversion"/></a-card></a-col></a-row>
      </a-tab-pane>
      <a-tab-pane key="sources" tab="流量来源"><a-card :bordered="false" class="card"><a-table :data-source="sources" :pagination="{pageSize:10}" row-key="source" :columns="sourceCols"/></a-card></a-tab-pane>
      <a-tab-pane key="products" tab="商品流量"><a-card :bordered="false" class="card"><a-table :data-source="products" :pagination="{pageSize:10}" row-key="prodId" :columns="productCols"/></a-card></a-tab-pane>
      <a-tab-pane key="conversion" tab="转化分析"><a-card :bordered="false" title="全链路转化" class="card"><Funnel :data="conversion"/><a-divider/><a-table :data-source="conversion.products" row-key="prodId" :pagination="{pageSize:8}" :columns="funnelCols"/></a-card></a-tab-pane>
      <a-tab-pane key="diagnosis" tab="流量诊断"><a-card :bordered="false" class="card"><a-alert v-for="d in diagnosis" :key="d.code" :type="d.level==='success'?'success':d.level==='warning'?'warning':'info'" :message="d.title" :description="d.suggestion" show-icon class="diag"/></a-card></a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import {ref,computed,onMounted} from 'vue'
import {trafficApi} from '../api'
const tab=ref('overview'),days=ref(30),ov=ref({}),trend=ref([]),sources=ref([]),products=ref([]),conversion=ref({products:[]}),diagnosis=ref([])
const fmt=n=>Number(n||0).toFixed(2)
const metrics=computed(()=>[
 {label:'近30天销售额',value:'¥'+fmt(ov.value.recentAmount),sub:ov.value.chainGrowthPct==null?'环比 —':`环比 ${Number(ov.value.chainGrowthPct)>=0?'↑':'↓'} ${Math.abs(Number(ov.value.chainGrowthPct)).toFixed(1)}%`,cls:Number(ov.value.chainGrowthPct)>=0?'up':'down'},
 {label:'近30天订单',value:ov.value.recentOrders||0,sub:'已支付订单'},
 {label:'客单价',value:'¥'+fmt(ov.value.avgOrderValue),sub:'近30天'},
 {label:'动销率',value:(ov.value.activeRatePct||0)+'%',sub:`动销 ${ov.value.activeProducts||0} / 在售 ${ov.value.onSaleProducts||0}`},
 {label:'累计销售额',value:'¥'+fmt(ov.value.totalAmount),sub:'已支付'},
 {label:'累计销量',value:ov.value.totalSold||0,sub:'件'}
])
const sourceCols=[{title:'来源',dataIndex:'source'},{title:'UV',dataIndex:'uv'},{title:'商品浏览',dataIndex:'productViews'},{title:'加购',dataIndex:'carts'},{title:'支付',dataIndex:'pays'}]
const productCols=[{title:'商品',dataIndex:'title'},{title:'浏览UV',dataIndex:'views'},{title:'加购UV',dataIndex:'carts'},{title:'支付UV',dataIndex:'pays'}]
const funnelCols=[{title:'商品',dataIndex:'title'},{title:'浏览UV',dataIndex:'views'},{title:'加购UV',dataIndex:'carts'},{title:'支付UV',dataIndex:'pays'}]
const maxAmount=computed(()=>Math.max(...trend.value.map(x=>Number(x.amount||0)),.01))
const barHeight=d=>Math.max(4,Number(d.amount||0)/maxAmount.value*100)+'%'
async function reload(){const d=days.value;try{const [o,t,s,p,c,di]=await Promise.all([trafficApi.overview(),trafficApi.trend(d),trafficApi.sources(d),trafficApi.products(d),trafficApi.conversion(d),trafficApi.diagnosis(d)]);ov.value=o;trend.value=t||[];sources.value=s||[];products.value=p||[];conversion.value=c||{products:[]};diagnosis.value=di||[]}catch(e){}}
onMounted(reload)
</script>

<script>
export default {components:{
  SourceList:{props:['rows'],template:`<div class="source-list"><div v-for="r in rows" :key="r.source" class="source"><b>{{r.source}}</b><span>{{r.uv}} UV</span><span>{{r.pays}} 支付</span></div></div>`},
  Funnel:{props:['data'],template:`<div class="funnel"><div><b>访问</b><strong>{{data.visits||0}}</strong></div><i>→</i><div><b>浏览</b><strong>{{data.views||0}}</strong><small>{{data.viewRate||0}}%</small></div><i>→</i><div><b>加购</b><strong>{{data.carts||0}}</strong><small>{{data.cartRate||0}}%</small></div><i>→</i><div><b>支付</b><strong>{{data.pays||0}}</strong><small>{{data.payRate||0}}%</small></div></div>`}
}}
</script>

<style scoped>
.traffic-wrap{max-width:1180px;margin:0 auto}.head{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.head h2{margin:0 0 4px}.head span,.metric div,.metric small{color:#64748b;font-size:12px}.metric{margin-bottom:16px;border-radius:12px}.metric strong{display:block;font-size:25px;margin:6px 0}.up{color:#16a34a!important}.down{color:#dc2626!important}.card{border-radius:12px;margin-bottom:16px}.bars{height:190px;display:flex;gap:3px;align-items:flex-end;border-bottom:1px solid #e5e7eb;padding:8px 4px 0}.bar{flex:1;min-height:4px;background:linear-gradient(180deg,#22d3ee,#1e6eb8);border-radius:3px 3px 0 0}.axis{display:flex;justify-content:space-between;color:#64748b;font-size:12px;padding-top:6px}.funnel{display:flex;align-items:center;justify-content:space-around;gap:8px}.funnel>div{background:#f5f7fa;border-radius:10px;padding:14px 20px;min-width:110px;text-align:center}.funnel b,.funnel strong,.funnel small{display:block}.funnel strong{font-size:22px;margin:4px 0}.funnel small{color:#1e6eb8}.source-list{display:flex;flex-direction:column;gap:10px}.source{display:flex;justify-content:space-between;padding:10px 12px;background:#f8fafc;border-radius:8px}.diag{margin-bottom:12px}@media(max-width:700px){.head{align-items:flex-start;gap:12px;flex-direction:column}.funnel{overflow:auto;justify-content:flex-start}.funnel>div{min-width:100px}}
</style>