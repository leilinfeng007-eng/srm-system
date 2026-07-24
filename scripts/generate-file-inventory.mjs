import { readdirSync, statSync, writeFileSync } from 'node:fs'
import { dirname, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const output = 'docs/05-测试验收与运维/阶段0文件清单.md'
const ignoredDirectories = new Set(['.git', 'node_modules', 'target', 'dist'])
const protectedFiles = new Set([
  'docs/01-业务蓝图与总体设计/SRM供应商协同管理系统蓝图与总体设计说明书_V1.0.docx',
  'docs/02-业务需求与功能规划/SRM_V1.0_阶段0_系统工程架构与功能骨架搭建任务书.docx',
  'docs/02-业务需求与功能规划/SRM_V1.0分阶段实施与开发任务总表.xlsx',
  'docs/02-业务需求与功能规划/SRM_V1.0分阶段实施与开发任务总表_执行版.xlsx',
])
const remediationAdded = [
  'backend/src/main/java/com/srm/config/OpenApiConfiguration.java',
  'backend/src/main/java/com/srm/config/SensitiveConfigurationInitializer.java',
  'backend/src/main/java/com/srm/config/SensitiveValuePolicy.java',
  'backend/src/main/java/com/srm/workbench/api/controller/WorkbenchBaselineController.java',
  'backend/src/main/java/com/srm/workbench/api/response/WorkbenchBaselineResponse.java',
  'backend/src/main/java/com/srm/workbench/application/query/WorkbenchBaselineQueryService.java',
  'backend/src/main/java/com/srm/workbench/application/service/WorkbenchBaselineService.java',
  'backend/src/main/java/com/srm/workbench/domain/model/BaselineState.java',
  'backend/src/main/java/com/srm/workbench/domain/model/WorkbenchBaseline.java',
  'backend/src/main/java/com/srm/workbench/domain/repository/WorkbenchBaselineRepository.java',
  'backend/src/main/resources/META-INF/spring.factories',
  'backend/src/test/java/com/srm/config/OpenApiDisabledIntegrationTest.java',
  'backend/src/test/java/com/srm/config/OpenApiEnabledIntegrationTest.java',
  'backend/src/test/java/com/srm/config/SensitiveValuePolicyTest.java',
  'backend/src/test/java/com/srm/workbench/WorkbenchBaselineIntegrationTest.java',
  'backend/src/test/java/com/srm/workbench/WorkbenchBaselineServiceTest.java',
  'deploy/README.md',
  'deploy/docker-compose.prod.yml',
  'deploy/docker/mysql-secure-entrypoint.sh',
  'deploy/prod.env.example',
  'frontend/apps/internal-web/tests/workbench-baseline.test.mjs',
]
const remediationModified = [
  '.env.example',
  'README.md',
  'backend/pom.xml',
  'backend/src/main/java/com/srm/security/SecurityConfig.java',
  'backend/src/main/java/com/srm/security/bootstrap/BootstrapAccountInitializer.java',
  'backend/src/main/java/com/srm/security/token/JwtTokenService.java',
  'backend/src/main/java/com/srm/workbench/package-info.java',
  'backend/src/main/resources/application-dev.yml',
  'backend/src/main/resources/application-prod.yml',
  'backend/src/main/resources/application.yml',
  'backend/src/test/java/com/srm/architecture/ModuleBoundaryTest.java',
  'backend/src/test/resources/application-test.yml',
  'docker-compose.yml',
  'docs/03-模块详细设计/模块开发模板.md',
  'docs/03-模块详细设计/阶段0工程开发指南.md',
  'docs/04-接口与数据设计/API规范.md',
  'docs/04-接口与数据设计/数据库规范.md',
  'docs/05-测试验收与运维/本地运行手册.md',
  'docs/05-测试验收与运维/构建与恢复说明.md',
  'docs/05-测试验收与运维/阶段0开发报告.md',
  'docs/05-测试验收与运维/阶段0文件清单.md',
  'docs/05-测试验收与运维/阶段0验收记录.md',
  'frontend/apps/internal-web/src/api/internal-api.ts',
  'frontend/apps/internal-web/src/views/workbench/home/index.vue',
  'frontend/packages/shared-types/src/index.ts',
  'scripts/check-migrations.sh',
  'scripts/check.sh',
  'scripts/generate-file-inventory.mjs',
  'scripts/generate-skeleton.mjs',
  'scripts/live-smoke.mjs',
  'scripts/validate-deploy.mjs',
  'scripts/validate-security.mjs',
  'scripts/validate-skeleton.mjs',
]
const flywayP1Added = [
  'docs/05-测试验收与运维/阶段0FlywayP1整改复验报告.md',
]
const flywayP1Modified = [
  'backend/pom.xml',
  'docs/04-接口与数据设计/数据库规范.md',
  'docs/05-测试验收与运维/构建与恢复说明.md',
  'docs/05-测试验收与运维/阶段0Docker专项验收报告.md',
  'docs/05-测试验收与运维/阶段0开发报告.md',
  'docs/05-测试验收与运维/阶段0文件清单.md',
  'docs/05-测试验收与运维/阶段0验收记录.md',
  'scripts/check-migrations.sh',
  'scripts/generate-file-inventory.mjs',
]
const architectureAdded = [
  'backend/src/main/java/com/srm/config/CacheConfiguration.java',
  'backend/src/main/java/com/srm/config/CacheNames.java',
  'backend/src/main/java/com/srm/platform/navigation/NavigationCacheInvalidator.java',
  'backend/src/main/java/com/srm/platform/navigation/NavigationQueryService.java',
  'backend/src/main/java/com/srm/platform/navigation/infrastructure/persistence/MybatisNavigationRepository.java',
  'backend/src/main/java/com/srm/platform/navigation/infrastructure/persistence/dto/NavigationMenuRow.java',
  'backend/src/main/java/com/srm/platform/navigation/infrastructure/persistence/mapper/NavigationMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/MybatisSecurityAuditRepository.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/MybatisTokenSessionRepository.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/MybatisUserAccountRepository.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysLoginLogEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysOperationLogEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysRefreshTokenEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysUserEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysUserRoleEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/LoginLogMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/OperationLogMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/TokenSessionMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/UserAccountMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/UserRoleMapper.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/MybatisWorkbenchBaselineRepository.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/mapper/WorkbenchBaselineMapper.java',
  'backend/src/main/resources/mybatis/navigation/NavigationMapper.xml',
  'backend/src/main/resources/mybatis/security/TokenSessionMapper.xml',
  'backend/src/main/resources/mybatis/security/UserAccountMapper.xml',
  'backend/src/main/resources/mybatis/security/UserRoleMapper.xml',
  'backend/src/test/java/com/srm/config/OpenApiContractExportTest.java',
  'backend/src/test/java/com/srm/platform/navigation/NavigationCacheIntegrationTest.java',
  'backend/src/test/java/com/srm/security/SystemBaselineTestMapper.java',
  'docs/04-接口与数据设计/openapi/baseline/internal-api.json',
  'docs/04-接口与数据设计/openapi/baseline/supplier-api.json',
  'docs/04-接口与数据设计/openapi/internal-api.json',
  'docs/04-接口与数据设计/openapi/redocly.yaml',
  'docs/04-接口与数据设计/openapi/supplier-api.json',
  'docs/05-测试验收与运维/阶段0架构优化复验报告.md',
  'frontend/packages/api-client/src/generated/internal-api-client.ts',
  'frontend/packages/api-client/src/generated/supplier-api-client.ts',
  'frontend/packages/shared-types/src/generated/internal-api.ts',
  'frontend/packages/shared-types/src/generated/supplier-api.ts',
  'scripts/check-openapi.mjs',
  'scripts/tests/openapi-contract.test.mjs',
  'scripts/validate-architecture.mjs',
]
const architectureModified = [
  'AGENTS.md',
  'README.md',
  'backend/pom.xml',
  'backend/src/main/java/com/srm/config/OpenApiConfiguration.java',
  'backend/src/main/java/com/srm/platform/meta/MetaController.java',
  'backend/src/main/java/com/srm/platform/navigation/NavigationController.java',
  'backend/src/main/java/com/srm/platform/navigation/NavigationRepository.java',
  'backend/src/main/java/com/srm/security/api/AuthController.java',
  'backend/src/main/java/com/srm/security/auth/SecurityAuditRepository.java',
  'backend/src/main/java/com/srm/security/auth/UserAccountRepository.java',
  'backend/src/main/java/com/srm/security/token/TokenSessionRepository.java',
  'backend/src/main/java/com/srm/workbench/api/controller/WorkbenchBaselineController.java',
  'backend/src/main/resources/application.yml',
  'backend/src/test/java/com/srm/architecture/ModuleBoundaryTest.java',
  'backend/src/test/java/com/srm/config/OpenApiDisabledIntegrationTest.java',
  'backend/src/test/java/com/srm/config/OpenApiEnabledIntegrationTest.java',
  'backend/src/test/java/com/srm/security/AuthFlowIntegrationTest.java',
  'docs/03-模块详细设计/模块开发模板.md',
  'docs/03-模块详细设计/阶段0工程开发指南.md',
  'docs/04-接口与数据设计/API规范.md',
  'docs/04-接口与数据设计/数据库规范.md',
  'docs/05-测试验收与运维/本地运行手册.md',
  'docs/05-测试验收与运维/构建与恢复说明.md',
  'docs/05-测试验收与运维/阶段0Docker专项验收报告.md',
  'docs/05-测试验收与运维/阶段0FlywayP1整改复验报告.md',
  'docs/05-测试验收与运维/阶段0开发报告.md',
  'docs/05-测试验收与运维/阶段0文件清单.md',
  'docs/05-测试验收与运维/阶段0验收记录.md',
  'frontend/packages/api-client/package.json',
  'frontend/packages/api-client/src/index.ts',
  'frontend/packages/shared-types/src/index.ts',
  'package-lock.json',
  'package.json',
  'scripts/build-all.sh',
  'scripts/check.sh',
  'scripts/generate-file-inventory.mjs',
  'scripts/validate-skeleton.mjs',
]
const architectureRemoved = [
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/JdbcWorkbenchBaselineRepository.java',
]

const files = []
const walk = (directory) => {
  for (const name of readdirSync(directory).sort((left, right) => left.localeCompare(right, 'zh-CN'))) {
    if (name === '.DS_Store') continue
    const absolute = resolve(directory, name)
    if (statSync(absolute).isDirectory()) {
      if (!ignoredDirectories.has(name)) walk(absolute)
      continue
    }
    files.push(relative(root, absolute))
  }
}
walk(root)

const delivered = files.filter((file) => !protectedFiles.has(file))
for (const file of [
  ...remediationAdded,
  ...remediationModified,
  ...flywayP1Added,
  ...flywayP1Modified,
  ...architectureAdded,
  ...architectureModified,
]) {
  if (!files.includes(file)) throw new Error(`整改文件清单引用了不存在的文件: ${file}`)
}
const groups = new Map()
for (const file of delivered) {
  const group = file.includes('/') ? file.split('/')[0] : '根目录'
  const entries = groups.get(group) ?? []
  entries.push(file)
  groups.set(group, entries)
}

const lines = [
  '# 阶段0文件清单',
  '',
  '本清单由 `node scripts/generate-file-inventory.mjs` 从实际工作区生成；排除 `.git`、`node_modules`、`target`、`dist` 和 `.DS_Store`。执行前已有的四个用户文档单独列为受保护文件，不计入新增交付。',
  '',
  `新增交付文件总数：${delivered.length}。既有用户文件修改数：0。`,
  '',
  '## 本轮整改文件',
  '',
  `本轮整改新增 ${remediationAdded.length} 个文件，修改 ${remediationModified.length} 个文件；以下列表按整改前工作区为比较基线。`,
  '',
  '### 本轮新增',
  '',
  ...remediationAdded.map((file) => `- \`${file}\``),
  '',
  '### 本轮修改',
  '',
  ...remediationModified.map((file) => `- \`${file}\``),
  '',
  '## Flyway P1 整改文件',
  '',
  `本次 Flyway P1 整改新增 ${flywayP1Added.length} 个文件，修改 ${flywayP1Modified.length} 个文件。`,
  '',
  '### Flyway P1 新增',
  '',
  ...flywayP1Added.map((file) => `- \`${file}\``),
  '',
  '### Flyway P1 修改',
  '',
  ...flywayP1Modified.map((file) => `- \`${file}\``),
  '',
  '## 架构优化文件',
  '',
  `本次架构优化新增 ${architectureAdded.length} 个文件，修改 ${architectureModified.length} 个文件，移除 ${architectureRemoved.length} 个旧实现文件。`,
  '',
  '### 架构优化新增',
  '',
  ...architectureAdded.map((file) => `- \`${file}\``),
  '',
  '### 架构优化修改',
  '',
  ...architectureModified.map((file) => `- \`${file}\``),
  '',
  '### 架构优化移除',
  '',
  ...architectureRemoved.map((file) => `- \`${file}\``),
  '',
  '## 受保护且未修改的原始文件',
  '',
  ...[...protectedFiles].sort((left, right) => left.localeCompare(right, 'zh-CN')).map((file) => `- \`${file}\``),
  '',
  '## 实际新增文件',
]

for (const [group, entries] of [...groups.entries()].sort(([left], [right]) => left.localeCompare(right, 'zh-CN'))) {
  lines.push('', `### ${group}（${entries.length}）`, '')
  for (const file of entries) lines.push(`- \`${file}\``)
}

lines.push('', '## 实际修改文件', '', '无。', '')
writeFileSync(resolve(root, output), `${lines.join('\n')}\n`, 'utf8')
console.log(`文件清单已生成：${output}（新增交付 ${delivered.length} 个，既有修改 0 个）`)
