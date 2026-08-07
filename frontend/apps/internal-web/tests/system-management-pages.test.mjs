import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../src/views/system/${path}`, import.meta.url), 'utf8')

test('workflow page passes keyword filter, edits nodes fully and selects real assignees', () => {
  const src = read('workflow/index.vue')
  assert.match(src, /keyword/)
  assert.match(src, /\/system\/workflows\?/ )
  assert.match(src, /assigneeType/)
  assert.match(src, /assigneeValue/)
  assert.match(src, /\/system\/users/)
  assert.match(src, /\/system\/roles/)
  assert.match(src, /\/system\/positions/)
  assert.match(src, /durationHours/)
  assert.match(src, /nodeCode/)
  assert.match(src, /openDetail/)
  assert.match(src, /moveNode/)
  assert.match(src, /saveNode/)
  assert.match(src, /usePermission/)
  assert.doesNotMatch(src, /addNode\(\) \{\s*nodes\.value\.push\(\{ id:null, nodeCode:'N'/)
})

test('dictionary-parameter page filters dicts, edits parameters and previews numbers', () => {
  const src = read('dictionary-parameter/index.vue')
  assert.match(src, /\/system\/dictionaries\?/)
  assert.match(src, /keyword/)
  assert.match(src, /status/)
  assert.match(src, /\/system\/parameters\/'\+paramForm\.id, *\{method:'PUT'/)
  assert.match(src, /validationRule/)
  assert.match(src, /\/system\/number-rules\/preview/)
  assert.match(src, /effectiveFrom/)
  assert.match(src, /createdAt/)
  assert.match(src, /PENDING_APPROVAL|待审批/)
  assert.match(src, /approvalRequired/)
  assert.doesNotMatch(src, /mockData|静态/)
})

test('template-attachment page manages draft edit, attachments and batch progress', () => {
  const src = read('template-attachment/index.vue')
  assert.match(src, /'templates'\|'attachments'\|'jobs'/)
  assert.match(src, /\/system\/document-templates\/'\+editForm\.id, *\{method:'PUT'/)
  assert.match(src, /\/system\/attachments\/by-owner/)
  assert.match(src, /\/system\/attachments\/'\+row\.id\+'\/replace/)
  assert.match(src, /system:attachment:upload/)
  assert.match(src, /system:attachment:delete/)
  assert.match(src, /progressPercent/)
  assert.match(src, /system:batch-job:retry/)
  assert.doesNotMatch(src, /mockData|静态/)
})

test('message page shows source and business object columns', () => {
  const src = read('message/index.vue')
  assert.match(src, /来源/)
  assert.match(src, /业务对象/)
  assert.match(src, /sourceType/)
  assert.match(src, /sourceId/)
  assert.match(src, /\/messages\/unread-count/)
  assert.match(src, /\/messages\/'\+row\.id\+'\/read/)
  assert.match(src, /\/messages\/read-all/)
  assert.doesNotMatch(src, /mockData/)
})

test('audit-log page filters by operator, result, traceId, time range and opens detail', () => {
  const src = read('audit-log/index.vue')
  assert.match(src, /operatorName/)
  assert.match(src, /targetId/)
  assert.match(src, /resultCode/)
  assert.match(src, /traceId/)
  assert.match(src, /from/)
  assert.match(src, /to/)
  assert.match(src, /\/system\/audit-logs\/'\+row\.id/)
  assert.match(src, /fieldChanges/)
  assert.match(src, /beforeHash/)
  assert.match(src, /afterHash/)
  assert.doesNotMatch(src, /mockData/)
})

test('integration-job page filters by source, event type and time, with detail, attempts and batch view', () => {
  const src = read('integration-job/index.vue')
  assert.match(src, /'inbox'\|'outbox'\|'jobs'/)
  assert.match(src, /sourceSystem/)
  assert.match(src, /eventType/)
  assert.match(src, /objectType/)
  assert.match(src, /\/system\/\$\{tab\.value\}-events\/\$\{row\.id\}/)
  assert.match(src, /\/attempts/)
  assert.match(src, /nextRetryAt/)
  assert.match(src, /lastError/)
  assert.match(src, /attemptCount/)
  assert.match(src, /maxAttempts/)
  assert.match(src, /system:integration-job:retry/)
  assert.match(src, /\/system\/batch-jobs/)
  assert.match(src, /retryable/)
  assert.doesNotMatch(src, /mockData|静态/)
})

test('all six pages use real APIs and no static data', () => {
  for (const page of ['workflow', 'dictionary-parameter', 'template-attachment', 'message', 'audit-log', 'integration-job']) {
    const src = read(`${page}/index.vue`)
    assert.match(src, /internalApi\.(get|post|request)/, page)
    assert.doesNotMatch(src, /mockData|静态假数据|SkeletonFeaturePage/, page)
  }
})
