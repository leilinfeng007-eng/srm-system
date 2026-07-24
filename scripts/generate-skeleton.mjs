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
expected.set(
  'backend/src/main/resources/db/migration/V3__seed_menu_permissions.sql',
  `${sql.join('\n')}\n`,
)

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

for (const { domain, feature } of placeholderRows) {
  const page = `<script setup lang="ts">\nimport { SkeletonFeaturePage } from '@srm/shared-ui'\n\nconst metadata = ${JSON.stringify({
    domainLabel: domain.label,
    featureLabel: feature.label,
    route: feature.route,
    permission: feature.permission,
    menuCode: feature.menuCode,
    phase: feature.phase,
  }, null, 2)} as const\n</script>\n\n<template>\n  <SkeletonFeaturePage v-bind="metadata" />\n</template>\n`
  expected.set(`frontend/apps/internal-web/src/views/${feature.componentKey}.vue`, page)
}

// The workbench home is the hand-maintained reference implementation for the full-stack
// layering template. validate-skeleton.mjs still verifies its manifest path and route registration,
// while this generator deliberately leaves its implementation intact.

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

const docLines = [
  '# 阶段0模块清单说明',
  '',
  '机器可读唯一来源为 `frontend/module-manifest.json`，数据库种子、内部端路由注册表和页面目录由 `scripts/generate-skeleton.mjs` 生成。',
  '',
  `- 一级功能域：${manifest.domains.length}`,
  `- V1.0 二级占位页：${placeholderRows.length}`,
  '- 工作台首页：1（独立首页，不计入二级占位页）',
  `- 阶段0可访问页面合计：${enabledRows.length}`,
  `- 隐藏预留：${featureRows.length - enabledRows.length}（阶段7“预测与交付计划”，不生成页面或路由）`,
  '',
  '执行 `node scripts/generate-skeleton.mjs` 只校验生成物；架构维护者在修改清单后显式执行 `node scripts/generate-skeleton.mjs --write`。Flyway迁移进入共享分支后不得重新生成覆盖，只能新增迁移。',
  '',
  '| 顺序 | domainCode | 一级菜单 | 二级页数 | 后端包 |',
  '|---:|---|---|---:|---|',
]
for (const domain of manifest.domains) {
  const count = domain.domainCode === 'workbench' ? 0 : domain.features.filter((item) => item.enabled).length
  docLines.push(`| ${domain.sortOrder / 10} | ${domain.domainCode} | ${domain.label} | ${count} | ${domain.backendPackage} |`)
}
docLines.push('')
expected.set('docs/02-业务需求与功能规划/阶段0模块清单说明.md', `${docLines.join('\n')}\n`)

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
console.log(`12 domains=${manifest.domains.length}, placeholders=${placeholderRows.length}, enabled routes=${enabledRows.length}, hidden=${featureRows.length - enabledRows.length}`)
console.log(`Supplier portal entries=${supplierManifest.entries.filter((item) => item.enabled).length}`)
