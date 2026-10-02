<template>
  <div class="ac-wrap">
    <a-alert type="info" show-icon class="tip"
             message="营销中台 · 活动中心"
             description="参与活动可获得积分奖励；「消费有礼」类活动在订单支付后由系统自动参与。" />
    <a-spin :spinning="loading">
      <div v-if="acts.length" class="acts">
        <a-card v-for="a in acts" :key="a.activityId" :bordered="false" class="act-card">
          <div class="act-type">
            <a-tag :color="a.type === 'register' ? 'blue' : 'gold'">{{ a.type === 'register' ? '注册有礼' : '消费有礼' }}</a-tag>
          </div>
          <div class="act-title">{{ a.title }}</div>
          <div class="act-desc">{{ a.description }}</div>
          <div class="act-actions">
            <a-button v-if="a.joined" disabled>已参与</a-button>
            <a-button v-else type="primary" :loading="joiningId === a.activityId" @click="join(a)">立即参与</a-button>
          </div>
        </a-card>
      </div>
      <a-empty v-else-if="!loading" description="暂无可参与的活动" />
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { activityApi } from '../api'

const acts = ref([])
const loading = ref(false)
const joiningId = ref(null)

async function join(a) {
  joiningId.value = a.activityId
  try {
    await activityApi.join(a.activityId)
    message.success('参与成功，奖励 100 积分', 3)
    a.joined = true
  } catch (e) { /* 拦截器已提示（如重复参与） */ }
  finally { joiningId.value = null }
}

onMounted(async () => {
  loading.value = true
  try { acts.value = (await activityApi.list()) || [] } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.ac-wrap { max-width: 820px; }
.tip { margin-bottom: 16px; }
.acts { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; }
.act-card { border-radius: 12px; }
.act-title { font-size: 16px; font-weight: 700; margin: 6px 0; }
.act-desc { font-size: 13px; color: var(--ll-gray, #4b5563); min-height: 40px; }
.act-actions { margin-top: 12px; }
</style>
