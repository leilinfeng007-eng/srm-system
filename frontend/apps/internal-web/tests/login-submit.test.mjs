import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { randomTraceId } from '../../../packages/api-client/src/trace-id.ts'

const loginPage = readFileSync(new URL('../src/views/auth/LoginPage.vue', import.meta.url), 'utf8')
const authStore = readFileSync(new URL('../src/stores/auth.ts', import.meta.url), 'utf8')
const internalApi = readFileSync(new URL('../src/api/internal-api.ts', import.meta.url), 'utf8')
const apiClient = readFileSync(new URL('../../../packages/api-client/src/index.ts', import.meta.url), 'utf8')

const overrideCrypto = (value) => {
  const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
  Object.defineProperty(globalThis, 'crypto', { value, configurable: true, writable: true })
  return () => {
    if (original) Object.defineProperty(globalThis, 'crypto', original)
    else delete globalThis.crypto
  }
}

test('randomTraceId produces a 32-char hex id when crypto.randomUUID is unavailable (insecure HTTP context)', () => {
  const restore = overrideCrypto({
    getRandomValues: (bytes) => { bytes.fill(7); return bytes },
  })
  try {
    const id = randomTraceId()
    assert.match(id, /^[0-9a-f]{32}$/)
    assert.equal(id, '07'.repeat(16))
  } finally {
    restore()
  }
})

test('randomTraceId falls back to Math.random when no crypto API exists at all', () => {
  const restore = overrideCrypto({})
  try {
    assert.match(randomTraceId(), /^[0-9a-f]{32}$/)
    assert.match(randomTraceId(), /^[0-9a-f]{32}$/)
  } finally {
    restore()
  }
})

test('a valid login form must actually call the login API', () => {
  assert.match(loginPage, /@submit\.prevent="submit"/)
  assert.match(loginPage, /auth\.login\(form\.username\.trim\(\), form\.password\)/)
  assert.match(loginPage, /:disabled="!form\.username \|\| !form\.password"/)
  assert.match(authStore, /\/auth\/login/)
  assert.match(authStore, /retryAuthentication: false/)
})

test('login success saves auth state then navigates to the workbench', () => {
  assert.match(authStore, /setAccessToken\(token\.accessToken\)/)
  assert.match(authStore, /this\.user = token\.user/)
  assert.match(authStore, /await this\.loadMenus\(\)/)
  assert.match(authStore, /this\.initialized = true/)
  assert.match(loginPage, /await router\.replace\(redirect\)/)
  assert.match(loginPage, /route\.query\.redirect === 'string' \? route\.query\.redirect : '\/workbench'/)
})

test('login failure surfaces the backend error message and trace id', () => {
  assert.match(loginPage, /error instanceof ApiError/)
  assert.match(loginPage, /error\.message/)
  assert.match(loginPage, /error\.traceId/)
  assert.match(apiClient, /new ApiError\(payload\.code, payload\.message, response\.status, payload\.traceId/)
})

test('pre-request exceptions are logged, never silently swallowed', () => {
  assert.match(loginPage, /console\.error\('SRM login failed before a request reached the server/)
  assert.match(apiClient, /headers\.set\('X-Trace-Id', randomTraceId\(\)\)/)
  assert.doesNotMatch(apiClient, /crypto\.randomUUID/)
  assert.match(internalApi, /'X-Trace-Id': randomTraceId\(\)/)
  assert.doesNotMatch(internalApi, /crypto\.randomUUID/)
})
