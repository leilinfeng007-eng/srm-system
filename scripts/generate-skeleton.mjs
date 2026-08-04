import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const manifestPath = resolve(root, 'frontend/module-manifest.json')
const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
const supplierManifest = JSON.parse(readFileSync(resolve(root, 'frontend/supplier-portal-manifest.json'), 'utf8'))
const writeMode = process.argv.includes('--write')

const sqlString = (value) => `'${String(value).replaceAll("'", "''")}'`
const bool = (value) => value ? 'TRUE' : 'FALSE'
const featureRows = manifest.domains.flatMap((domain) =>
  domain.features.map((feature) => ({ domain, feature })),
)
const enabledRows = featureRows.filter(({ feature }) => feature.enabled)
const placeholderRows = enabledRows.filter(({ domain }) => domain.domainCode !== 'workbench')

const expected = new Map()

const sql = [
  '-- Generated from frontend/module-manifest.json by scripts/generate-skeleton.mjs.',
  '-- Do not hand-edit this migration; update the manifest before the migration is released.',
  '',
]
for (const domain of manifest.domains) {
  sql.push(
    'INSERT INTO sys_menu (',
    '  menu_code, label, menu_type, route, component_key, permission_code,',
    '  sort_order, phase, visible, enabled, created_by, updated_by',
    ') VALUES (',
    `  ${sqlString(domain.menuCode)}, ${sqlString(domain.label)}, 'DIRECTORY', NULL, NULL, NULL,`,
    `  ${domain.sortOrder}, ${domain.phase}, TRUE, ${bool(domain.enabled)}, 'flyway', 'flyway'`,
    ');',
    '',
  )
}
for (const { domain, feature } of featureRows) {
  sql.push(
    'INSERT INTO sys_menu (',
    '  parent_id, menu_code, label, menu_type, route, component_key, permission_code,',
    '  sort_order, phase, visible, enabled, created_by, updated_by',
    ') SELECT',
    `  id, ${sqlString(feature.menuCode)}, ${sqlString(feature.label)}, 'PAGE',`,
    `  ${sqlString(feature.route)}, ${sqlString(feature.componentKey)}, ${sqlString(feature.permission)},`,
    `  ${feature.sortOrder}, ${feature.phase}, ${bool(feature.enabled)}, ${bool(feature.enabled)}, 'flyway', 'flyway'`,
    `FROM sys_menu WHERE menu_code = ${sqlString(domain.menuCode)};`,
    '',
    'INSERT INTO sys_permission (',
    '  permission_code, domain_code, resource_code, action_code, description,',
    '  enabled, created_by, updated_by',
    ') VALUES (',
    `  ${sqlString(feature.permission)}, ${sqlString(feature.domainCode)},`,
    `  ${sqlString(feature.permission.split(':')[1])}, 'view',`,
    `  ${sqlString(`查看${feature.label}`)}, ${bool(feature.enabled)}, 'flyway', 'flyway'`,
    ');',
    '',
  )
}
sql.push(
  '-- SUPER_ADMIN receives every enabled menu and permission.',
  'INSERT INTO sys_role_menu (role_id, menu_id, created_by)',
  "SELECT r.id, m.id, 'flyway'",
  'FROM sys_role r CROSS JOIN sys_menu m',
  "WHERE r.role_code = 'SUPER_ADMIN' AND m.enabled = TRUE AND m.visible = TRUE;",
  '',
  'INSERT INTO sys_role_permission (role_id, permission_id, created_by)',
  "SELECT r.id, p.id, 'flyway'",
  'FROM sys_role r CROSS JOIN sys_permission p',
  "WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE;",
  '',
  '-- SKELETON_VIEWER proves database menu filtering and backend 403 enforcement.',
  'INSERT INTO sys_role_menu (role_id, menu_id, created_by)',
  "SELECT r.id, m.id, 'flyway'",
  'FROM sys_role r CROSS JOIN sys_menu m',
  "WHERE r.role_code = 'SKELETON_VIEWER'",
  "  AND m.menu_code IN ('MENU_WORKBENCH', 'MENU_WORKBENCH_HOME', 'MENU_SUPPLIER', 'MENU_SUPPLIER_POOL');",
  '',
  'INSERT INTO sys_role_permission (role_id, permission_id, created_by)',
  "SELECT r.id, p.id, 'flyway'",
  'FROM sys_role r CROSS JOIN sys_permission p',
  "WHERE r.role_code = 'SKELETON_VIEWER'",
  "  AND p.permission_code IN ('workbench:home:view', 'supplier:pool:view');",
  '',
)
// V3 is a released Stage-0 migration and is intentionally never regenerated.
// Stage-1 menu visibility is advanced by later Flyway migrations.

const registry = [
  '// Generated from frontend/module-manifest.json. Do not hand-edit.',
  "import type { RouteComponent, RouteRecordRaw } from 'vue-router'",
  '',
  'export const componentRegistry: Readonly<Record<string, RouteComponent>> = {',
]
for (const { feature } of enabledRows) {
  registry.push(`  ${JSON.stringify(feature.componentKey)}: () => import('../views/${feature.componentKey}.vue'),`)
}
registry.push('}', '', 'export const featureRoutes: RouteRecordRaw[] = [')
for (const { domain, feature } of enabledRows) {
  registry.push(
    '  {',
    `    path: ${JSON.stringify(feature.route)},`,
    `    name: ${JSON.stringify(feature.menuCode)},`,
    `    component: componentRegistry[${JSON.stringify(feature.componentKey)}],`,
    '    meta: {',
    `      title: ${JSON.stringify(feature.label)},`,
    `      domainLabel: ${JSON.stringify(domain.label)},`,
    `      domainCode: ${JSON.stringify(domain.domainCode)},`,
    `      menuCode: ${JSON.stringify(feature.menuCode)},`,
    `      permission: ${JSON.stringify(feature.permission)},`,
    '      requiresAuth: true,',
    '    },',
    '  },',
  )
}
registry.push(']', '')
expected.set(
  'frontend/apps/internal-web/src/router/route-registry.ts',
  `${registry.join('\n')}\n`,
)

// Stage-1 pages are hand-maintained implementations. The generator owns only the
// allow-listed route registry and must never replace a real page with a skeleton.

const supplierRegistry = [
  '// Generated from frontend/supplier-portal-manifest.json. Do not hand-edit.',
  "import type { RouteRecordRaw } from 'vue-router'",
  '',
  'export const supplierPortalRoutes: RouteRecordRaw[] = [',
]
for (const entry of supplierManifest.entries.filter((item) => item.enabled)) {
  supplierRegistry.push(
    '  {',
    `    path: ${JSON.stringify(entry.route)},`,
    `    name: ${JSON.stringify(entry.menuCode)},`,
    `    component: () => import('../views/${entry.componentKey}.vue'),`,
    `    meta: { title: ${JSON.stringify(entry.label)}, permission: ${JSON.stringify(entry.permission)}, requiresPortalEntry: true },`,
    '  },',
  )
  const page = `<script setup lang="ts">\nimport { SkeletonFeaturePage } from '@srm/shared-ui'\n\nconst metadata = ${JSON.stringify({
    domainLabel: '供应商门户',
    featureLabel: entry.label,
    route: entry.route,
    permission: entry.permission,
    menuCode: entry.menuCode,
    phase: entry.phase,
  }, null, 2)} as const\n</script>\n\n<template>\n  <SkeletonFeaturePage v-bind="metadata" />\n</template>\n`
  expected.set(`frontend/apps/supplier-web/src/views/${entry.componentKey}.vue`, page)
}
supplierRegistry.push(']', '')
expected.set('frontend/apps/supplier-web/src/router/route-registry.ts', `${supplierRegistry.join('\n')}\n`)

const differences = []
for (const [relativePath, content] of expected) {
  const target = resolve(root, relativePath)
  let actual = null
  try {
    actual = readFileSync(target, 'utf8')
  } catch {}
  if (actual !== content) {
    differences.push(relativePath)
    if (writeMode) {
      mkdirSync(dirname(target), { recursive: true })
      writeFileSync(target, content)
    }
  }
}

if (differences.length && !writeMode) {
  console.error('Skeleton generated files are missing or stale:')
  for (const item of differences) console.error(`- ${item}`)
  process.exit(1)
}

console.log(`${writeMode ? 'Generated' : 'Verified'} ${expected.size} files from module manifest.`)
console.log(`12 domains=${manifest.domains.length}, stage1 pages=${enabledRows.length}, hidden=${featureRows.length - enabledRows.length}`)
console.log(`Supplier portal entries=${supplierManifest.entries.filter((item) => item.enabled).length}`)
