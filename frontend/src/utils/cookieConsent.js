/**
 * LANLinkshopping Cookie Consent
 * 必要 Cookie 永远启用；其余类别由用户选择。
 */
const STORAGE_KEY = 'll_cookie_consent_v1'
const COOKIE_KEY = 'll_cookie_consent'
const DEFAULTS = { necessary: true, preferences: false, analytics: false, marketing: false }

function safeParse(raw) {
  try { return raw ? JSON.parse(raw) : null } catch (e) { return null }
}

export function readCookie(name) {
  if (typeof document === 'undefined') return ''
  const item = document.cookie.split('; ').find(row => row.startsWith(name + '='))
  return item ? decodeURIComponent(item.split('=').slice(1).join('=')) : ''
}

export function readCookieConsent() {
  if (typeof window === 'undefined') return null
  const stored = safeParse(localStorage.getItem(STORAGE_KEY))
  if (stored && stored.version === 1 && stored.categories) {
    return { version: 1, updatedAt: stored.updatedAt || null, categories: { ...DEFAULTS, ...stored.categories } }
  }
  const cookie = safeParse(readCookie(COOKIE_KEY))
  if (cookie && cookie.version === 1 && cookie.categories) {
    return { version: 1, updatedAt: cookie.updatedAt || null, categories: { ...DEFAULTS, ...cookie.categories } }
  }
  return null
}

export function saveCookieConsent(categories) {
  const consent = {
    version: 1,
    updatedAt: new Date().toISOString(),
    categories: { ...DEFAULTS, ...categories, necessary: true }
  }
  if (typeof window !== 'undefined') localStorage.setItem(STORAGE_KEY, JSON.stringify(consent))
  if (typeof document !== 'undefined') {
    document.cookie = COOKIE_KEY + '=' + encodeURIComponent(JSON.stringify(consent)) +
      '; Max-Age=31536000; Path=/; SameSite=Lax'
  }
  window.dispatchEvent(new CustomEvent('ll-cookie-consent-changed', { detail: consent }))
  return consent
}

export function hasCookieConsent(category) {
  const consent = readCookieConsent()
  if (category === 'necessary') return true
  return !!(consent && consent.categories && consent.categories[category])
}

export function openCookiePreferences() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('ll-open-cookie-preferences'))
  }
}

export function clearOptionalCookies() {
  if (typeof document === 'undefined') return
  // 清理本平台明确命名的可选客户端 Cookie；不触碰服务端会话 Cookie。
  const names = ['ll_preferences', 'll_analytics', 'll_marketing']
  for (const name of names) document.cookie = name + '=; Max-Age=0; Path=/; SameSite=Lax'
}
