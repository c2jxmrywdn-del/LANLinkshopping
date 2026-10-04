<template>
  <div class="about-wrap">
    <!-- 页内锚点导航 -->
    <div class="anchor-bar">
      <a-anchor :offset-top="80" :items="anchors" @click="onAnchor" />
    </div>

    <a-card :bordered="false" class="ab-card" id="company">
      <template #title><span class="ab-ico">🏢</span> 公司介绍</template>
      <p>
        <b>LANLinkshopping</b> 是一个聚合型、一体多元化的 B2B 电商解决方案平台，致力于为中小企业与
        各行业商户提供商品展示、在线交易、供应链协同的一站式数字化服务。
        平台连接采购方、入驻商户与平台运营三方，通过身份识别、会员体系、营销中台与多渠道支付系统，
        构建安全、高效、可信赖的企业级采购体验。
      </p>
      <div class="ab-grid">
        <div class="ab-item"><div class="ab-num">4+</div><div class="ab-label">覆盖行业（建筑/纺织/石化/电子）</div></div>
        <div class="ab-item"><div class="ab-num">5</div><div class="ab-label">用户身份体系（访客/采购方/VIP/商户/运营）</div></div>
        <div class="ab-item"><div class="ab-num">3</div><div class="ab-label">营销中台模块（活动/促销/会员）</div></div>
        <div class="ab-item"><div class="ab-num">4</div><div class="ab-label">支付渠道（钱包/微信/支付宝/模拟）</div></div>
      </div>
    </a-card>

    <a-card :bordered="false" class="ab-card" id="contact">
      <template #title><span class="ab-ico">📞</span> 联系方式</template>
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="客服热线">400-000-0000（工作日 9:00 - 18:00）</a-descriptions-item>
        <a-descriptions-item label="商务合作">business@lanlinkshopping.example.com</a-descriptions-item>
        <a-descriptions-item label="技术支持">support@lanlinkshopping.example.com</a-descriptions-item>
        <a-descriptions-item label="公司地址">示例市示例区科技创新园区 LANLink 大厦</a-descriptions-item>
      </a-descriptions>
      <a-alert type="info" show-icon class="ab-tip"
               message="本站为毕业设计演示环境，以上联系方式为示例内容。" />
    </a-card>

    <a-card :bordered="false" class="ab-card" id="terms">
      <template #title><span class="ab-ico">📄</span> 服务条款</template>
      <ol class="ab-legal">
        <li><b>账号与实名</b>：用户应使用真实企业信息注册并完成身份认证，妥善保管账号密码及两步验证凭证，因账号保管不善造成的损失由用户自行承担。</li>
        <li><b>交易规范</b>：平台商品信息由入驻商户发布并经平台审核；采购方下单前应仔细核对商品规格、价格与库存，订单一经支付即构成有效交易凭证。</li>
        <li><b>支付与退款</b>：平台支持钱包余额、微信支付、支付宝等渠道。订单退款将原路退回支付账户；使用积分抵现的订单退款后积分按规则返还/扣回。</li>
        <li><b>商户义务</b>：入驻商户应保证商品质量与证照信息真实有效，按约定时效发货；违规商户平台有权下架商品直至终止合作。</li>
        <li><b>免责声明</b>：因不可抗力、系统维护等导致的服务中断，平台将提前公告并及时恢复，但不承担由此产生的间接损失。</li>
      </ol>
    </a-card>

    <a-card :bordered="false" class="ab-card" id="privacy">
      <template #title><span class="ab-ico">🔒</span> 隐私政策</template>
      <ol class="ab-legal">
        <li><b>信息收集</b>：为提供交易与物流服务，我们收集账号信息（手机号、企业信息）、收货地址与订单数据；敏感凭证（支付密钥、证书）加密存储且绝不外泄。</li>
        <li><b>信息使用</b>：用户数据仅用于身份识别、订单履约、会员权益计算与安全审计（登录日志、越权记录），不出售或共享给任何无关第三方。</li>
        <li><b>信息保护</b>：全站采用 HTTPS 传输、BCrypt 密码散列、支付流水脱敏日志与权限拦截体系，支付渠道交互遵循微信支付/支付宝官方安全规范。</li>
        <li><b>用户权利</b>：您可在「个人中心」查看、更正资料，管理收货地址与第三方授权；注销账号后数据将按法规要求删除或匿名化。</li>
        <li><b>Cookie 说明</b>：仅使用必要会话 Cookie 维持登录态，不用于跨站广告追踪。</li>
      </ol>
    </a-card>
  </div>
</template>

<script setup>
const anchors = [
  { key: 'company', href: '#company', title: '公司介绍' },
  { key: 'contact', href: '#contact', title: '联系方式' },
  { key: 'terms', href: '#terms', title: '服务条款' },
  { key: 'privacy', href: '#privacy', title: '隐私政策' }
]

// 锚点点击平滑滚动（卡片 id 与锚点一致）
function onAnchor(e, link) {
  e.preventDefault()
  document.getElementById(link.href.slice(1))?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.about-wrap { max-width: 860px; margin: 0 auto; position: relative; }
.anchor-bar { position: sticky; top: 72px; z-index: 10; margin-bottom: 12px;
              background: var(--ll-page, #f5f6f8); }
.ab-card { border-radius: 12px; margin-bottom: 16px; scroll-margin-top: 140px; }
.ab-ico { margin-right: 8px; }
.ab-card p { line-height: 1.9; color: var(--ll-gray, #4b5563); margin: 0 0 14px; }
.ab-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.ab-item { background: rgba(30, 110, 184, .05); border-radius: 10px; padding: 14px 10px; text-align: center; }
.ab-num { font-size: 22px; font-weight: 800; color: var(--ll-primary, #1e6eb8); }
.ab-label { font-size: 12px; color: var(--ll-muted, #64748b); margin-top: 4px; line-height: 1.5; }
.ab-tip { margin-top: 14px; }
.ab-legal { padding-left: 20px; margin: 0; color: var(--ll-gray, #4b5563); }
.ab-legal li { line-height: 2; margin-bottom: 6px; }
.ab-legal b { color: var(--ll-ink, #0f172a); }
@media (max-width: 640px) { .ab-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
