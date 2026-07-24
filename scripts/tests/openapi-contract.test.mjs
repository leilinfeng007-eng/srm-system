import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const { diffSpecs } = require('openapi-diff')
const internal = JSON.parse(readFileSync(
  new URL('../../docs/04-接口与数据设计/openapi/internal-api.json', import.meta.url),
  'utf8',
))
const supplier = JSON.parse(readFileSync(
  new URL('../../docs/04-接口与数据设计/openapi/supplier-api.json', import.meta.url),
  'utf8',
))

test('internal and supplier OpenAPI contracts remain explicitly separated', () => {
  assert.equal(internal.info.title, 'SRM 内部管理端 API')
  assert.equal(supplier.info.title, 'SRM 供应商端 API')
  assert.ok(Object.keys(internal.paths).every((path) => path.startsWith('/api/v1/')))
  assert.ok(Object.keys(supplier.paths).every((path) => path.startsWith('/supplier-api/v1/')))
  assert.ok('/api/v1/workbench/baseline' in internal.paths)
  assert.equal(Object.keys(internal.paths).length, 7)
  assert.equal(Object.keys(supplier.paths).length, 0)
  assert.equal(internal.servers[0].url, '/')
  assert.equal(supplier.servers[0].url, '/')
  assert.deepEqual(internal.paths['/api/v1/auth/login'].post.security, [])
  assert.deepEqual(internal.paths['/api/v1/auth/refresh'].post.security, [])
  assert.deepEqual(internal.security, [{ bearerAuth: [] }])
  assert.deepEqual(supplier.components.securitySchemes, {})
})

test('breaking change detector rejects a removed internal operation', async () => {
  const incompatible = structuredClone(internal)
  delete incompatible.paths['/api/v1/workbench/baseline']
  const result = await diffSpecs({
    sourceSpec: {
      content: JSON.stringify(internal),
      location: 'internal-baseline.json',
      format: 'openapi3',
    },
    destinationSpec: {
      content: JSON.stringify(incompatible),
      location: 'internal-incompatible.json',
      format: 'openapi3',
    },
  })
  assert.equal(result.breakingDifferencesFound, true)
})
