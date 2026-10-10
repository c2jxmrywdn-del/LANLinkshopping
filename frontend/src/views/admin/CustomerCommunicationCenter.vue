<template>
  <div class="cc-page">
    <a-page-header title="客户沟通中心" sub-title="统一处理客户咨询、人工回复与受控知识沉淀">
      <template #extra>
        <a-button @click="load" :loading="loading"><template #icon><ReloadOutlined /></template>刷新数据</a-button>
        <a-button type="primary" @click="go('admin-communications-inbox')"><template #icon><MessageOutlined /></template>进入会话工作台</a-button>
      </template>
    </a-page-header>

    <a-alert class="privacy-alert" type="info" show-icon
      message="沟通数据保护与学习边界"
      description="网站客服会话保存前进行常见个人信息与凭据脱敏。对话只能生成待审核候选，必须人工核验后才能发布为知识条目；不会自动训练模型，也不读取客户订单、钱包或账户资料。" />

    <a-row :gutter="[16,16]" class="stat-grid">
      <a-col :xs="12" :lg="6">
        <a-card class="metric-card"><a-statistic title="待处理会话" :value="stats.needsReply || 0" :value-style="{color:'#b54708'}" /><div class="metric-note">需运营人员关注</div></a-card>
      </a-col>
      <a-col :xs="12" :lg="6">
        <a-card class="metric-card"><a-statistic title="未分配会话" :value="stats.unassigned || 0" :value-style="{color:'#175cd3'}" /><div class="metric-note">等待认领</div></a-card>
      </a-col>
      <a-col :xs="12" :lg="6">
        <a-card class="metric-card"><a-statistic title="今日会话" :value="stats.todayConversations || 0" /><div class="metric-note">按服务器业务日期统计</div></a-card>
      </a-col>
      <a-col :xs="12" :lg="6">
        <a-card class="metric-card"><a-statistic title="待审核知识" :value="stats.pendingLearning || 0" :value-style="{color:'#b42318'}" /><div class="metric-note">尚未影响线上客服</div></a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16,16]" class="action-grid">
      <a-col :xs="24" :lg="8">
        <a-card class="workflow-card" hoverable @click="go('admin-communications-inbox')">
          <div class="workflow-icon navy"><MessageOutlined /></div>
          <h3>会话工作台</h3>
          <p>筛选未分配、待回复和已解决会话；在单个会话中回复、添加内部备注、认领与调整优先级。</p>
          <a-button type="link" @click.stop="go('admin-communications-inbox')">打开工作台 <RightOutlined /></a-button>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card class="workflow-card" hoverable @click="go('admin-communications-learning')">
          <div class="workflow-icon amber"><BulbOutlined /></div>
          <h3>对话学习审核</h3>
          <p>从客户问题和人工回答中提取脱敏候选，检查关键词、回答事实、重复意图和安全范围，再决定发布或驳回。</p>
          <a-button type="link" @click.stop="go('admin-communications-learning')">查看候选 <RightOutlined /></a-button>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card class="workflow-card" hoverable @click="go('admin-support-learning')">
          <div class="workflow-icon pale"><ExperimentOutlined /></div>
          <h3>学习沙盒</h3>
          <p>对新问法和候选回答进行隔离试跑。沙盒预检通过不等于自动发布，正式知识依然需要人工审核。</p>
          <a-button type="link" @click.stop="go('admin-support-learning')">进入沙盒 <RightOutlined /></a-button>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="panel-card" :bordered="false">
      <template #title><span class="panel-title">最近更新的会话</span></template>
      <template #extra><a-button type="link" @click="go('admin-communications-inbox')">查看全部</a-button></template>
      <a-table :columns="columns" :data-source="recent" row-key="id" :loading="loading" :pagination="false" :scroll="{x:900}">
        <template #bodyCell="{column,record}">
          <template v-if="column.key==='subject'">
            <a-button type="link" class="subject-link" @click="openConversation(record)">{{ record.subject || '专属客服咨询' }}</a-button>
            <div class="muted small">{{ record.conversationNo }}</div>
          </template>
          <template v-else-if="column.key==='status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key==='priority'"><a-tag :color="priorityColor(record.priority)">{{ priorityLabel(record.priority) }}</a-tag></template>
          <template v-else-if="column.key==='last'"><span class="last-preview">{{ record.lastMessagePreview || '暂无摘要' }}</span><div class="muted small">{{ record.lastMessageAt || record.createdAt }}</div></template>
          <template v-else-if="column.key==='action'"><a-button size="small" @click="openConversation(record)">处理会话</a-button></template>
        </template>
        <template #emptyText><a-empty description="暂时没有客户会话"><template #description><span>网站客服收到客户咨询后，会自动在这里建立会话。</span></template></a-empty></template>
      </a-table>
    </a-card>
    <div class="page-foot"><span class="status-dot"></span> 客户沟通服务连接正常性以接口响应为准；此处仅展示当前账号有权访问的数据。</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined, MessageOutlined, RightOutlined, BulbOutlined, ExperimentOutlined } from '@ant-design/icons-vue'
import { adminApi } from '../../api'

const router = useRouter()
const loading = ref(false)
const stats = reactive({ needsReply:0, waitingCustomer:0, resolved:0, todayConversations:0, unassigned:0, pendingLearning:0, publishedLearning:0 })
const recent = ref([])
const columns = [
  {title:'客户会话',key:'subject',width:230},
  {title:'业务分类',dataIndex:'category',width:130},
  {title:'状态',key:'status',width:120},
  {title:'优先级',key:'priority',width:100},
  {title:'最近消息',key:'last',width:250},
  {title:'操作',key:'action',width:100,fixed:'right'}
]
function go(name) { router.push({name}) }
function openConversation(record) { router.push({name:'admin-communication-conversation',params:{conversationId:record.id}}) }
function statusLabel(v) { return ({open:'待处理',waiting_agent:'待客服回复',waiting_customer:'等待客户',resolved:'已解决'})[v] || '未知' }
function statusColor(v) { return ({open:'orange',waiting_agent:'red',waiting_customer:'blue',resolved:'green'})[v] || 'default' }
function priorityLabel(v) { return ({low:'低',normal:'普通',high:'高',urgent:'紧急'})[v] || '普通' }
function priorityColor(v) { return ({low:'default',normal:'blue',high:'orange',urgent:'red'})[v] || 'blue' }
async function load() {
  loading.value = true
  try {
    const [overview,page] = await Promise.all([
      adminApi.communicationOverview(),
      adminApi.communicationConversations({queue:'all',page:1,size:8})
    ])
    Object.assign(stats,overview || {})
    recent.value = page?.records || []
  } catch {
    message.error('沟通中心数据加载失败，请检查管理员权限或后端服务。')
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.cc-page{max-width:1600px;margin:0 auto}.privacy-alert{margin:0 0 18px}.stat-grid{margin-bottom:16px}.metric-card{height:100%;border:1px solid #e7eaf0;border-radius:14px;box-shadow:0 2px 8px rgba(16,24,40,.025)}.metric-note{margin-top:8px;color:#667085;font-size:12px}.action-grid{margin-bottom:18px}.workflow-card{height:100%;min-height:216px;border:1px solid #e7eaf0;border-radius:14px;transition:transform .18s ease,box-shadow .18s ease}.workflow-card:hover{transform:translateY(-2px);box-shadow:0 12px 28px rgba(16,24,40,.08)}.workflow-icon{display:grid;place-items:center;width:42px;height:42px;border-radius:12px;font-size:19px}.workflow-icon.navy{color:#fff;background:#13233a}.workflow-icon.amber{color:#8a4b08;background:#fff0d7}.workflow-icon.pale{color:#344054;background:#f2f4f7}.workflow-card h3{margin:14px 0 8px;color:#101828;font-size:15px;font-weight:650}.workflow-card p{min-height:56px;margin:0;color:#667085;font-size:13px;line-height:1.7}.panel-card{border:1px solid #e7eaf0;border-radius:14px}.panel-title{font-weight:650;color:#101828}.subject-link{padding:0;height:auto;font-weight:600;text-align:left}.muted{color:#98a2b3}.small{font-size:11px}.last-preview{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;color:#344054;font-size:12px}.page-foot{display:flex;align-items:center;gap:8px;margin-top:16px;color:#667085;font-size:12px}.status-dot{width:7px;height:7px;border-radius:50%;background:#12b76a;box-shadow:0 0 0 3px #dcfae6}
</style>
