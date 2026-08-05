import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('../src/views/system/template-attachment/index.vue', import.meta.url), 'utf8')

test('batch dialog idempotency key no longer depends on crypto.randomUUID (insecure HTTP context)', () => {
  assert.match(page, /import \{ internalApi \} from '\.\.\/\.\.\/\.\.\/api\/internal-api'/)
  assert.match(page, /import \{ randomTraceId \} from '@srm\/api-client'/)
  assert.doesNotMatch(page, /crypto\.randomUUID/)
  assert.match(page, /const trace=randomTraceId\(\)/)
  assert.match(page, /idempotencyKey:`\$\{trace\.slice\(0,8\)\}-\$\{trace\.slice\(8,12\)\}-\$\{trace\.slice\(12,16\)\}-\$\{trace\.slice\(16,20\)\}-\$\{trace\.slice\(20\)\}`/)
  assert.match(page, /objectType:'MATERIAL'/)
})

test('uuid-shaped formatting of the 32-char trace id stays within backend constraints', () => {
  const trace = '0123456789abcdef0123456789abcdef'
  const key = `${trace.slice(0,8)}-${trace.slice(8,12)}-${trace.slice(12,16)}-${trace.slice(16,20)}-${trace.slice(20)}`
  assert.equal(key, '01234567-89ab-cdef-0123-456789abcdef')
  assert.match(key, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
  assert.ok(key.length <= 80 && key.trim().length > 0)
})
