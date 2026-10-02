<template>
  <div>
    <!-- ===== 证照管理（商户专属：营业执照 / 税务证明 / 税务登记号查询） ===== -->
    <a-card title="证照管理" class="cert-card" v-if="!notMerchant">
      <a-row :gutter="24">
        <!-- 营业执照 -->
        <a-col :span="12">
          <div class="cert-title">营业执照</div>
          <a-upload v-model:file-list="licenseFiles" list-type="picture-card" accept="image/jpeg,image/png"
                    :max-count="1" :before-upload="beforeLicense" :custom-request="doUploadLicense">
            <div v-if="licenseFiles.length === 0">
              <plus-outlined />
              <div class="up-hint">上传执照</div>
            </div>
          </a-upload>
          <div class="hint">JPG/PNG，≤5MB，需≥400×400 完整清晰</div>
          <div v-if="cert.licenseUrl" class="cert-link">已上传：<a :href="cert.licenseUrl" target="_blank">查看</a></div>
        </a-col>
        <!-- 税务记录 -->
        <a-col :span="12">
          <div class="cert-title">税务缴纳证明（近 3 个月，最多 3 份）</div>
          <a-upload v-model:file-list="taxFiles" accept=".pdf,.jpg,.jpeg,.png"
                    :max-count="3" :before-upload="beforeTaxFile" :custom-request="doUploadTaxProof">
            <a-button><upload-outlined />上传证明（PDF/JPG）</a-button>
          </a-upload>
          <div class="tax-query-row">
            <a-input v-model:value="taxRegNo" placeholder="税务登记号查询（15/18/20 位）" style="flex:1" allow-clear />
            <a-button type="primary" :loading="querying" @click="queryTax">查询</a-button>
          </div>
          <div v-if="taxRecords" class="tax-ok">已查询到近 3 个月缴纳记录，合计 ¥{{ taxRecords.totalAmount }}（登记号已绑定）</div>
        </a-col>
      </a-row>
    </a-card>

    <a-card title="我的商品">
      <template #extra>
        <a-space>
          <a-button @click="load">刷新</a-button>
          <a-button type="primary" @click="publishOpen = true">＋ 发布商品</a-button>
        </a-space>
      </template>

      <a-alert v-if="notMerchant" type="warning" show-icon style="margin-bottom:16px"
               message="尚未成为入驻商户"
               description="完成商户入驻并通过审核后，即可发布商品与管理证照。">
        <template #action>
          <a-button size="small" type="primary" @click="$router.push('/merchant')">去入驻</a-button>
        </template>
      </a-alert>

      <a-table v-else :data-source="rows" :columns="cols" row-key="prodId" :loading="loading"
               :pagination="{ current, pageSize: 10, total, onChange: (p) => { current = p; load() } }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'prod'">
            <div class="prod-cell">
              <div class="thumb">
                <img v-if="record.coverUrl" :src="record.coverUrl" :alt="record.title" loading="lazy" />
                <span v-else>{{ (record.title || '').slice(0, 2) }}</span>
              </div>
              <div class="pt">
                <div class="pt-title">{{ record.title }}</div>
                <div class="pt-meta">{{ record.brand || '—' }} · {{ record.spec || '无规格' }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'price'">¥{{ record.price }}</template>
          <template v-else-if="column.key === 'stock'">{{ record.stock }}</template>
          <template v-else-if="column.key === 'rv'">
            <a-tooltip v-if="record.reviewStatus === 2 && record.rejectReason" :title="'驳回原因：' + record.rejectReason">
              <a-tag :color="rvColor(record.reviewStatus)">{{ rvText(record.reviewStatus) }} ⓘ</a-tag>
            </a-tooltip>
            <a-tag v-else :color="rvColor(record.reviewStatus)">{{ rvText(record.reviewStatus) }}</a-tag>
            <div v-if="record.reviewStatus === 1" class="saleable">{{ record.status === 1 ? '可售中' : '已下架' }}</div>
          </template>
          <template v-else-if="column.key === 'time'">{{ (record.createTime || '').replace('T', ' ') }}</template>
        </template>
      </a-table>
    </a-card>

    <!-- 发布商品组件（含草稿/表单校验/图片上传） -->
    <ProductPublishForm v-model:open="publishOpen" @published="load" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { productApi, merchantApi } from '../api'
import ProductPublishForm from '../components/merchant/ProductPublishForm.vue'

const rows = ref([])
const loading = ref(false)
const notMerchant = ref(false)
const publishOpen = ref(false)
const current = ref(1)
const total = ref(0)

// ===== 证照管理（商户专属：上传即绑定商户档案） =====
const cert = reactive({ licenseUrl: '' })
const licenseFiles = ref([])
const taxFiles = ref([])
const taxRegNo = ref('')
const querying = ref(false)
const taxRecords = ref(null)

function beforeLicense(file) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) { message.error('营业执照仅支持 JPG/PNG 格式'); return false }
  if (file.size > 5 * 1024 * 1024) { message.error('图片不能超过 5MB'); return false }
  if (file.size < 30 * 1024) { message.error('图片过小，可能不清晰，请重新拍摄或扫描'); return false }
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      if (img.naturalWidth < 400 || img.naturalHeight < 400) {
        message.error('图片尺寸过小（需≥400×400），请上传完整清晰的营业执照'); reject()
      } else { resolve(true) }
    }
    img.onerror = () => { message.error('图片无法解析，请更换文件'); reject() }
    img.src = URL.createObjectURL(file)
  })
}
function doUploadLicense({ file, onSuccess, onError }) {
  const fd = new FormData()
  fd.append('file', file)
  merchantApi.uploadLicense(fd)
    .then(data => { cert.licenseUrl = data.url; onSuccess(data) })
    .catch(e => onError(e))
}
function beforeTaxFile(file) {
  if (!['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)) { message.error('仅支持 PDF/JPG/PNG 格式'); return false }
  if (file.size > 5 * 1024 * 1024) { message.error('单个文件不能超过 5MB'); return false }
  return true
}
function doUploadTaxProof({ file, onSuccess, onError }) {
  const fd = new FormData()
  fd.append('file', file)
  merchantApi.uploadTaxProof(fd)
    .then(data => onSuccess(data))
    .catch(e => onError(e))
}
async function queryTax() {
  const no = taxRegNo.value.trim().toUpperCase()
  if (!/^[0-9A-Z]{15}$|^[0-9A-Z]{18}$|^[0-9A-Z]{20}$/.test(no)) { message.error('税务登记号格式不正确（15/18/20 位数字或大写字母）'); return }
  querying.value = true
  try {
    taxRecords.value = await merchantApi.taxQuery(no)
    message.success('查询成功，登记号已绑定至商户档案')
  } finally { querying.value = false }
}
async function loadCert() {
  try {
    const m = await merchantApi.my()
    if (m) {
      cert.licenseUrl = m.licenseUrl || ''
      if (cert.licenseUrl) licenseFiles.value = [{ uid: '-1', name: 'license', status: 'done', url: cert.licenseUrl }]
      if (m.taxRegNo) taxRegNo.value = m.taxRegNo
    }
  } catch (e) { /* 拦截器已提示 */ }
}

const cols = [
  { title: '商品', key: 'prod' },
  { title: '价格', key: 'price', width: 110 },
  { title: '库存', key: 'stock', width: 90 },
  { title: '销量', dataIndex: 'sales', width: 90 },
  { title: '审核状态', key: 'rv', width: 140 },
  { title: '发布时间', key: 'time', width: 170 }
]

/** 审核状态展示：待审核(审核中)/审核通过/审核驳回 */
function rvText(s) {
  return { 0: '待审核 · 审核中', 1: '审核通过', 2: '审核驳回' }[s] ?? '未知'
}
function rvColor(s) {
  return { 0: 'orange', 1: 'green', 2: 'red' }[s] ?? 'default'
}

async function load() {
  loading.value = true
  try {
    const page = await productApi.myList({ current: current.value, size: 10 })
    rows.value = page.records || []
    total.value = page.total || 0
    notMerchant.value = false
  } catch (e) {
    // 「请先完成商户入驻」等业务错误：切换为引导视图
    notMerchant.value = true
    rows.value = []
  } finally { loading.value = false }
}

onMounted(() => { load(); loadCert() })
</script>

<style scoped>
.cert-card { margin-bottom: 16px; border-radius: 12px; }
.cert-title { font-weight: 600; margin-bottom: 10px; }
.up-hint { font-size: 12px; margin-top: 4px; }
.hint { font-size: 12px; color: var(--ll-gray2, #4b5563); margin-top: 4px; }
.cert-link { font-size: 13px; margin-top: 6px; }
.tax-query-row { display: flex; gap: 8px; margin-top: 12px; }
.tax-ok { font-size: 13px; color: #10b981; margin-top: 8px; }
.prod-cell { display: flex; gap: 10px; align-items: center; }
.thumb { width: 48px; height: 48px; border-radius: 6px; background: var(--ll-thumb-bg, #eef1f6);
         display: flex; align-items: center; justify-content: center; overflow: hidden;
         font-size: 16px; color: var(--ll-thumb-fg, #b9c3d4); flex-shrink: 0; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.pt-title { font-size: 13px; }
.pt-meta { font-size: 12px; color: var(--ll-muted, #64748b); }
.saleable { font-size: 12px; color: var(--ll-muted, #64748b); margin-top: 2px; }
</style>
