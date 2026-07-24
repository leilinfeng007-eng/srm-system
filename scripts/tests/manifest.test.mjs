import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const manifest = JSON.parse(readFileSync(new URL('../../frontend/module-manifest.json', import.meta.url), 'utf8'))

test('stage 0 manifest has the taskbook cardinalities', () => {
  const enabled = manifest.domains.flatMap((domain) => domain.features)
    .filter((feature) => feature.enabled)
  assert.equal(manifest.domains.length, 12)
  assert.equal(enabled.filter((feature) => feature.domainCode !== 'workbench').length, 85)
  assert.equal(enabled.length, 86)
})

test('menu, route, component and permission identifiers are unique', () => {
  const features = manifest.domains.flatMap((domain) => domain.features)
  for (const key of ['menuCode', 'route', 'componentKey', 'permission']) {
    const values = features.map((item) => item[key])
    assert.equal(new Set(values).size, values.length, `${key} should be unique`)
  }
})
