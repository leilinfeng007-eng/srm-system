const baseUrl = process.env.SRM_SMOKE_BASE_URL ?? 'http://localhost:8080'
const adminUsername = process.env.SRM_SMOKE_ADMIN_USERNAME
const adminPassword = process.env.SRM_SMOKE_ADMIN_PASSWORD
const viewerUsername = process.env.SRM_SMOKE_VIEWER_USERNAME
const viewerPassword = process.env.SRM_SMOKE_VIEWER_PASSWORD

if (![adminUsername, adminPassword, viewerUsername, viewerPassword].every(Boolean)) {
  console.error('请提供 SRM_SMOKE_ADMIN_USERNAME/PASSWORD 与 SRM_SMOKE_VIEWER_USERNAME/PASSWORD。')
  process.exit(2)
}

const check = (condition, message) => {
  if (!condition) throw new Error(message)
}
const setCookieFrom = (response) => response.headers.get('set-cookie')
const cookieFrom = (response) => setCookieFrom(response)?.split(';', 1)[0]
const envelope = async (response) => ({ response, body: await response.json() })
const authHeaders = (accessToken) => ({ Authorization: `Bearer ${accessToken}` })

const login = async (username, password) => {
  const result = await envelope(await fetch(`${baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  }))
  check(result.response.status === 200 && result.body.code === '0', `登录失败: ${username}`)
  const setCookie = setCookieFrom(result.response)
  const cookie = cookieFrom(result.response)
  check(cookie?.startsWith('SRM_REFRESH='), '登录未返回HttpOnly刷新Cookie')
  check(/;\s*HttpOnly/i.test(setCookie ?? ''), '刷新Cookie未启用HttpOnly')
  check(/;\s*Secure/i.test(setCookie ?? ''), '生产刷新Cookie未启用Secure')
  return { accessToken: result.body.data.accessToken, cookie }
}

const health = await envelope(await fetch(`${baseUrl}/actuator/health/readiness`))
check(health.response.status === 200 && health.body.status === 'UP', 'readiness未就绪')

const admin = await login(adminUsername, adminPassword)
const me = await envelope(await fetch(`${baseUrl}/api/v1/auth/me`, { headers: authHeaders(admin.accessToken) }))
check(me.response.status === 200 && me.body.data.roles.includes('SUPER_ADMIN'), '管理员/me校验失败')

const adminMenus = await envelope(await fetch(`${baseUrl}/api/v1/navigation/menus`, { headers: authHeaders(admin.accessToken) }))
const adminPages = adminMenus.body.data.reduce((count, domain) => count + domain.children.length, 0)
check(adminMenus.response.status === 200 && adminMenus.body.data.length === 3, '管理员一级菜单数量不正确')
check(adminPages === 18, '管理员可访问页面数量不正确')
check(!JSON.stringify(adminMenus.body).includes('MENU_PROCUREMENT_FORECAST_PLAN'), '隐藏预留被下发')

const modules = await envelope(await fetch(`${baseUrl}/api/v1/meta/modules`, { headers: authHeaders(admin.accessToken) }))
check(modules.response.status === 200 && modules.body.data.length === 12, '模块元数据校验失败')
const workbench = await envelope(await fetch(`${baseUrl}/api/v1/workbench/baseline`, {
  headers: authHeaders(admin.accessToken),
}))
check(
  workbench.response.status === 200
    && workbench.body.data.moduleCode === 'workbench'
    && workbench.body.data.status === 'READY'
    && workbench.body.data.databaseReachable === true,
  '工作台标准分层链路校验失败',
)

const criticalGetPaths = [
  '/api/v1/system/users',
  '/api/v1/system/roles',
  '/api/v1/system/permissions',
  '/api/v1/system/dictionaries',
  '/api/v1/system/parameters',
  '/api/v1/system/audit-logs',
  '/api/v1/master-data/materials',
  '/api/v1/master-data/organizations',
]
for (const path of criticalGetPaths) {
  const result = await envelope(await fetch(`${baseUrl}${path}`, { headers: authHeaders(admin.accessToken) }))
  check(result.response.status === 200 && result.body.code === '0', `关键API失败: ${path}`)
}

for (const path of ['/v3/api-docs', '/v3/api-docs/internal', '/swagger-ui.html', '/swagger-ui/index.html']) {
  const response = await fetch(`${baseUrl}${path}`, { redirect: 'manual' })
  check(response.status === 404, `生产OpenAPI安全开关未生效: ${path}`)
}

const refreshed = await envelope(await fetch(`${baseUrl}/api/v1/auth/refresh`, {
  method: 'POST',
  headers: { Cookie: admin.cookie },
}))
check(refreshed.response.status === 200, '刷新失败')
const refreshedCookie = cookieFrom(refreshed.response)
const refreshedAccess = refreshed.body.data.accessToken
const oldAccess = await envelope(await fetch(`${baseUrl}/api/v1/auth/me`, { headers: authHeaders(admin.accessToken) }))
check(oldAccess.response.status === 401, '刷新后旧访问令牌仍有效')

const logout = await fetch(`${baseUrl}/api/v1/auth/logout`, {
  method: 'POST',
  headers: { ...authHeaders(refreshedAccess), Cookie: refreshedCookie },
})
check(logout.status === 200, '退出失败')
const revoked = await envelope(await fetch(`${baseUrl}/api/v1/auth/me`, { headers: authHeaders(refreshedAccess) }))
check(revoked.response.status === 401, '退出后访问令牌仍有效')

const viewer = await login(viewerUsername, viewerPassword)
const viewerMenus = await envelope(await fetch(`${baseUrl}/api/v1/navigation/menus`, { headers: authHeaders(viewer.accessToken) }))
check(
  viewerMenus.response.status === 200
    && viewerMenus.body.data.length === 1
    && viewerMenus.body.data[0].children.length === 1,
  '查看者菜单过滤失败',
)
const forbidden = await envelope(await fetch(`${baseUrl}/api/v1/meta/modules`, { headers: authHeaders(viewer.accessToken) }))
check(forbidden.response.status === 403 && forbidden.body.code === 'PLATFORM_ACCESS_DENIED', '后端403鉴权失败')

console.log('HTTP联调通过')
console.log(`readiness=${health.body.status}, adminDomains=${adminMenus.body.data.length}, adminPages=${adminPages}, modules=${modules.body.data.length}, workbench=${workbench.body.data.status}`)
console.log(`criticalApis=${criticalGetPaths.length}, openApiDisabled=passed, secureCookie=passed`)
console.log(`refreshRotation=passed, logoutRevocation=passed, viewerDomains=${viewerMenus.body.data.length}, viewerForbidden=403`)
