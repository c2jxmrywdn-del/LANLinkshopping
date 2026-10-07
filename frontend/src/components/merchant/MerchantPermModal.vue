<template>
  <a-modal :open="open" title="商户权限配置" width="580px" :confirm-loading="saving"
           ok-text="保存并生效" @cancel="$emit('update:open', false)" @ok="submit" destroy-on-close>
    <a-spin :spinning="loading">
      <a-alert v-if="status !== 1" type="error" show-icon class="tip"
               message="该商户账户已冻结/注销，以下授权暂不生效；恢复正常后自动生效。" />
      <a-alert v-else type="info" show-icon class="tip"
               message="仅可配置商户档可选权限；保存后立即生效，无需商户重新登录。" />

      <div class="sec-title">
        商户档可选权限
        <span class="sub">（未配置时默认全部授予，兼容存量商户）</span>
      </div>
      <a-checkbox-group v-model:value="checked" class="perm-group">
        <div v-for="p in optionalPerms" :key="p" class="perm-item" :class="{ on: checked.includes(p) }">
          <a-checkbox :value="p">
            <span class="perm-code">{{ p }}</span>
            <span class="perm-label">{{ permLabel(p) }}</span>
          </a-checkbox>
        </div>
      </a-checkbox-group>

      <div class="footer-tip">
        <a-tag v-if="configured" color="blue">已显式配置</a-tag>
        <a-tag v-else>未配置（默认全量授予）</a-tag>
        <span class="cnt">当前已授权 {{ checked.length }} / {{ optionalPerms.length }} 项</span>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { merchantApi } from '../../api'

const props = defineProps({
  open: Boolean,
  merchant: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:open', 'saved'])

/** 权限点说明文案（与后端 UserIdentity.MERCHANT_OPTIONAL_PERMS 白名单一致） */
const PERM_LABELS = {
  'merchant:manage': '商户中心与证照管理：流量分析、营业执照/税务证明上传、纳税记录查询',
  'product:publish': '商品发布与管理：发布商品、编辑与上下架'
}
function permLabel(code) { return PERM_LABELS[code] || code }

const loading = ref(false)
const saving = ref(false)
const checked = ref([])
const optionalPerms = ref([])
const configured = ref(false)
const status = ref(1)

watch(() => props.open, async (v) => {
  if (!v) return
  loading.value = true
  try {
    const cfg = await merchantApi.adminPermConfig(props.merchant.merId)
    optionalPerms.value = cfg.optionalPerms || []
    checked.value = cfg.grantedPerms || []
    configured.value = !!cfg.configured
    status.value = cfg.status ?? 1
  } catch (e) {
    console.error(e) // 响应拦截器已提示错误
  } finally {
    loading.value = false
  }
})

async function submit() {
  saving.value = true
  try {
    await merchantApi.adminPermUpdate(props.merchant.merId, checked.value)
    message.success('权限配置已生效')
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
.tip { margin-bottom: 14px; }
.sec-title { font-weight: 700; font-size: 14px; margin-bottom: 10px; }
.sec-title .sub { font-weight: 400; font-size: 12px; color: #999; }
.perm-group { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.perm-item {
  padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px;
  transition: all .2s; background: #fff;
}
.perm-item:hover { border-color: var(--ll-primary, #1e6eb8); }
.perm-item.on { border-color: var(--ll-primary, #1e6eb8); background: rgba(30, 110, 184, .04); }
.perm-item :deep(.ant-checkbox-wrapper) { display: flex; align-items: flex-start; }
.perm-code { display: inline-block; font-family: monospace; font-size: 12px; color: #666; margin-right: 6px; }
.perm-label { font-size: 13px; }
.footer-tip { margin-top: 14px; display: flex; align-items: center; gap: 8px; font-size: 12px; color: #999; }
.footer-tip .cnt { margin-left: auto; }
</style>
