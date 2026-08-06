import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('data permission page loads role options and queries policies via API', () => {
  const src = readFileSync(new URL('../src/views/system/data-permission/index.vue', import.meta.url), 'utf8')
  assert.match(src, /\/system\/roles\?pageSize=999/)
  assert.match(src, /\/system\/roles\/' \+ role\.id/)
  assert.match(src, /dataPolicies/)
  assert.match(src, /DIMENSION_LABELS/)
    assert.match(src, /loadRoleOptions/)
    assert.match(src, /暂无数据权限策略/)
    assert.doesNotMatch(src, /mockData|静态/)
    assert.doesNotMatch(src, /el-button.*type="primary".*@click="savePolicies/)
})

test('data permission page and role data-scope tab share same API', () => {
  const dataPermSrc = readFileSync(new URL('../src/views/system/data-permission/index.vue', import.meta.url), 'utf8')
  const roleSrc = readFileSync(new URL('../src/views/system/role/index.vue', import.meta.url), 'utf8')
  assert.match(dataPermSrc, /\/system\/roles/)
  assert.match(roleSrc, /\/system\/roles/)
  assert.match(roleSrc, /data-policies/)
  assert.match(dataPermSrc, /dataPolicies/)
})

test('function permission page queries real API and supports filtering', () => {
  const src = readFileSync(new URL('../src/views/system/function-permission/index.vue', import.meta.url), 'utf8')
  assert.match(src, /\/system\/permissions/)
  assert.match(src, /\/system\/menus/)
  assert.match(src, /selectedDomain/)
  assert.match(src, /searchKeyword/)
  assert.match(src, /无匹配权限/)
  assert.match(src, /el-tree/)
  assert.doesNotMatch(src, /SkeletonFeaturePage/)
})

test('role page has five tabs with real API calls', () => {
  const src = readFileSync(new URL('../src/views/system/role/index.vue', import.meta.url), 'utf8')
    assert.match(src, /基本信息/)
    assert.match(src, /已挂用户/)
    assert.match(src, /功能权限/)
    assert.match(src, /数据权限/)
    assert.match(src, /授权记录/)
    assert.match(src, /usePermission/)
    assert.match(src, /\/system\/roles/)
    assert.match(src, /\/system\/menus/)
    assert.match(src, /\/system\/permissions/)
    assert.match(src, /\/system\/users/)
})

test('department page uses real API with manager search', () => {
  const src = readFileSync(new URL('../src/views/system/department/index.vue', import.meta.url), 'utf8')
    assert.match(src, /\/system\/departments/)
    assert.match(src, /\/system\/users\?pageSize=50/)
    assert.match(src, /displayName/)
    assert.match(src, /usePermission/)
    assert.match(src, /部门管理/)
})

test('position page uses dictionary dropdown and validates category', () => {
  const src = readFileSync(new URL('../src/views/system/position/index.vue', import.meta.url), 'utf8')
    assert.match(src, /\/system\/positions/)
    assert.match(src, /POSITION_CATEGORY/)
    assert.match(src, /categoryOptions/)
    assert.match(src, /itemCode/)
    assert.match(src, /itemName/)
    assert.match(src, /usePermission/)
    assert.doesNotMatch(src, /el-input.*v-model="form\.category"/)
})
