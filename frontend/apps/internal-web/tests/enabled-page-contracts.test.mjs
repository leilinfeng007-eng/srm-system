import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const manifest = JSON.parse(readFileSync(new URL('../../../module-manifest.json', import.meta.url), 'utf8'))
const enabled = manifest.domains.flatMap((domain) => domain.features).filter((feature) => feature.enabled)

function sourceOf(componentKey) {
  return readFileSync(new URL(`../src/views/${componentKey}.vue`, import.meta.url), 'utf8')
}

test('every enabled Stage-1 page is implemented and avoids retired endpoint guesses', () => {
  const sources = enabled.map((feature) => sourceOf(feature.componentKey))
  assert.equal(sources.length, 18)
  for (const source of sources) {
    assert.doesNotMatch(source, /SkeletonFeaturePage/)
    assert.doesNotMatch(source, /\/master-data\/plants(?:[/?'"`]|$)/)
    assert.doesNotMatch(source, /\/system\/integration-jobs/)
    assert.doesNotMatch(source, /模拟KPI|静态假数据|mockData/)
  }
})

test('paged master-data pages use the controller size parameter', () => {
  for (const key of [
    'masterdata/purchasing-organization/index',
    'masterdata/delivery-location/index',
    'masterdata/category/index',
    'masterdata/material/index',
    'masterdata/external-mapping/index',
  ]) {
    const source = sourceOf(key)
    assert.match(source, /size: String\(pageSize\.value\)/, key)
    assert.doesNotMatch(source, /pageSize: String\(pageSize\.value\)/, key)
  }
})

test('organization, user and role pages load persisted details and gate exact actions', () => {
  const organization = sourceOf('masterdata/organization/index')
  assert.match(organization, /\/master-data\/organizations\/['" +]/)
  assert.match(organization, /valid-parents\?orgType=/)
  assert.match(organization, /masterdata:organization:enable/)
  assert.match(organization, /masterdata:organization:disable/)

  const user = sourceOf('system/user/index')
  assert.match(user, /flattenOrganizations/)
  assert.match(user, /\/system\/users\/['" +]/)
  for (const action of ['create', 'update', 'enable', 'disable', 'reset-password']) {
    assert.match(user, new RegExp(`system:user:${action}`))
  }

  const role = sourceOf('system/role/index')
  for (const action of ['create', 'update', 'enable', 'disable', 'assign-user', 'assign-permission', 'assign-data-scope']) {
    assert.match(role, new RegExp(`system:role:${action}`))
  }
})
