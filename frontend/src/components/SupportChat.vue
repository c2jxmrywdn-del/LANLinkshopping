<template>
  <div class="support-root">
    <button class="support-launcher" type="button" :aria-expanded="visible"
      aria-controls="lanlink-support-panel" @click="toggle">
      <span class="support-launcher-mark" aria-hidden="true">?</span>
      <span>专属客服</span>
    </button>

    <section v-if="visible" id="lanlink-support-panel" class="support-panel"
      role="dialog" aria-label="LANLinkshopping 专属客服">
      <header class="support-header">
        <div>
          <strong>LANLinkshopping 客服</strong>
          <p>只回答专属知识库已收录的电商问题</p>
        </div>
        <button class="support-close" type="button" aria-label="关闭客服" @click="visible = false">×</button>
      </header>

      <div ref="messageList" class="support-messages" aria-live="polite">
        <div v-for="(item, index) in messages" :key="index" class="support-message"
          :class="item.role === 'user' ? 'from-user' : 'from-support'">
          <div class="support-message-label">{{ item.role === 'user' ? '你' : '专属客服' }}</div>
          <p>{{ item.text }}</p>
        </div>
        <div v-if="loading" class="support-message from-support">
          <div class="support-message-label">专属客服</div>
          <p>正在检索知识库…</p>
        </div>
      </div>

      <form class="support-compose" @submit.prevent="send">
        <label class="support-sr-only" for="support-question">输入电商相关问题</label>
        <textarea id="support-question" v-model="draft" :disabled="loading" maxlength="300" rows="2"
          placeholder="请输入电商相关问题（最多 300 字）"
          @keydown.enter.exact.prevent="send" />
        <div class="support-compose-footer">
          <span>{{ draft.length }}/300</span>
          <button type="submit" :disabled="loading || !draft.trim()">发送</button>
        </div>
      </form>
      <footer class="support-scope-note">未收录的问题将返回统一引导提示；客服不会查询个人账号、订单或钱包数据。</footer>
    </section>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { supportApi } from '../api'

const FALLBACK = '抱歉，当前客服仅能解答 LANLinkshopping 专属知识库已收录的电商相关问题。可咨询账号注册与登录、商品浏览、购物车与订单、商户入驻、钱包与账期、会员与活动、站内消息及 Cookie 设置；其他问题暂不在可答范围内。'
const WELCOME = '您好，我是 LANLinkshopping 专属客服。请咨询平台账号注册与登录、商品浏览、购物车与订单、商户入驻、钱包与账期、会员与活动、站内消息或 Cookie 设置等已收录问题。'

const visible = ref(false)
const draft = ref('')
const loading = ref(false)
const messageList = ref(null)
const messages = ref([{ role: 'assistant', text: WELCOME }])

function toggle() { visible.value = !visible.value }

watch(messages, async () => {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}, { deep: true })

async function send() {
  const question = draft.value.trim()
  if (!question || loading.value) return
  if (question.length > 300) {
    messages.value.push({ role: 'assistant', text: FALLBACK })
    return
  }

  messages.value.push({ role: 'user', text: question })
  draft.value = ''
  loading.value = true
  try {
    const result = await supportApi.ask(question)
    const answer = typeof result?.answer === 'string' && result.answer ? result.answer : FALLBACK
    messages.value.push({ role: 'assistant', text: answer })
  } catch {
    messages.value.push({ role: 'assistant', text: FALLBACK })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.support-root { position: fixed; right: 24px; bottom: 24px; z-index: 1200; font-family: inherit; }
.support-launcher { display: inline-flex; align-items: center; gap: 9px; border: 0; border-radius: 999px; padding: 12px 18px 12px 12px; color: #fff; background: #13233A; box-shadow: 0 10px 30px rgba(19,35,58,.24); font-size: 14px; font-weight: 650; cursor: pointer; }
.support-launcher:hover { background: #203958; }
.support-launcher-mark { display: grid; place-items: center; width: 28px; height: 28px; border: 1px solid rgba(255,255,255,.55); border-radius: 50%; font-size: 17px; }
.support-panel { display: flex; flex-direction: column; position: absolute; right: 0; bottom: 62px; width: min(370px, calc(100vw - 28px)); height: min(560px, calc(100vh - 110px)); background: #fff; border: 1px solid #e4e7ec; border-radius: 18px; box-shadow: 0 20px 55px rgba(16,24,40,.2); overflow: hidden; }
.support-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 17px 18px; background: #13233A; color: #fff; }
.support-header strong { display: block; font-size: 14px; font-weight: 700; }
.support-header p { margin: 4px 0 0; color: rgba(255,255,255,.78); font-size: 11px; }
.support-close { border: 0; background: transparent; color: #fff; font-size: 26px; line-height: 1; cursor: pointer; }
.support-messages { display: flex; flex: 1; flex-direction: column; align-items: stretch; gap: 12px; overflow-y: auto; padding: 16px 13px; background: #f8f9fb; }
.support-message { max-width: 92%; padding: 10px 12px; border-radius: 13px; border: 1px solid #e7eaf0; background: #fff; overflow-wrap: anywhere; }
.support-message.from-user { align-self: flex-end; border-color: #d6e4f5; background: #eef5ff; }
.support-message.from-support { align-self: flex-start; }
.support-message-label { margin-bottom: 5px; color: #667085; font-size: 10px; font-weight: 700; }
.support-message p { margin: 0; color: #182230; font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
.support-compose { padding: 11px 12px 9px; border-top: 1px solid #e4e7ec; background: #fff; }
.support-compose textarea { display: block; box-sizing: border-box; width: 100%; resize: none; border: 1px solid #d0d5dd; border-radius: 10px; padding: 10px; outline: none; color: #182230; background: #fff; font: inherit; font-size: 12px; line-height: 1.6; }
.support-compose textarea:focus { border-color: #587ca5; box-shadow: 0 0 0 3px rgba(88,124,165,.12); }
.support-compose-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; color: #98a2b3; font-size: 10px; }
.support-compose-footer button { padding: 7px 14px; border: 0; border-radius: 8px; color: #fff; background: #13233A; font-size: 12px; font-weight: 650; cursor: pointer; }
.support-compose-footer button:disabled { opacity: .45; cursor: not-allowed; }
.support-scope-note { padding: 0 12px 12px; color: #667085; background: #fff; font-size: 10px; line-height: 1.5; }
.support-sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 520px) { .support-root { right: 12px; bottom: 12px; } .support-panel { right: -4px; bottom: 58px; height: min(540px, calc(100vh - 90px)); } }
</style>
