import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('../src/views/system/user/index.vue', import.meta.url), 'utf8')

test('internal user page queries the real API with account and name filters', () => {
  assert.match(page, /label="用户账号"/)
  assert.match(page, /label="用户名称"/)
  assert.match(page, /\/system\/users\?/)
  assert.match(page, /qs\.set\('username', searchForm\.username\)/)
  assert.match(page, /if \(searchForm\.displayName\) qs\.set\('displayName', searchForm\.displayName\)/)
  assert.match(page, /onReset/)
  assert.match(page, /openCreate/)
  assert.doesNotMatch(page, /mock|静态假数据|模拟KPI/)
})

test('internal user page shows role tags, disabled role marks and Chinese status', () => {
  assert.match(page, /row\.roles/)
  assert.match(page, /roleTagLabel\(role\)/)
  assert.match(page, /已停用/)
  assert.match(page, /statusText\(row\.status\)/)
  assert.match(page, /row\.employeeCode \|\| '—'/)
})

test('internal user page gates tabs and buttons by split permissions', () => {
  for (const action of ['create', 'update', 'enable', 'disable', 'reset-password']) {
    assert.match(page, new RegExp(`system:user:${action}`))
  }
  assert.match(page, /system:user:assign-role/)
  assert.match(page, /system:user:view-permissions/)
  assert.match(page, /system:user:view-authorization/)
  assert.match(page, /canAssignRole\.value && !isView\.value/)
  assert.match(page, /ROLE_TAB_VISIBLE/)
  assert.match(page, /HISTORY_TAB_VISIBLE/)
})

test('internal user page assigns roles through the shared user role relationship', () => {
  assert.match(page, /\/system\/users\/' \+ form\.id \+ '\/roles/)
  assert.match(page, /roleIds:\[\.\.\.selectedRoleIds\.value\]/)
  assert.match(page, /\/system\/users\/assignable-roles/)
  assert.match(page, /option\.status !== 'ACTIVE' \|\| !option\.assignable/)
})

test('internal user page shows effective permissions, data scope and authorization records', () => {
  assert.match(page, /\/effective-permissions/)
  assert.match(page, /scopeLabel/)
  assert.match(page, /ineffectiveReason/)
  assert.match(page, /effectiveRoleCount/)
  assert.match(page, /accessibleMenuCount/)
  assert.match(page, /permissionCount/)
  assert.match(page, /organizationScopeLabel/)
  assert.match(page, /\/authorization-history/)
  assert.match(page, /授权记录/)
})

test('password reset shows temporary password once and supports copy', () => {
  assert.match(page, /重置后原会话将全部失效/)
  assert.match(page, /temporaryPassword/)
  assert.match(page, /resetPwdVisible/)
  assert.match(page, /navigator\.clipboard\.writeText/)
  assert.match(page, /仅显示一次/)
  assert.match(page, /将被强制修改密码/)
})
