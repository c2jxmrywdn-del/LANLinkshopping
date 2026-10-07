<template>
  <a-modal :open="open" :title="isFreeze ? '冻结商户账户' : '恢复商户账户'" width="520px"
           :ok-text="isFreeze ? '确认冻结' : '确认恢复'" :ok-button-props="{ danger: isFreeze }"
           :confirm-loading="saving" @cancel="$emit('update:open', false)" @ok="submit" destroy-on-close>
    <a-alert :type="isFreeze ? 'warning' : 'info'" show-icon class="tip" :message="alertText" />
    <a-form layout="vertical">
      <a-form-item label="目标状态" required>
        <a-radio-group v-model:value="status">
          <a-radio :value="1">正常（可经营）</a-radio>
          <a-radio :value="2">冻结（暂停经营权限）</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="变更原因（必填，将写入审计日志与流程跟踪）" required>
        <a-textarea v-model:value="reason" :rows="3" :maxlength="200" show-count
                    placeholder="例如：存在违规经营行为，暂停经营权限待核查" />
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
  targetStatus: { type: Number, default: 2 }
})
const emit = defineEmits(['update:open', 'saved'])

const saving = ref(false)
const status = ref(2)
const reason = ref('')

const isFreeze = computed(() => status.value === 2)
const alertText = computed(() => isFreeze.value
  ? '冻结后该商户将立即失去商户中心与商品发布等经营权限（重新登录亦不恢复），已产生的数据不受影响。'
  : '恢复正常后该商户的经营权限立即生效，无需商户重新登录。')

watch(() => props.open, (v) => {
  if (!v) return
  status.value = props.targetStatus === 1 ? 1 : 2
  reason.value = ''
})

async function submit() {
  if (!reason.value.trim()) {
    message.warning('请填写变更原因')
    return
  }
  saving.value = true
  try {
    await merchantApi.adminStatus(props.merchant.merId, status.value, reason.value.trim())
    message.success(status.value === 2 ? '商户账户已冻结' : '商户账户已恢复正常')
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
</style>
