<template>
  <div class="mc-wrap">
    <a-spin :spinning="loading">
      <template v-if="data">
        <!-- 会员卡 -->
        <a-card :bordered="false" class="mc-card">
          <div class="mc-head">
            <div>
              <div class="mc-level">{{ data.level?.levelName }}</div>
              <div class="mc-sub">成长值 {{ data.card.growth }} · 积分 {{ data.card.points }}</div>
            </div>
            <div class="mc-rate">等级折扣 <b>{{ rate }}</b></div>
          </div>
          <div class="mc-bar">
            <div class="mc-bar-in" :style="{ width: growthPct }"></div>
          </div>
          <div class="mc-hint">{{ nextLevelHint }}</div>
        </a-card>

        <!-- 等级体系 -->
        <a-card :bordered="false" class="mc-card" title="会员等级体系">
          <a-table :data-source="data.levels" :columns="levelCols" row-key="levelId" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'rate'">
                {{ discountText(record.discountRate) }}
                <a-tag v-if="data.level && data.level.levelId === record.levelId" color="gold">当前</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- 积分规则（获取/抵现/上限/VIP 倍数） -->
        <a-card :bordered="false" class="mc-card" title="积分规则">
          <div class="rule-grid">
            <div class="rule-item">
              <div class="rule-num">{{ data.rule?.pointsPerYuanEarn ?? 1 }} 积分</div>
              <div class="rule-label">消费 1 元获得</div>
            </div>
            <div class="rule-item">
              <div class="rule-num">{{ data.rule?.redeemPointsPerYuan ?? 100 }} 积分</div>
              <div class="rule-label">可抵扣 1 元</div>
            </div>
            <div class="rule-item">
              <div class="rule-num">{{ data.rule?.redeemMaxPercent ?? 10 }}%</div>
              <div class="rule-label">单笔抵现上限</div>
            </div>
            <div class="rule-item">
              <div class="rule-num">×{{ data.rule?.vipEarnMultiplier ?? 2 }}</div>
              <div class="rule-label">VIP 积分倍数</div>
            </div>
          </div>
          <div class="rule-tip">积分在订单支付后到账；使用积分抵现的订单若取消，积分将自动返还。</div>
        </a-card>

        <!-- 积分流水 -->
        <a-card :bordered="false" class="mc-card" title="积分流水">
          <a-empty v-if="!data.pointsLog?.length" description="暂无积分流水" />
          <a-list v-else size="small" :data-source="data.pointsLog">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="pl-item">
                  <div>{{ item.remark }} <span class="pl-order" v-if="item.refOrderNo">（{{ item.refOrderNo }}）</span></div>
                  <div class="pl-time">{{ (item.createTime || '').replace('T', ' ') }}</div>
                </div>
                <span class="pl-val">+{{ item.changeVal }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </template>
      <a-empty v-else-if="!loading" description="暂无会员数据" />
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { membershipApi } from '../api'

const data = ref(null)
const loading = ref(false)
const levelCols = [
  { title: '等级', dataIndex: 'levelName' },
  { title: '成长值门槛', dataIndex: 'minGrowth' },
  { title: '权益', dataIndex: 'description' },
  { title: '折扣', key: 'rate', width: 130 }
]

const rate = computed(() => (data.value?.level?.discountRate ? discountText(data.value.level.discountRate) : '—'))
const growthPct = computed(() => {
  const levels = data.value?.levels || []
  const cur = data.value?.level?.minGrowth || 0
  const next = levels.find(l => l.minGrowth > cur)
  if (!next) return '100%'
  const span = next.minGrowth - cur
  const got = (data.value?.card?.growth || 0) - cur
  return Math.max(3, Math.min(100, (got / span) * 100)).toFixed(0) + '%'
})
const nextLevelHint = computed(() => {
  const levels = data.value?.levels || []
  const cur = data.value?.level?.minGrowth || 0
  const next = levels.find(l => l.minGrowth > cur)
  if (!next) return '已达最高等级'
  return `距离「${next.levelName}」还需成长值 ${Math.max(0, next.minGrowth - (data.value?.card?.growth || 0))}`
})

function discountText(r) {
  if (!r) return '无'
  const v = Number(r)
  if (v >= 1) return '无折扣'
  return (v * 10).toFixed(1) + ' 折'
}

onMounted(async () => {
  loading.value = true
  try { data.value = await membershipApi.my() } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.mc-wrap { max-width: 760px; }
.mc-card { border-radius: 12px; margin-bottom: 16px; }
.mc-head { display: flex; justify-content: space-between; align-items: center; }
.mc-level { font-size: 22px; font-weight: 800; color: #b45309; }
.mc-sub { font-size: 13px; color: var(--ll-gray, #4b5563); margin-top: 4px; }
.mc-rate { font-size: 15px; font-weight: 700; color: var(--ll-primary, #1e6eb8); }
.mc-bar { height: 8px; border-radius: 4px; background: #e5e7eb; margin: 14px 0 8px; overflow: hidden; }
.mc-bar-in { height: 100%; border-radius: 4px; background: linear-gradient(90deg, #b45309, #f59e0b); }
.mc-hint { font-size: 12px; color: var(--ll-muted, #64748b); }
.pl-item { line-height: 1.5; }
.pl-order { font-size: 12px; color: var(--ll-muted, #64748b); }
.pl-time { font-size: 12px; color: var(--ll-gray2, #999); }
.pl-val { font-weight: 700; color: #10b981; }
/* 积分规则卡片 */
.rule-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.rule-item { background: rgba(30, 110, 184, .05); border-radius: 10px; padding: 14px 10px; text-align: center; }
.rule-num { font-size: 20px; font-weight: 800; color: var(--ll-primary, #1e6eb8); }
.rule-label { font-size: 12px; color: var(--ll-muted, #64748b); margin-top: 4px; }
.rule-tip { font-size: 12px; color: var(--ll-muted, #64748b); margin-top: 12px; }
@media (max-width: 640px) { .rule-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
