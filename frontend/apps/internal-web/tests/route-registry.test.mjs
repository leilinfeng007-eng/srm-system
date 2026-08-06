import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('route registry allow-lists exactly the twenty-one Stage-1 pages', () => {
  const source = readFileSync(new URL('../src/router/route-registry.ts', import.meta.url), 'utf8')
  assert.match(source, /MENU_WORKBENCH_HOME/)
  assert.doesNotMatch(source, /MENU_PROCUREMENT_FORECAST_PLAN/)
  assert.doesNotMatch(source, /MENU_SUPPLIER_POOL/)
  assert.equal((source.match(/component: componentRegistry/g) ?? []).length, 21)
})
