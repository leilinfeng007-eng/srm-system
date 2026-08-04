import { existsSync, readFileSync } from 'node:fs'
import { extname, relative, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const errors = []
const excluded = ['.git', 'node_modules', 'target', 'dist']
const binary = new Set(['.docx', '.xlsx', '.png', '.pdf', '.jar', '.class'])
const legacyPackagePattern = new RegExp(['com', 'scm'].join('\\.'))
const legacyMenuPattern = new RegExp(['MENU', 'SCM'].join('_'))
const allowedEnvironmentExamples = new Set([
  '.env.example',
  'deploy/prod.env.example',
  'deploy/stage1-acceptance.env.example',
])

const gitFiles = spawnSync('git', ['ls-files', '-co', '--exclude-standard', '-z'], {
  cwd: root,
  encoding: 'utf8',
})
if (gitFiles.status !== 0) {
  throw new Error(`无法获取Git提交候选文件: ${gitFiles.stderr.trim()}`)
}
const files = [...new Set(gitFiles.stdout.split('\0').filter(Boolean))]
  .filter((name) => !name.split('/').some((part) => excluded.includes(part)))
  .filter((name) => name !== '.DS_Store' && !binary.has(extname(name)))
  .map((name) => resolve(root, name))

for (const path of files) {
  const name = relative(root, path)
  const content = readFileSync(path, 'utf8')
  if (/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/.test(content)) errors.push(`${name}: 检测到私钥`)
  if (/\bAKIA[0-9A-Z]{16}\b/.test(content)) errors.push(`${name}: 检测到访问密钥格式`)
  if (/\beyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\b/.test(content)) errors.push(`${name}: 检测到JWT格式`)
  if (legacyPackagePattern.test(content) || legacyMenuPattern.test(content)) {
    errors.push(`${name}: 检测到旧SCM包名或菜单编码`)
  }
}

for (const path of [
  'backend/src/main/resources/application.yml',
  'docker-compose.yml',
  'deploy/docker-compose.prod.yml',
]) {
  for (const [index, line] of readFileSync(resolve(root, path), 'utf8').split('\n').entries()) {
    if (/(?:password|secret):/i.test(line) && !line.includes('${')) {
      errors.push(`${path}:${index + 1}: 秘密必须来自环境变量`)
    }
  }
}

for (const path of files) {
  const name = relative(root, path)
  const basename = name.split('/').at(-1)
  if ((basename?.startsWith('.env') || basename?.endsWith('.env')) && !allowedEnvironmentExamples.has(name)) {
    errors.push(`不得提交本地环境文件: ${name}`)
  }
}
for (const forbidden of ['deploy/.env', 'deploy/.env.prod']) {
  if (existsSync(resolve(root, forbidden))) errors.push(`不得提交 ${forbidden}`)
}

const productionEnv = readFileSync(resolve(root, 'deploy/prod.env.example'), 'utf8')
const acceptanceEnv = readFileSync(resolve(root, 'deploy/stage1-acceptance.env.example'), 'utf8')
for (const secret of [
  'SRM_DB_PASSWORD',
  'SRM_DB_ROOT_PASSWORD',
  'SRM_JWT_SECRET',
  'SRM_BOOTSTRAP_ADMIN_PASSWORD',
  'SRM_BOOTSTRAP_VIEWER_PASSWORD',
]) {
  if (!new RegExp(`^${secret}=$`, 'm').test(productionEnv)) {
    errors.push(`deploy/prod.env.example: ${secret}必须为空，确保样例无法直接启动`)
  }
  if (!new RegExp(`^${secret}=$`, 'm').test(acceptanceEnv)) {
    errors.push(`deploy/stage1-acceptance.env.example: ${secret}必须为空，确保样例无法直接启动`)
  }
}

const guard = resolve(root, 'deploy/docker/mysql-secure-entrypoint.sh')
const invalidGuard = spawnSync('sh', [guard], {
  env: {
    ...process.env,
    SRM_VALIDATE_ONLY: 'true',
    MYSQL_PASSWORD: 'CHANGE_ME_LOCAL_DB_PASSWORD',
    MYSQL_ROOT_PASSWORD: 'CHANGE_ME_LOCAL_ROOT_PASSWORD',
  },
  encoding: 'utf8',
})
if (invalidGuard.status === 0) errors.push('MySQL秘密入口脚本未拒绝CHANGE_ME占位值')
const validGuard = spawnSync('sh', [guard], {
  env: {
    ...process.env,
    SRM_VALIDATE_ONLY: 'true',
    MYSQL_PASSWORD: 'Local-Db-Only!2026',
    MYSQL_ROOT_PASSWORD: 'Local-Root-Only!2026',
  },
  encoding: 'utf8',
})
if (validGuard.status !== 0) errors.push('MySQL秘密入口脚本拒绝了符合策略的值')

const baseConfig = readFileSync(resolve(root, 'backend/src/main/resources/application.yml'), 'utf8')
const devConfig = readFileSync(resolve(root, 'backend/src/main/resources/application-dev.yml'), 'utf8')
const prodConfig = readFileSync(resolve(root, 'backend/src/main/resources/application-prod.yml'), 'utf8')
const testConfig = readFileSync(resolve(root, 'backend/src/test/resources/application-test.yml'), 'utf8')
if (!/springdoc:\s*\n\s+api-docs:\s*\n\s+enabled: false/m.test(baseConfig)) {
  errors.push('基础配置必须默认关闭OpenAPI')
}
if (!/enabled: \$\{SRM_OPENAPI_ENABLED:true\}/.test(devConfig)) {
  errors.push('开发配置必须支持OpenAPI环境开关且默认开启')
}
for (const [name, content] of [['生产', prodConfig], ['测试', testConfig]]) {
  if (!/springdoc:\s*\n\s+api-docs:\s*\n\s+enabled: false/m.test(content)
      || !/swagger-ui:\s*\n\s+enabled: false/m.test(content)) {
    errors.push(`${name}配置必须默认关闭OpenAPI与Swagger UI`)
  }
}
const sensitivePolicy = readFileSync(
  resolve(root, 'backend/src/main/java/com/srm/config/SensitiveValuePolicy.java'),
  'utf8',
)
for (const marker of ['CHANGE_ME', 'REPLACE_WITH', 'EXAMPLE_PASSWORD', 'PLACEHOLDER']) {
  if (!sensitivePolicy.includes(marker)) errors.push(`后端敏感值策略缺少占位检测: ${marker}`)
}

if (errors.length) {
  console.error(`安全与秘密扫描失败（${errors.length}项）`)
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log(`安全与秘密扫描通过（${files.length}个Git提交候选文本文件）：无私钥、访问密钥、JWT、旧包名、固定主配置密码或待提交.env；部署入口可拒绝占位秘密。`)
