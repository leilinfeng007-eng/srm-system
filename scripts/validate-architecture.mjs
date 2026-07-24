import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { extname, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const read = (path) => readFileSync(resolve(root, path), 'utf8')
const errors = []
const check = (condition, message) => {
  if (!condition) errors.push(message)
}

const javaFiles = []
const walk = (directory) => {
  for (const name of readdirSync(directory)) {
    const path = resolve(directory, name)
    if (statSync(path).isDirectory()) walk(path)
    else if (extname(name) === '.java') javaFiles.push(path)
  }
}
walk(resolve(root, 'backend/src'))

const directJdbcUsage = javaFiles
  .filter((path) => !path.endsWith('ModuleBoundaryTest.java'))
  .filter((path) => /import org\.springframework\.jdbc\.core|\b(?:NamedParameter)?JdbcTemplate\b/.test(
    readFileSync(path, 'utf8'),
  ))
  .map((path) => relative(root, path))
check(directJdbcUsage.length === 0, `禁止直接使用Spring JDBC: ${directJdbcUsage.join(', ')}`)

const pom = read('backend/pom.xml')
check(
  /<mybatis-plus\.version>3\.5\.17<\/mybatis-plus\.version>/.test(pom),
  'MyBatis-Plus版本必须固定为3.5.17',
)
check(
  pom.includes('<artifactId>mybatis-plus-spring-boot3-starter</artifactId>'),
  '缺少MyBatis-Plus Spring Boot 3 starter',
)
check(
  !/<artifactId>spring-boot-starter-jdbc<\/artifactId>/.test(pom),
  '不得直接声明spring-boot-starter-jdbc',
)
check(pom.includes('<artifactId>spring-boot-starter-cache</artifactId>'), '缺少Spring Cache starter')
check(pom.includes('<artifactId>caffeine</artifactId>'), '缺少Caffeine实现')
check(
  !/(spring-boot-starter-data-redis|lettuce-core|jedis)/.test(pom),
  '阶段0不得引入Redis客户端或starter',
)

const application = read('backend/src/main/resources/application.yml')
check(/cache-enabled:\s*false/.test(application), '必须关闭MyBatis二级缓存')
check(/local-cache-scope:\s*statement/i.test(application), 'MyBatis本地缓存范围必须限制为statement')
check(/type:\s*caffeine/.test(application), 'Spring Cache当前实现必须为Caffeine')
check(/navigation-menus/.test(application), '必须显式声明导航菜单缓存名')

const mapperRoot = resolve(root, 'backend/src/main/resources/mybatis')
const mapperXml = []
const walkXml = (directory) => {
  for (const name of readdirSync(directory)) {
    const path = resolve(directory, name)
    if (statSync(path).isDirectory()) walkXml(path)
    else if (extname(name) === '.xml') mapperXml.push(path)
  }
}
walkXml(mapperRoot)
check(mapperXml.length >= 4, 'MyBatis XML查询映射不完整')
for (const path of mapperXml) {
  check(!/<cache(?:-ref)?\b/.test(readFileSync(path, 'utf8')), `${relative(root, path)}不得启用二级缓存`)
}

const cacheAnnotations = javaFiles
  .filter((path) => /@Cacheable|@CacheEvict|@CachePut/.test(readFileSync(path, 'utf8')))
  .map((path) => relative(root, path))
check(
  cacheAnnotations.every((path) => [
    'backend/src/main/java/com/srm/platform/navigation/NavigationQueryService.java',
    'backend/src/main/java/com/srm/platform/navigation/NavigationCacheInvalidator.java',
  ].includes(path)),
  `缓存只能用于非实时导航元数据并统一失效: ${cacheAnnotations.join(', ')}`,
)

for (const required of [
  'backend/src/main/java/com/srm/security/infrastructure/persistence/entity/SysUserEntity.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/mapper/UserAccountMapper.java',
  'backend/src/main/java/com/srm/security/infrastructure/persistence/MybatisUserAccountRepository.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/mapper/WorkbenchBaselineMapper.java',
  'backend/src/main/java/com/srm/workbench/infrastructure/persistence/MybatisWorkbenchBaselineRepository.java',
  'backend/src/test/java/com/srm/platform/navigation/NavigationCacheIntegrationTest.java',
]) {
  check(existsSync(resolve(root, required)), `缺少分层或缓存基线文件: ${required}`)
}

for (const required of [
  'docs/04-接口与数据设计/openapi/internal-api.json',
  'docs/04-接口与数据设计/openapi/supplier-api.json',
  'docs/04-接口与数据设计/openapi/baseline/internal-api.json',
  'docs/04-接口与数据设计/openapi/baseline/supplier-api.json',
  'frontend/packages/shared-types/src/generated/internal-api.ts',
  'frontend/packages/shared-types/src/generated/supplier-api.ts',
  'frontend/packages/api-client/src/generated/internal-api-client.ts',
  'frontend/packages/api-client/src/generated/supplier-api-client.ts',
]) {
  check(existsSync(resolve(root, required)), `缺少OpenAPI受控或生成产物: ${required}`)
}

const internal = JSON.parse(read('docs/04-接口与数据设计/openapi/internal-api.json'))
const supplier = JSON.parse(read('docs/04-接口与数据设计/openapi/supplier-api.json'))
check(Object.keys(internal.paths).length === 7, '内部端OpenAPI应包含7条阶段0接口路径')
check(Object.keys(supplier.paths).length === 0, '阶段0供应商端OpenAPI不得伪造业务接口')
check(
  Object.keys(internal.paths).every((path) => path.startsWith('/api/v1/')),
  '内部端OpenAPI存在越界路径',
)
check(
  Object.keys(supplier.paths).every((path) => path.startsWith('/supplier-api/v1/')),
  '供应商端OpenAPI存在越界路径',
)

if (errors.length) {
  console.error(`架构约束校验失败（${errors.length}项）`)
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log(
  `架构约束校验通过：${mapperXml.length}个MyBatis XML映射、零直接JdbcTemplate、`
  + `${cacheAnnotations.length}个受控缓存组件、内部/供应商双OpenAPI契约及生成客户端完整。`,
)
