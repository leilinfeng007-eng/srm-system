import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const manifest = JSON.parse(readFileSync(resolve(root, 'frontend/module-manifest.json'), 'utf8'))
const supplier = JSON.parse(readFileSync(resolve(root, 'frontend/supplier-portal-manifest.json'), 'utf8'))
const sql = readFileSync(resolve(root, 'backend/src/main/resources/db/migration/V3__seed_menu_permissions.sql'), 'utf8')
const registry = readFileSync(resolve(root, 'frontend/apps/internal-web/src/router/route-registry.ts'), 'utf8')
const supplierRegistry = readFileSync(resolve(root, 'frontend/apps/supplier-web/src/router/route-registry.ts'), 'utf8')
const internalApi = readFileSync(resolve(root, 'frontend/apps/internal-web/src/api/internal-api.ts'), 'utf8')
const errors = []

const requireValue = (condition, message) => { if (!condition) errors.push(message) }
const ensureUnique = (items, selector, label) => {
  const seen = new Map()
  for (const item of items) {
    const value = selector(item)
    if (seen.has(value)) errors.push(`${label}重复: ${value}`)
    seen.set(value, item)
  }
}

const expectedDomains = [
  'workbench', 'supplier', 'sourcing', 'contract', 'source', 'procurement',
  'delivery', 'quality', 'settlement', 'performance', 'masterdata', 'system',
]
const expectedPhases = [1, 2, 4, 5, 3, 6, 7, 8, 9, 10, 1, 1]
requireValue(manifest.domains.length === 12, `一级功能域应为12，实际${manifest.domains.length}`)
requireValue(
  JSON.stringify(manifest.domains.map((item) => item.domainCode)) === JSON.stringify(expectedDomains),
  '一级功能域编码或顺序与任务书不一致',
)
requireValue(
  JSON.stringify(manifest.domains.map((item) => item.phase)) === JSON.stringify(expectedPhases),
  '一级功能域计划阶段与实施总表不一致',
)

const all = manifest.domains.flatMap((domain) => domain.features.map((feature) => ({ domain, feature })))
const enabled = all.filter(({ feature }) => feature.enabled)
const placeholders = enabled.filter(({ domain }) => domain.domainCode !== 'workbench')
const hidden = all.filter(({ feature }) => !feature.enabled)
requireValue(placeholders.length === 85, `V1.0二级占位页应为85，实际${placeholders.length}`)
requireValue(enabled.length === 86, `含工作台的可访问页面应为86，实际${enabled.length}`)
requireValue(hidden.length === 1, `隐藏预留应为1，实际${hidden.length}`)
requireValue(hidden[0]?.feature.menuCode === 'MENU_PROCUREMENT_FORECAST_PLAN', '隐藏预留必须是预测与交付计划')
requireValue(hidden[0]?.feature.phase === 7, '预测与交付计划必须预留在阶段7')
ensureUnique(manifest.domains, (item) => item.menuCode, '一级menuCode')
ensureUnique(all, ({ feature }) => feature.menuCode, '二级menuCode')
ensureUnique(all, ({ feature }) => feature.route, 'route')
ensureUnique(all, ({ feature }) => feature.componentKey, 'componentKey')
ensureUnique(all, ({ feature }) => feature.permission, 'permission')

for (const domain of manifest.domains) {
  requireValue(existsSync(resolve(root, `backend/src/main/java/${domain.backendPackage.replaceAll('.', '/')}/package-info.java`)), `后端领域包缺失: ${domain.backendPackage}`)
  const orders = domain.features.map((item) => item.sortOrder)
  ensureUnique(orders, (item) => item, `${domain.domainCode} sortOrder`)
}

for (const referenceFile of [
  'backend/src/main/java/com/srm/workbench/api/controller/WorkbenchBaselineController.java',
  'backend/src/main/java/com/srm/workbench/api/response/WorkbenchBaselineResponse.java',
  'backend/src/main/java/com/srm/workbench/application/query/WorkbenchBaselineQueryService.java',
  'backend/src/main/java/com/srm/workbench/application/service/WorkbenchBaselineService.java',
  'backend/src/main/java/com/srm/workbench/domain/model/WorkbenchBaseline.java',
  'backend/src/main/java/com/srm/workbench/domain/repository/WorkbenchBaselineRepository.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/MybatisWorkbenchBaselineRepository.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/mapper/WorkbenchBaselineMapper.java',
  'backend/src/test/java/com/srm/workbench/WorkbenchBaselineIntegrationTest.java',
  'frontend/apps/internal-web/src/views/workbench/home/index.vue',
]) {
  requireValue(existsSync(resolve(root, referenceFile)), `工作台标准分层链路缺失: ${referenceFile}`)
}
requireValue(
  internalApi.includes("internalApi.get<WorkbenchBaseline>('/workbench/baseline')"),
  '内部端未通过类型化API客户端连接工作台标准分层接口',
)

for (const { feature } of all) {
  for (const key of ['domainCode', 'menuCode', 'label', 'route', 'componentKey', 'permission', 'sortOrder', 'phase', 'enabled']) {
    requireValue(feature[key] !== undefined && feature[key] !== '', `${feature.menuCode ?? '未知页面'} 缺少 ${key}`)
  }
  requireValue(sql.includes(feature.menuCode), `菜单SQL缺少 ${feature.menuCode}`)
  requireValue(sql.includes(feature.permission), `权限SQL缺少 ${feature.permission}`)
  const view = resolve(root, `frontend/apps/internal-web/src/views/${feature.componentKey}.vue`)
  if (feature.enabled) {
    requireValue(existsSync(view), `页面目录缺失: ${feature.componentKey}`)
    requireValue(registry.includes(JSON.stringify(feature.componentKey)), `路由注册表缺少 ${feature.componentKey}`)
    requireValue(registry.includes(JSON.stringify(feature.route)), `路由注册表缺少 ${feature.route}`)
  } else {
    requireValue(!existsSync(view), `隐藏预留不得生成页面: ${feature.componentKey}`)
    requireValue(!registry.includes(feature.menuCode), `隐藏预留不得注册路由: ${feature.menuCode}`)
  }
}

requireValue(supplier.entries.length === 9, `供应商门户入口应为9，实际${supplier.entries.length}`)
ensureUnique(supplier.entries, (item) => item.route, '供应商route')
ensureUnique(supplier.entries, (item) => item.permission, '供应商permission')
for (const entry of supplier.entries) {
  requireValue(existsSync(resolve(root, `frontend/apps/supplier-web/src/views/${entry.componentKey}.vue`)), `供应商页面缺失: ${entry.componentKey}`)
  requireValue(supplierRegistry.includes(JSON.stringify(entry.route)), `供应商路由缺失: ${entry.route}`)
}

if (errors.length) {
  console.error(`骨架一致性校验失败（${errors.length}项）`)
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log('骨架一致性校验通过')
console.log(`一级功能域: ${manifest.domains.length}`)
console.log(`V1.0二级占位页: ${placeholders.length}`)
console.log(`工作台首页: 1`)
console.log(`阶段7隐藏预留: ${hidden.length}`)
console.log(`供应商门户入口: ${supplier.entries.length}`)
