# Agent Loadout 邮件代理接入

本文说明如何将 Agent Loadout 接入 LANLinkshopping 的验证码发送和营销邮件工作流。

## 1. Railway 环境变量

在 **Railway → 后端服务 → Variables** 中设置：

- `MAIL_PROVIDER=agent-loadout`
- `AGENT_LOADOUT_MCP_URL=https://agent-loadout.com/api/mcp`
- `AGENT_LOADOUT_API_KEY`：填写在 Agent Loadout 后台重新生成的 API Key；只放入部署变量，不提交到 Git。
- `AGENT_LOADOUT_INBOX_ID`：已授权 Agent 的发件箱 ID（当前连接曾返回 `inb_1k4jbtj1763b8e8sdkcc0brd0`，如有变化以 `list_inboxes` 的结果为准）。
- `lanlink.verify-code.demo-mode=false`（生产必须关闭演示模式）。

设置变量后重新部署后端。Agent Loadout 发件人由配置的 inbox 决定；`MAIL_FROM` 不用于覆盖它。若 `MAIL_PROVIDER=auto`，代码会在 Agent Loadout 已完整配置时优先使用它，否则继续按现有 Resend/SMTP 方式选择。

**密钥说明：** API Key 曾被粘贴在聊天中，部署前应在 Agent Loadout 中撤销旧密钥并生成新密钥。不要把密钥写入 `.env.local`、源码、提交、日志或前端变量。

## 2. 验证码邮件

现有账号中心接口 `POST /api/user/email-code` 会继续生成 6 位验证码并由后端发送。Agent Loadout 通道不会将验证码返回给前端；验证码 5 分钟过期、成功校验后立即失效，并保留现有 60 秒冷却规则。

该接口当前服务于已登录用户的邮箱绑定/验证流程，不等于已经实现“登录注册全流程邮箱 OTP”。验证码存储仍是应用进程内存，生产多副本部署前应迁移到共享存储（例如 Redis），否则跨副本校验和重启恢复不可靠。

## 3. 营销邮件

新增管理员接口：

`POST /api/admin/marketing-email/send`

它要求已登录的平台运营账号，并通过 CSRF 令牌保护。先调用 `GET /api/user/csrf-token` 获取当前会话令牌，然后以请求头 `X-CSRF-TOKEN` 发送。

请求示例：

```json
{
  "recipients": ["subscribed-customer@example.com"],
  "subject": "LANLinkshopping 新供应商活动",
  "text": "查看本周的新供应商与采购机会。",
  "unsubscribeUrl": "https://your-domain.example/email/unsubscribe",
  "idempotencyKey": "campaign-2026-10-10-v1",
  "consentConfirmed": true
}
```

限制与行为：

- 一次最多 20 位收件人，逐人单独发送，避免泄露收件人地址。
- 只有管理员明确确认收件人已订阅时才发送。
- 主题会加上“商业推广”标识。
- 每位收件人的幂等键独立生成；接口返回排队数与失败数，不返回邮箱地址。
- `unsubscribeUrl` 必须是 HTTPS 地址并会写入正文。必须填入**真实有效的退订端点**；当前代码尚未实现自己的订阅同意/退订数据库，因此上线营销群发前还需要把该 URL 接入实际的退订处理流程，不能使用占位链接或向未订阅对象发送。

## 4. 验证

部署后先只给自己发一封测试邮件，确认 Agent Loadout inbox 处于 `ready`、API Key 具有 `email:send` 权限，再测试 `/api/user/email-code`。不要用营销接口做大批量首发测试。CI 单元测试会覆盖 MCP 初始化/会话、单收件人发送、幂等键和营销同意校验。
