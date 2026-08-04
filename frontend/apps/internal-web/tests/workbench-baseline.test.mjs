import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('workbench uses paged APIs, persisted task relation and binds withdraw to the selected row', () => {
  const page = readFileSync(new URL('../src/views/workbench/home/index.vue', import.meta.url), 'utf8')

  assert.match(page, /get<\{items:ApprovalItem\[\],total:number\}>\('\/system\/approvals/)
  assert.match(page, /task\.approvalInstanceId/)
  assert.match(page, /get<ApprovalItem>\('\/system\/approvals\/'\+task\.approvalInstanceId\)/)
  assert.match(page, /doWithdraw\(row\)/)
  assert.doesNotMatch(page, /businessSummary===task\.title/)
  assert.doesNotMatch(page, /mock|模拟KPI|静态假数据/)
})
