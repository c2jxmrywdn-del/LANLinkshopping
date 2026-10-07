<template>
  <a-card title="商户管理">
    <template #extra>
      <a-space wrap>
        <a-input-search v-model:value="keyword" placeholder="商户ID/企业ID/企业名称" style="width:210px"
                        allow-clear @search="onFilterChange" @change="onKeywordChange" />
        <a-radio-group v-model:value="filter" button-style="solid" @change="onFilterChange">
          <a-radio-button :value="null">全部</a-radio-button>
          <a-radio-button :value="0">待审核</a-radio-button>
          <a-radio-button :value="1">已通过</a-radio-button>
          <a-radio-button :value="2">已驳回</a-radio-button>
        </a-radio-group>
        <a-select v-model:value="statusFilter" style="width:132px" placeholder="账户状态" @change="onFilterChange">
          <a-select-option :value="null">全部账户状态</a-select-option>
          <a-select-option :value="1">正常</a-select-option>
          <a-select-option :value="2">冻结</a-select-option>
          <a-select-option :value="3">已注销</a-select-option>
        </a-select>
      </a-space>
    </template>
    <a-table :data-source="list" :columns="cols" row-key="merId" :loading="loading"
             :pagination="pager" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cap'">¥{{ record.regCapital }}</template>
        <template v-else-if="column.key === 'tax'">
          <a-tag :color="record.taxStatus === 1 ? 'green' : 'default'">{{ record.taxStatus === 1 ? '稳定' : '无' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'cert'">
          <a-space :size="4">
            <a-tooltip title="营业执照"><a-tag :color="record.licenseUrl ? 'success' : 'default'">照</a-tag></a-tooltip>
            <a-tooltip title="纳税记录份数"><a-tag :color="taxCount(record) > 0 ? 'success' : 'default'">税 {{ taxCount(record) }}</a-tag></a-tooltip>
            <a-tooltip title="税务登记号"><a-tag :color="record.taxRegNo ? 'success' : 'default'">号</a-tag></a-tooltip>
          </a-space>
        </template>
        <template v-else-if="column.key === 'time'">{{ (record.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
        <template v-else-if="column.key === 'acc'">
          <a-tag :color="accColor[accStatus(record)]">{{ accText[accStatus(record)] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'st'">
          <a-tag :color="stColor[record.reviewStatus]">{{ stText[record.reviewStatus] }}</a-tag>
          <a-tooltip v-if="record.rejectReason" :title="record.rejectReason">
            <div class="reason">原因：{{ record.rejectReason }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'op'">
          <a-space wrap>
            <a-button size="small" @click="openDetail(record)">快速查看</a-button>
            <a-button size="small" type="link" @click="router.push('/admin/merchants/' + record.merId)">档案</a-button>
            <a-button v-if="record.reviewStatus !== 1" size="small" type="primary" @click="approve(record)">通过</a-button>
            <a-button v-if="record.reviewStatus !== 2" size="small" danger @click="openReject(record)">驳回</a-button>
            <a-button size="small" :disabled="accStatus(record) === 3" @click="openEdit(record)">资料</a-button>
            <a-button size="small" :disabled="accStatus(record) === 3" @click="openPerm(record)">权限</a-button>
            <a-button v-if="accStatus(record) !== 3" size="small" @click="openStatus(record)">
              {{ accStatus(record) === 2 ? '解冻' : '冻结' }}
            </a-button>
            <a-button v-if="accStatus(record) !== 3" size="small" danger @click="openDelete(record)">注销</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 商户详情抽屉：资料维护 + 资质验证 + 流程跟踪 -->
    <a-drawer v-model:open="detailOpen" title="商户档案详情" width="560" :loading="detailLoading">
      <template v-if="detail">
        <!-- 入驻流程跟踪 -->
        <a-steps :current="flowStep" size="small" style="margin-bottom:20px">
          <a-step title="提交申请" :description="fmtTime(detail.merchant.createTime)" />
          <a-step title="资质核验" :description="readinessText" />
          <a-step title="审核完成" :description="reviewDesc" />
        </a-steps>

        <!-- 工商信息 -->
        <a-descriptions title="工商信息" :column="1" size="small" bordered style="margin-bottom:16px">
          <a-descriptions-item label="企业名称">{{ detail.enterprise?.name || '—' }}</a-descriptions-item>
          <a-descriptions-item label="统一社会信用代码">{{ detail.enterprise?.creditCode || '—' }}</a-descriptions-item>
          <a-descriptions-item label="主体类型">{{ detail.merchant.regType }}</a-descriptions-item>
          <a-descriptions-item label="注册资本">¥{{ detail.merchant.regCapital }}</a-descriptions-item>
          <a-descriptions-item label="入驻方式">{{ detail.merchant.joinType || '—' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 资质验证 -->
        <div class="sec-title">资质验证</div>
        <div class="cert-checks">
          <div class="cert-check" :class="{ ok: detail.certReadiness.hasLicense }">
            <check-circle-filled v-if="detail.certReadiness.hasLicense" class="ck ok" />
            <close-circle-filled v-else class="ck miss" />
            营业执照
            <a v-if="detail.merchant.licenseUrl" :href="detail.merchant.licenseUrl" target="_blank" style="margin-left:auto">查看原件</a>
          </div>
          <div class="cert-check" :class="{ ok: detail.certReadiness.taxProofCount > 0 }">
            <check-circle-filled v-if="detail.certReadiness.taxProofCount > 0" class="ck ok" />
            <close-circle-filled v-else class="ck miss" />
            近期纳税记录（{{ detail.certReadiness.taxProofCount }}/3 份）
          </div>
          <div class="cert-check" :class="{ ok: detail.certReadiness.hasTaxRegNo }">
            <check-circle-filled v-if="detail.certReadiness.hasTaxRegNo" class="ck ok" />
            <close-circle-filled v-else class="ck miss" />
            税务登记号{{ detail.merchant.taxRegNo ? '：' + detail.merchant.taxRegNo : '' }}
          </div>
        </div>

        <!-- 纳税记录材料 -->
        <template v-if="taxUrls.length">
          <div class="sec-title">纳税记录材料</div>
          <div class="proof-list">
            <a v-for="(u, i) in taxUrls" :key="i" :href="u" target="_blank" class="proof-item">
              <file-pdf-outlined v-if="u.toLowerCase().includes('.pdf')" />
              <file-image-outlined v-else />
              记录 {{ i + 1 }}
            </a>
          </div>
        </template>

        <!-- 已具备资质 -->
        <template v-if="detail.qualifications?.length">
          <div class="sec-title">已具备资质（{{ detail.qualifications.length }}）</div>
          <a-tag v-for="q in detail.qualifications" :key="q.qId" style="margin-bottom:6px">{{ q.qName }}</a-tag>
        </template>

        <!-- 审核信息 -->
        <div class="sec-title">审核信息</div>
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="当前状态">
            <a-tag :color="stColor[detail.merchant.reviewStatus]">{{ stText[detail.merchant.reviewStatus] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.merchant.rejectReason" label="驳回原因">
            <span style="color:#e4393c">{{ detail.merchant.rejectReason }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="最近更新">{{ fmtTime(detail.merchant.updateTime) }}</a-descriptions-item>
        </a-descriptions>

        <!-- 账户状态与权限（运营端维护入口） -->
        <div class="sec-title">账户状态与权限</div>
        <a-descriptions :column="1" size="small" bordered style="margin-bottom:12px">
          <a-descriptions-item label="账户状态">
            <a-tag :color="accColor[accStatus(detail.merchant)]">{{ accText[accStatus(detail.merchant)] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="税务登记号">
            <span>{{ detail.merchant.taxRegNo || '—' }}</span>
            <a v-if="detail.merchant.taxRegNo && !fullTaxNo" style="margin-left:8px"
               @click="viewTaxRegNo(detail.merchant)">查看完整</a>
            <span v-if="fullTaxNo" style="margin-left:8px;font-family:monospace">{{ fullTaxNo }}</span>
          </a-descriptions-item>
        </a-descriptions>
        <a-space wrap style="margin-bottom:4px">
          <a-button size="small" :disabled="accStatus(detail.merchant) === 3"
                    @click="openEdit(detail.merchant, detail.enterprise)">编辑资料</a-button>
          <a-button size="small" :disabled="accStatus(detail.merchant) === 3"
                    @click="openPerm(detail.merchant)">权限配置</a-button>
          <a-button v-if="accStatus(detail.merchant) !== 3" size="small" @click="openStatus(detail.merchant)">
            {{ accStatus(detail.merchant) === 2 ? '解冻账户' : '冻结账户' }}
          </a-button>
          <a-button v-if="accStatus(detail.merchant) !== 3" size="small" danger
                    @click="openDelete(detail.merchant)">注销商户</a-button>
        </a-space>

        <!-- 抽屉内审核操作 -->
        <div style="margin-top:20px" v-if="detail.merchant.reviewStatus !== 1">
          <a-space>
            <a-button type="primary" @click="approve(detail.merchant)">审核通过</a-button>
            <a-button v-if="detail.merchant.reviewStatus !== 2" danger @click="openReject(detail.merchant)">驳回</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>

    <!-- 驳回原因（必填） -->
    <a-modal v-model:open="rejectOpen" title="驳回入驻申请" @ok="doReject" :confirm-loading="reviewing">
      <a-form layout="vertical">
        <a-form-item label="驳回原因（必填，将展示给商户用于整改）" required>
          <a-textarea v-model:value="rejectReason" :rows="3" :maxlength="200" show-count
                      placeholder="例如：注册资本或主体类型不符合平台筛选条件" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 注销商户（必填原因：逻辑删除 + 商户身份降级） -->
    <a-modal v-model:open="deleteOpen" title="注销商户" :confirm-loading="deleting" ok-text="确认注销"
             :ok-button-props="{ danger: true }" @ok="doDelete">
      <a-alert type="warning" show-icon style="margin-bottom:14px"
               message="注销后账户状态置为「已注销」、档案逻辑删除（数据保留可追溯），商户负责人身份降级为普通买家，不可再恢复经营。" />
      <a-form layout="vertical">
        <a-form-item label="注销原因（必填，写入审计日志与流程跟踪）" required>
          <a-textarea v-model:value="deleteReason" :rows="3" :maxlength="200" show-count
                      placeholder="例如：商户主动申请退出平台 / 长期未经营" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 运营端维护：资料编辑 / 账户状态 / 权限配置 -->
    <MerchantEditModal v-model:open="editOpen" :merchant="target" :enterprise="editEnterprise" @saved="afterMutate" />
    <MerchantStatusModal v-model:open="statusOpen" :merchant="target" :target-status="statusTarget" @saved="afterMutate" />
    <MerchantPermModal v-model:open="permOpen" :merchant="target" @saved="afterMutate" />
  </a-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { CheckCircleFilled, CloseCircleFilled, FilePdfOutlined, FileImageOutlined } from '@ant-design/icons-vue'
import { merchantApi } from '../../api'
import MerchantEditModal from '../../components/merchant/MerchantEditModal.vue'
import MerchantStatusModal from '../../components/merchant/MerchantStatusModal.vue'
import MerchantPermModal from '../../components/merchant/MerchantPermModal.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const filter = ref(null)
const keyword = ref('')
const statusFilter = ref(null)
const stText = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const stColor = { 0: 'orange', 1: 'green', 2: 'red' }
/** 账户状态：1正常 2冻结 3注销（缺省视为正常，兼容存量数据） */
const accText = { 1: '正常', 2: '冻结', 3: '已注销' }
const accColor = { 1: 'green', 2: 'orange', 3: 'default' }
function accStatus(r) { return r?.status ?? 1 }
const cols = [
  { title: '商户ID', dataIndex: 'merId', width: 80 },
  { title: '企业ID', dataIndex: 'entId', width: 80 },
  { title: '主体类型', dataIndex: 'regType', width: 100 },
  { title: '注册资本', key: 'cap', width: 110 },
  { title: '纳税申报', key: 'tax', width: 90 },
  { title: '证照材料', key: 'cert', width: 140 },
  { title: '账户状态', key: 'acc', width: 100 },
  { title: '审核状态', key: 'st', width: 190 },
  { title: '申请时间', key: 'time', width: 140 },
  { title: '操作', key: 'op', width: 330 }
]

function taxCount(r) { return r.taxProofUrls ? r.taxProofUrls.split(',').filter(Boolean).length : 0 }

// ===== 分页与服务端筛选（MyBatis-Plus Page：records/total）=====
const pager = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (t) => `共 ${t} 家商户`
})

async function load() {
  loading.value = true
  try {
    const res = await merchantApi.adminPage({
      page: pager.current,
      size: pager.pageSize,
      reviewStatus: filter.value,
      status: statusFilter.value,
      keyword: keyword.value.trim() || undefined
    })
    list.value = res?.records || []
    pager.total = Number(res?.total || 0)
  } finally { loading.value = false }
}

/** 筛选条件变更：回到第一页再查询 */
function onFilterChange() {
  pager.current = 1
  load()
}

/** 关键词清空时立即恢复全量（输入过程中不打扰，回车/点搜索才查询） */
function onKeywordChange(e) {
  if (!e?.target?.value) onFilterChange()
}

function onTableChange(p) {
  pager.current = p.current
  pager.pageSize = p.pageSize
  load()
}

// ===== 详情抽屉 =====
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const taxUrls = computed(() => detail.value?.merchant?.taxProofUrls
  ? detail.value.merchant.taxProofUrls.split(',').filter(Boolean) : [])
const flowStep = computed(() => {
  const s = detail.value?.merchant?.reviewStatus
  return s === 1 ? 2 : (s === 2 ? 2 : 1)   // 待审核停在「资质核验」；通过/驳回完成
})
const readinessText = computed(() => {
  const r = detail.value?.certReadiness
  if (!r) return ''
  const n = [r.hasLicense, r.taxProofCount > 0, r.hasTaxRegNo].filter(Boolean).length
  return `材料 ${n}/3`
})
const reviewDesc = computed(() => {
  const s = detail.value?.merchant?.reviewStatus
  if (s === 1) return '已入驻'
  if (s === 2) return '已驳回'
  return '待审核'
})
function fmtTime(t) { return (t || '').replace('T', ' ').slice(0, 16) || '—' }

async function openDetail(record) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  fullTaxNo.value = '' // 每次打开重置明文展示，避免跨商户残留
  try { detail.value = await merchantApi.adminDetail(record.merId) }
  catch (e) { /* 拦截器已提示 */ }
  finally { detailLoading.value = false }
}

// ===== 审核操作 =====
const reviewing = ref(false)
const rejectOpen = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)

function approve(record) {
  Modal.confirm({
    title: '确认通过该商户入驻申请？',
    content: `商户 ${record.merId}（${record.regType}，注册资本 ¥${record.regCapital}）通过后即获得商户身份，可发布商品。`,
    okText: '确认通过', cancelText: '取消',
    onOk: async () => {
      reviewing.value = true
      try {
        await merchantApi.adminReview(record.merId, 1, null)
        message.success('已通过')
        if (detailOpen.value && detail.value?.merchant?.merId === record.merId) await openDetail(record)
        await load()
      } finally { reviewing.value = false }
    }
  })
}
function openReject(record) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectOpen.value = true
}
async function doReject() {
  if (!rejectReason.value.trim()) { message.warning('请填写驳回原因'); return }
  reviewing.value = true
  try {
    await merchantApi.adminReview(rejectTarget.value.merId, 2, rejectReason.value.trim())
    message.success('已驳回')
    rejectOpen.value = false
    if (detailOpen.value && detail.value?.merchant?.merId === rejectTarget.value.merId) await openDetail(rejectTarget.value)
    await load()
  } finally { reviewing.value = false }
}

// ===== 运营端维护：资料编辑 / 账户状态 / 权限配置 / 注销 =====
const editOpen = ref(false)
const editEnterprise = ref(null)
const statusOpen = ref(false)
const statusTarget = ref(2)
const permOpen = ref(false)
const deleteOpen = ref(false)
const deleting = ref(false)
const deleteReason = ref('')
const target = ref(null)
const fullTaxNo = ref('')

function openEdit(record, enterprise = null) {
  target.value = record
  editEnterprise.value = enterprise || detail.value?.enterprise || null
  editOpen.value = true
}
function openStatus(record) {
  target.value = record
  statusTarget.value = accStatus(record) === 2 ? 1 : 2
  statusOpen.value = true
}
function openPerm(record) {
  target.value = record
  permOpen.value = true
}
function openDelete(record) {
  target.value = record
  deleteReason.value = ''
  deleteOpen.value = true
}

async function doDelete() {
  if (!deleteReason.value.trim()) { message.warning('请填写注销原因'); return }
  deleting.value = true
  try {
    await merchantApi.adminDelete(target.value.merId, deleteReason.value.trim())
    message.success('商户已注销')
    deleteOpen.value = false
    await afterMutate()
  } catch (e) {
    console.error(e) // 响应拦截器已提示错误
  } finally {
    deleting.value = false
  }
}

/** 资料/状态/权限变更成功后：刷新列表，并同步刷新已打开的详情抽屉 */
async function afterMutate() {
  await load()
  const merId = target.value?.merId
  if (detailOpen.value && detail.value?.merchant?.merId === merId) {
    await openDetail(detail.value.merchant)
  }
}

/** 解密查看完整税务登记号（敏感操作，服务端会写审计日志） */
async function viewTaxRegNo(record) {
  try {
    const res = await merchantApi.adminTaxRegNo(record.merId)
    fullTaxNo.value = res.taxRegNo
    message.success('已解密展示完整税务登记号，该操作已记入审计日志')
  } catch (e) {
    console.error(e) // 响应拦截器已提示错误
  }
}

onMounted(load)
</script>

<style scoped>
.reason { font-size: 12px; color: #999; margin-top: 2px; max-width: 180px;
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sec-title { font-weight: 700; font-size: 14px; margin: 16px 0 10px; }
.cert-checks { display: flex; flex-direction: column; gap: 8px; }
.cert-check { display: flex; align-items: center; gap: 8px; padding: 8px 12px;
              border-radius: 8px; background: var(--ll-page, #f7f8fa); font-size: 13px; }
.cert-check.ok { background: rgba(16, 185, 129, .06); }
.ck { font-size: 15px; }
.ck.ok { color: #10b981; }
.ck.miss { color: #cbd5e1; }
.proof-list { display: flex; flex-wrap: wrap; gap: 8px; }
.proof-item { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px;
              border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; }
.proof-item:hover { border-color: var(--ll-primary, #1e6eb8); color: var(--ll-primary, #1e6eb8); }
</style>
