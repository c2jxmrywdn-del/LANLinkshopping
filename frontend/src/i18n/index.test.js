import { describe, it, expect, beforeEach } from 'vitest'
import indexSource from './index.js?raw'
import { t, setLocale } from './index'

describe('轻量 i18n', () => {
  beforeEach(() => setLocale('zh-CN'))

  it('中文取值', () => {
    expect(t('acct.nav.profile')).toBe('个人信息')
    expect(t('acct.profile.male')).toBe('男')
  })

  it('英文切换', () => {
    setLocale('en-US')
    expect(t('acct.nav.profile')).toBe('Profile')
    expect(t('acct.profile.male')).toBe('Male')
  })

  it('插值 {n}', () => {
    expect(t('acct.profile.bioHelp', { n: 12 })).toBe('剩余可输入 12 字')
  })

  it('未知 key 回退到 key 本身', () => {
    expect(t('no.such.key')).toBe('no.such.key')
  })


  it('业务词条不得重复定义且核心商户资质翻译保持一致', async () => {
    const source = indexSource
    const start = source.indexOf('const UI_ZH_EN = {')
    const end = source.indexOf('\n}\n\nconst _zhOrder', start)
    expect(start).toBeGreaterThanOrEqual(0)
    expect(end).toBeGreaterThan(start)

    const dictionarySource = source.slice(start, end)
    const keys = [...dictionarySource.matchAll(/^\s*'((?:\\.|[^'])+)'\s*:/gm)].map(m => m[1])
    const occurrences = key => keys.filter(k => k === key).length

    for (const key of ['营业执照', '税务登记号', '查看原件', '记录']) {
      expect(occurrences(key), `duplicate translation key: ${key}`).toBe(1)
    }

    const expected = {
      '营业执照': 'Business License',
      '税务登记号': 'Tax Registration No.',
      '查看原件': 'View Original',
      '记录': 'Record',
    }

    for (const [zh, en] of Object.entries(expected)) {
      const pattern = new RegExp(`'${zh}'\\s*:\\s*'${en}'`)
      expect(dictionarySource).toMatch(pattern)
    }
  })

  it('非法语言回退中文', () => {
    setLocale('fr-FR')
    expect(t('acct.nav.profile')).toBe('个人信息')
  })
})