<template>
  <a-modal :open="open" title="编辑商户基本资料" width="620px" :confirm-loading="saving"
           ok-text="保存修改" @cancel="$emit('update:open', false)" @ok="submit" destroy-on-close>
    <a-alert type="info" show-icon class="tip"
             message="仅提交已填写的字段，留空字段保持原值不变；敏感信息（统一社会信用代码、税务登记号）由服务端加密存储。" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="企业名称">
            <a-input v-model:value="form.entName" maxlength="128" :placeholder="entNamePh" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="统一社会信用代码">
            <a-input v-model:value="form.creditCode" maxlength="64" :placeholder="creditPh" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="注册类型">
            <a-select v-model:value="form.regType">
              <a-select-option value="公司">公司</a-select-option>
              <a-select-option value="个体工商户">个体工商户</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="注册资本（元）">
            <a-input-number v-model:value="form.regCapital" :min="0" :precision="2" style="width:100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="纳税申报">
            <a-select v-model:value="form.taxStatus">
              <a-select-option :value="1">有稳定纳税记录</a-select-option>
              <a-select-option :value="0">无/未知</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="入驻方式">
            <a-select v-model:value="form.joinType">
              <a-select-option value="加盟">加盟</a-select-option>
              <a-select-option value="入驻">入驻</a-select-option>
              <a-select-option value="邀约">邀约</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="税务登记号">
        <a-input v-model:value="form.taxRegNo" maxlength="64" :disabled="clearTax" :placeholder="taxRegPh" />
        <a-checkbox v-model:checked="clearTax" class="clear-tax">清空已绑定的税务登记号</a-checkbox>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { merchantApi } from '../../api'

const props = defineProps({
  open: Boolean,
  merchant: { type: Object, default: () => ({}) },
  enterprise: { type: Object, default: null }
})
const emit = defineEmits(['update:open', 'saved'])

const formRef = ref(null)
const saving = ref(false)
const clearTax = ref(false)
const form = ref({})

const entNamePh = computed(() => props.enterprise?.name || '留空不修改')
const creditPh = computed(() => props.enterprise?.creditCode
  ? `当前：${props.enterprise.creditCode}（留空不修改）` : '留空不修改')
// 税务登记号出口已脱敏，仅作「当前值」提示，避免把掩码回写覆盖真实值
const taxRegPh = computed(() => props.merchant?.taxRegNo
  ? `当前：${props.merchant.taxRegNo}（留空不修改）` : '未绑定，可填写绑定')

watch(() => props.open, (v) => {
  if (!v) return
  clearTax.value = false
  form.value = {
    entName: '',
    creditCode: '',
    regType: props.merchant?.regType || '公司',
    regCapital: props.merchant?.regCapital ?? 0,
    taxStatus: props.merchant?.taxStatus ?? 0,
    joinType: props.merchant?.joinType || '入驻',
    taxRegNo: ''
  }
})

async function submit() {
  // 与后端一致的前置拦截：脱敏展示值（含 ****）不可回填提交，否则会污染档案
  const maskedField = [form.value.creditCode, form.value.taxRegNo]
    .find(v => v && v.includes('****'))
  if (maskedField) {
    message.warning('请填写完整的真实值，不要提交脱敏后的占位内容（含 ****）')
    return
  }

  const payload = {}
  if (form.value.entName?.trim()) payload.entName = form.value.entName.trim()
  if (form.value.creditCode?.trim()) payload.creditCode = form.value.creditCode.trim()
  if (form.value.regType) payload.regType = form.value.regType
  if (form.value.regCapital !== null && form.value.regCapital !== undefined) payload.regCapital = form.value.regCapital
  if (form.value.taxStatus !== null && form.value.taxStatus !== undefined) payload.taxStatus = form.value.taxStatus
  if (form.value.joinType) payload.joinType = form.value.joinType
  if (clearTax.value) payload.taxRegNo = ''
  else if (form.value.taxRegNo?.trim()) payload.taxRegNo = form.value.taxRegNo.trim()

  saving.value = true
  try {
    await merchantApi.adminUpdate(props.merchant.merId, payload)
    message.success('商户资料已更新')
    emit('update:open', false)
    emit('saved')
  } catch (e) {
    console.error(e) // 响应拦截器已提示错误
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip { margin-bottom: 16px; }
.clear-tax { margin-top: 6px; font-size: 12px; color: #999; }
</style>
