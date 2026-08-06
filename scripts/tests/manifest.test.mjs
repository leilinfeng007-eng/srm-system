import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const manifest = JSON.parse(readFileSync(new URL('../../frontend/module-manifest.json', import.meta.url), 'utf8'))

test('stage 1 manifest has twenty-one enabled and sixty-nine hidden pages', () => {
  const enabled = manifest.domains.flatMap((domain) => domain.features)
    .filter((feature) => feature.enabled)
  const hidden = manifest.domains.flatMap((domain) => domain.features)
    .filter((feature) => !feature.enabled)
  assert.equal(manifest.domains.length, 12)
  assert.equal(enabled.filter((feature) => feature.domainCode !== 'workbench').length, 20)
  assert.equal(enabled.length, 21)
  assert.equal(hidden.length, 69)
})

test('menu, route, component and permission identifiers are unique', () => {
  const features = manifest.domains.flatMap((domain) => domain.features)
  for (const key of ['menuCode', 'route', 'componentKey', 'permission']) {
    const values = features.map((item) => item[key])
    assert.equal(new Set(values).size, values.length, `${key} should be unique`)
  }
})
