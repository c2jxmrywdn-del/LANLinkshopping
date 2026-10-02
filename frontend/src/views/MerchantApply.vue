<template>
  <div class="wrap">
    <a-card title="商户入驻申请" class="left">
      <a-alert type="info" show-icon style="margin-bottom:16px"
               message="平台筛选机制"
               description="需满足：公司类型注册，或个体工商户注册资本 > 10 万元；且有稳定纳税记录。系统自动审核。营业执照与税务证明等证照材料将在入驻通过后，于「我的商品 → 证照管理」中上传绑定。" />
      <a-form :model="form" layout="vertical" @finish="submit">
        <a-form-item label="企业名称" :rules="[{ required: true }]"><a-input v-model:value="form.entName" /></a-form-item>
        <a-form-item label="统一社会信用代码"><a-input v-model:value="form.creditCode" /></a-form-item>
        <a-form-item label="注册类型">
          <a-select v-model:value="form.regType">
            <a-select-option value="公司">公司</a-select-option>
            <a-select-option value="个体工商户">个体工商户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="注册资本（元）" :rules="[{ required: true }]"><a-input-number v-model:value="form.regCapital" :min="0" style="width:100%" /></a-form-item>
        <a-form-item label="是否有稳定纳税记录">
          <a-radio-group v-model:value="form.taxStatus">
            <a-radio :value="1">有</a-radio><a-radio :value="0">无</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="入驻方式">
          <a-select v-model:value="form.joinType">
            <a-select-option value="入驻">自主入驻</a-select-option>
            <a-select-option value="加盟">加盟</a-select-option>
            <a-select-option value="邀约">平台邀约</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="已具备资质（逗号分隔，选填）"><a-input v-model:value="form.qNames" /></a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">提交申请</a-button>
      </a-form>
    </a-card>
    <a-card title="我的商户状态" class="right">
      <template v-if="mine">
        <a-result :status="mine.reviewStatus === 1 ? 'success' : (mine.reviewStatus === 2 ? 'error' : 'info')"
                  :title="statusText">
          <template #subTitle>
            <div>注册资本：¥{{ mine.regCapital }}</div>
            <div v-if="mine.licenseUrl" class="license-view">
              营业执照：<a :href="mine.licenseUrl" target="_blank">查看</a>
            </div>
            <div v-if="mine.rejectReason" style="color:#e4393c">原因：{{ mine.rejectReason }}</div>
            <div v-if="mine.reviewStatus === 1" class="next-step">
              下一步：前往 <a @click="$router.push('/merchant/products')">我的商品 → 证照管理</a> 上传营业执照与税务证明
            </div>
          </template>
        </a-result>
      </template>
      <a-empty v-else description="尚未提交入驻申请" />
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { merchantApi } from '../api'

const loading = ref(false)
const mine = ref(null)
const form = reactive({ entName: '', creditCode: '', regType: '公司', regCapital: 500000, taxStatus: 1, joinType: '入驻', qNames: '' })
const statusText = computed(() => ({ 0: '审核中', 1: '审核通过 · 已入驻', 2: '审核未通过' }[mine.value?.reviewStatus] || ''))

async function submit() {
  loading.value = true
  try {
    const r = await merchantApi.apply(form)
    mine.value = r
    message.success(r.reviewStatus === 1 ? '恭喜，自动审核通过！' : '已提交，请等待审核')
  } finally { loading.value = false }
}
onMounted(async () => { try { mine.value = await merchantApi.my() } catch (e) {} })
</script>

<style scoped>
.wrap { display: flex; gap: 24px; align-items: flex-start; }
.left { flex: 1; } .right { width: 420px; }
.license-view { font-size: 13px; }
.next-step { font-size: 13px; margin-top: 8px; }
.next-step a { color: var(--ll-primary, #1e6eb8); cursor: pointer; }
</style>
