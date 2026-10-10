import { isIP } from "node:net";

const ORIGIN = "https://lan-linkshopping.vercel.app";
const BACKEND = "https://lanlinkshopping-production.up.railway.app/api/support/ask";
const FALLBACK = "抱歉，当前客服仅能解答 LANLinkshopping 专属知识库已收录的电商相关问题。可咨询账号注册与登录、商品浏览、购物车与订单、商户入驻、钱包与账期、会员与活动、站内消息及 Cookie 设置；其他问题暂不在可答范围内。";
const LIMIT = 20, UNKNOWN_IP_LIMIT = 300, WINDOW_MS = 60_000, MAX_TRACKED = 10_000;
const rateWindows = globalThis.__lanlinkSupportRateWindows || new Map();
globalThis.__lanlinkSupportRateWindows = rateWindows;

function header(req, name) {
  const value = req.headers[name.toLowerCase()];
  return Array.isArray(value) ? String(value[0] || "") : String(value || "");
}
function trustedBrowserRequest(req) {
  const origin = header(req, "origin");
  const fetchSite = header(req, "sec-fetch-site");
  const referer = header(req, "referer");
  if (origin && origin !== ORIGIN) return false;
  if (fetchSite && fetchSite.toLowerCase() !== "same-origin") return false;
  if (referer) {
    try { if (new URL(referer).origin !== ORIGIN) return false; } catch { return false; }
  }
  // If Origin is omitted, require same-origin Fetch Metadata and a same-origin Referer.
  if (!origin && (!referer || !fetchSite || fetchSite.toLowerCase() !== "same-origin")) return false;
  return true;
}
function clientIp(req) {
  const realIp = header(req, "x-real-ip").trim();
  if (isIP(realIp)) return realIp;
  const forwarded = header(req, "x-forwarded-for").split(",")[0].trim();
  if (isIP(forwarded)) return forwarded;
  const remote = String(req.socket?.remoteAddress || "").trim();
  return isIP(remote) ? remote : "unknown";
}
function allowRate(ip) {
  if (!ip) return false;
  const now = Date.now();
  if (rateWindows.size >= MAX_TRACKED) {
    for (const [key, bucket] of rateWindows) if (now - bucket.startedAt >= WINDOW_MS) rateWindows.delete(key);
  }
  if (rateWindows.size >= MAX_TRACKED && !rateWindows.has(ip)) return false;
  const existing = rateWindows.get(ip);
  if (!existing || now - existing.startedAt >= WINDOW_MS) {
    rateWindows.set(ip, { startedAt: now, count: 1 }); return true;
  }
  if (existing.count >= (ip === "unknown" ? UNKNOWN_IP_LIMIT : LIMIT)) return false;
  existing.count += 1; return true;
}
function send(res, status, answer) {
  res.setHeader("Cache-Control", "no-store, max-age=0");
  res.setHeader("Pragma", "no-cache");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Vary", "Origin");
  return res.status(status).json({ code: status === 200 ? 200 : status, message: answer, data: { answer } });
}

export default async function handler(req, res) {
  if (req.method !== "POST") { res.setHeader("Allow", "POST"); return send(res, 405, FALLBACK); }
  if (!trustedBrowserRequest(req)) return send(res, 403, FALLBACK);
  const length = Number(header(req, "content-length") || "0");
  if (Number.isFinite(length) && length > 4096) return send(res, 413, FALLBACK);
  const ip = clientIp(req);
  if (!allowRate(ip)) return send(res, 429, FALLBACK);
  if (!header(req, "content-type").toLowerCase().startsWith("application/json")) return send(res, 400, FALLBACK);

  let body = req.body;
  if (typeof body === "string") { try { body = JSON.parse(body); } catch { return send(res, 200, FALLBACK); } }
  if (!body || typeof body !== "object" || Array.isArray(body)
      || typeof body.question !== "string" || body.question.length > 300) return send(res, 200, FALLBACK);

  const secret = process.env.SUPPORT_PROXY_SECRET;
  if (typeof secret !== "string" || secret.length < 40) return send(res, 503, FALLBACK);

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8_000);
  timeout.unref?.();
  try {
    const upstream = await fetch(BACKEND, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Support-Proxy-Secret": secret, "X-Support-Client-IP": ip },
      body: JSON.stringify({ question: body.question }),
      cache: "no-store", redirect: "error", signal: controller.signal
    });
    const payload = await upstream.json().catch(() => null);
    const answer = typeof payload?.data?.answer === "string" ? payload.data.answer
      : typeof payload?.message === "string" ? payload.message : FALLBACK;
    if ([403,429,413].includes(upstream.status)) return send(res, upstream.status, FALLBACK);
    if (!upstream.ok || !payload) return send(res, 503, FALLBACK);
    return send(res, 200, answer);
  } catch {
    return send(res, 503, FALLBACK);
  } finally {
    clearTimeout(timeout);
  }
}
