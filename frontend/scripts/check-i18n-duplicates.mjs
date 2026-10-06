import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const root = path.resolve(process.cwd(), 'src')
const expected = new Map([
  ['营业执照', 'Business License'],
  ['税务登记号', 'Tax Registration No.'],
  ['查看原件', 'View Original'],
  ['记录', 'Record'],
])

function escapeRegExp(value) {
  return value.replace(/[\\^$.*+?()[\\]{}|]/g, '\\\\$&')
}

const sourceFiles = []
function walk(dir) {
  if (!fs.existsSync(dir)) return
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full)
    else if (/\\.(js|jsx|ts|tsx|vue|json)$/.test(entry.name)) sourceFiles.push(full)
  }
}
walk(root)

const targetDefinitions = new Map()

for (const file of sourceFiles) {
  const content = fs.readFileSync(file, 'utf8')
  for (const [zh] of expected) {
    const re = new RegExp("['\\"]" + escapeRegExp(zh) + "['\\"]\\s*:", 'g')
    const matches = content.match(re) || []
    if (matches.length) {
      const key = path.relative(process.cwd(), file).replaceAll(path.sep, '/')
      targetDefinitions.set(zh, [...(targetDefinitions.get(zh) || []), ...matches.map(() => key)])
    }
  }
}

const failures = []

for (const [zh] of expected) {
  const locations = targetDefinitions.get(zh) || []
  if (locations.length !== 1) {
    failures.push(zh + ': expected exactly 1 definition, found ' + locations.length + ' (' + (locations.join(', ') || 'none') + ')')
  }
}

const i18nFile = path.resolve(process.cwd(), 'src/i18n/index.js')
const source = fs.readFileSync(i18nFile, 'utf8')
const start = source.indexOf('const UI_ZH_EN = {')
const end = source.indexOf('\n}\n\nconst _zhOrder', start)

if (start < 0 || end < 0) {
  failures.push('UI_ZH_EN dictionary block could not be located')
} else {
  const block = source.slice(start, end)
  const definitionPattern = /['"]([^'"]+)['"]\s*:/g
  const keys = [...block.matchAll(definitionPattern)].map(match => match[1])
  const counts = new Map()

  for (const key of keys) counts.set(key, (counts.get(key) || 0) + 1)
  for (const [key, count] of counts) {
    if (count > 1) failures.push('UI_ZH_EN duplicate key: ' + key + ' (' + count + ' definitions)')
  }

  for (const [zh, en] of expected) {
    const translation = new RegExp(
      "['\\"]" + escapeRegExp(zh) + "['\\"]\\s*:\\s*['\\"]" + escapeRegExp(en) + "['\\"]"
    )
    if (!translation.test(block)) failures.push(zh + ': expected translation ' + en)
  }
}

if (failures.length) {
  console.error('i18n duplicate/consistency check failed:')
  for (const failure of failures) console.error('- ' + failure)
  process.exit(1)
}

console.log('i18n duplicate/consistency check passed:')
for (const [zh, en] of expected) console.log('- ' + zh + ' -> ' + en)
