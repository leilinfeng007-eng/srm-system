import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const read = (path) => readFileSync(resolve(root, path), 'utf8')
const localCompose = read('docker-compose.yml')
const productionCompose = read('deploy/docker-compose.prod.yml')
const gateway = read('deploy/nginx/gateway/default.conf')
const envExample = read('.env.example')
const productionEnvExample = read('deploy/prod.env.example')
const mysqlGuard = read('deploy/docker/mysql-secure-entrypoint.sh')
const errors = []
const check = (condition, message) => { if (!condition) errors.push(message) }
const requiredExpression = (variable) => `\${${variable}:?`

const checkComposeBaseline = (label, compose) => {
  for (const service of ['mysql', 'backend', 'internal-web', 'supplier-web', 'nginx']) {
    check(new RegExp(`^  ${service}:`, 'm').test(compose), `${label}缺少服务: ${service}`)
  }
  check(/image: mysql:8\.4/.test(compose), `${label}必须固定MySQL 8.4`)
  check((compose.match(/healthcheck:/g) ?? []).length === 5, `${label}的五个服务都必须配置健康检查`)
  check(compose.includes('mysql-secure-entrypoint.sh'), `${label}必须启用MySQL秘密入口校验`)
  check(/condition: service_healthy/.test(compose), `${label}必须使用健康启动依赖`)
}

checkComposeBaseline('本地Compose', localCompose)
checkComposeBaseline('生产Compose', productionCompose)

for (const secret of [
  'SRM_DB_PASSWORD', 'SRM_DB_ROOT_PASSWORD', 'SRM_JWT_SECRET',
  'SRM_BOOTSTRAP_ADMIN_PASSWORD', 'SRM_BOOTSTRAP_VIEWER_PASSWORD',
]) {
  check(localCompose.includes(requiredExpression(secret)), `本地Compose必须强制要求秘密变量: ${secret}`)
  check(productionCompose.includes(requiredExpression(secret)), `生产Compose必须强制要求秘密变量: ${secret}`)
  check(envExample.includes(`${secret}=CHANGE_ME`), `.env.example缺少CHANGE_ME占位: ${secret}`)
  check(new RegExp(`^${secret}=$`, 'm').test(productionEnvExample), `生产环境样例的${secret}必须留空`)
}
for (const required of [
  'SRM_BACKEND_IMAGE',
  'SRM_INTERNAL_WEB_IMAGE',
  'SRM_SUPPLIER_WEB_IMAGE',
  'SRM_IMAGE_TAG',
  'SRM_DB_NAME',
  'SRM_DB_USERNAME',
  'SRM_BOOTSTRAP_ADMIN_USERNAME',
  'SRM_BOOTSTRAP_VIEWER_USERNAME',
  'SRM_CORS_ALLOWED_ORIGINS',
  'SRM_MYSQL_VOLUME_NAME',
]) {
  check(productionCompose.includes(requiredExpression(required)), `生产Compose必须强制要求变量: ${required}`)
}
for (const image of ['SRM_BACKEND_IMAGE', 'SRM_INTERNAL_WEB_IMAGE', 'SRM_SUPPLIER_WEB_IMAGE']) {
  check(
    productionCompose.includes(`\${${image}:?${image} is required}:\${SRM_IMAGE_TAG:?SRM_IMAGE_TAG is required}`),
    `生产Compose的${image}必须使用统一SRM_IMAGE_TAG`,
  )
}
for (const repository of [
  'ccr.ccs.tencentyun.com/leizi114/srm-backend',
  'ccr.ccs.tencentyun.com/leizi114/srm-internal-web',
  'ccr.ccs.tencentyun.com/leizi114/srm-supplier-web',
]) {
  check(productionEnvExample.includes(repository), `生产环境样例缺少TCR仓库: ${repository}`)
}
check(/^SRM_IMAGE_TAG=$/m.test(productionEnvExample), '生产环境样例的SRM_IMAGE_TAG必须留空')
check(/SPRING_PROFILES_ACTIVE: prod/.test(productionCompose), '生产Compose必须激活prod配置')
check(/SRM_SECURE_COOKIES: "true"/.test(productionCompose), '生产Compose必须启用Secure Cookie')
check(/SRM_OPENAPI_ENABLED: "false"/.test(productionCompose), '生产Compose必须关闭OpenAPI')
check(!/^\s+build:/m.test(productionCompose), '生产Compose必须使用不可变镜像引用，不得现场构建')
check(/srm-mysql-data:\s*\n\s+name: \$\{SRM_MYSQL_VOLUME_NAME:\?/m.test(productionCompose), '生产Compose必须声明稳定MySQL命名卷')
check(localCompose.includes('127.0.0.1:${SRM_BACKEND_PORT:-8080}:8080'), '本地Compose的后端调试端口必须仅绑定127.0.0.1')
check(!productionCompose.includes('SRM_BACKEND_PORT'), '生产Compose不得发布后端端口')
check((localCompose.match(/\/bin\/bash.*\/dev\/tcp\/127\.0\.0\.1\/8080/g) ?? []).length === 1, '本地Compose必须使用JRE内置健康探测')
check((productionCompose.match(/\/bin\/bash.*\/dev\/tcp\/127\.0\.0\.1\/8080/g) ?? []).length === 1, '生产Compose必须使用JRE内置健康探测')
check((localCompose.match(/http:\/\/127\.0\.0\.1\/healthz/g) ?? []).length === 3, '本地Compose的Web健康检查必须固定使用IPv4回环地址')
check((productionCompose.match(/http:\/\/127\.0\.0\.1\/healthz/g) ?? []).length === 3, '生产Compose的Web健康检查必须固定使用IPv4回环地址')
check(!read('backend/Dockerfile').includes('apt-get'), '后端运行镜像不得为健康检查在线安装额外工具')
check(!read('backend/Dockerfile').includes('dependency:go-offline'), '后端镜像不得预取无关Maven插件依赖图')
check(read('backend/Dockerfile').includes('--mount=type=cache,target=/root/.m2'), '后端镜像构建必须复用BuildKit Maven缓存')
check(!/^\s+(?:MYSQL_)?PASSWORD:\s+[^$]/m.test(localCompose), '本地Compose不得写入固定数据库密码')
check(!/^\s+(?:MYSQL_)?PASSWORD:\s+[^$]/m.test(productionCompose), '生产Compose不得写入固定数据库密码')
check(mysqlGuard.includes('SRM_VALIDATE_ONLY'), 'MySQL秘密入口脚本必须支持非容器策略校验')
check(gateway.includes('location /api/'), 'Nginx缺少/api反向代理')
check(gateway.includes('location /supplier/'), 'Nginx缺少供应商端路径规则')
check(gateway.includes('proxy_pass http://srm_internal_web'), 'Nginx缺少内部端入口')
check(gateway.includes('proxy_pass http://srm_backend'), 'Nginx缺少后端上游')
check((gateway.match(/{/g) ?? []).length === (gateway.match(/}/g) ?? []).length, 'Nginx配置花括号不平衡')
check(read('deploy/docker/internal-web.Dockerfile').includes('node:22-alpine'), '内部端镜像必须使用Node 22构建')
check(read('deploy/docker/supplier-web.Dockerfile').includes('node:22-alpine'), '供应商端镜像必须使用Node 22构建')
check(read('backend/Dockerfile').includes('eclipse-temurin-17'), '后端镜像必须使用JDK 17')

if (errors.length) {
  console.error(`部署配置静态校验失败（${errors.length}项）`)
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log('部署配置静态校验通过：本地与生产模板均含5个服务、5个健康检查、启动依赖、MySQL 8.4、持久卷和秘密必填约束。')
console.log('说明：本检查不启动Docker、MySQL或Nginx，不能替代容器运行与数据卷持久化验收。')
