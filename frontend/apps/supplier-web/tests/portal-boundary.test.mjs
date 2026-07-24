import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('supplier portal has nine routes and no internal authentication endpoints', () => {
  const registry = readFileSync(new URL('../src/router/route-registry.ts', import.meta.url), 'utf8')
  const api = readFileSync(new URL('../src/api/supplier-api.ts', import.meta.url), 'utf8')
  assert.equal((registry.match(/requiresPortalEntry/g) ?? []).length, 9)
  assert.doesNotMatch(`${registry}\n${api}`, /\/api\/v1\/(auth|navigation)/)
  assert.doesNotMatch(`${registry}\n${api}`, /MENU_WORKBENCH/)
})
