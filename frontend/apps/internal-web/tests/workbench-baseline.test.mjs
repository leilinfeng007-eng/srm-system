import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('workbench uses the typed backend reference endpoint without business mock data', () => {
  const api = readFileSync(new URL('../src/api/internal-api.ts', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/views/workbench/home/index.vue', import.meta.url), 'utf8')

  assert.match(api, /internalApi\.get<WorkbenchBaseline>\('\/workbench\/baseline'\)/)
  assert.match(page, /getWorkbenchBaseline/)
  assert.match(page, /不读取或保存业务数据/)
  assert.doesNotMatch(page, /mock|模拟KPI|交易数据/)
})
