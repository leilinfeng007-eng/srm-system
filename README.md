# SRM 供应商协同管理系统

本仓库是 SRM V1.0 的阶段0工程基线：前后端分离、Java 模块化单体、内部管理端与供应商端双前端应用、数据库驱动的菜单与 RBAC 最小闭环。阶段0仅交付可运行的工程架构和功能页面骨架，不包含供应商、寻源、合同、采购、交付、质量、结算、绩效等领域的真实业务 CRUD。

## 工程组成

- `backend/`：Java 17、Spring Boot 3、Maven、Spring Security、MyBatis-Plus、Spring Cache/Caffeine、Flyway、MySQL 8。
- `frontend/apps/internal-web/`：内部管理端，12 个一级功能域、85 个 V1.0 二级页面骨架和独立工作台首页。
- `frontend/apps/supplier-web/`：独立供应商端，登录占位和 9 个门户入口。
- `frontend/packages/`：无业务权限判断的共享 UI、类型、API 客户端和配置。
- `database/`：迁移校验与人工运维说明；数据库结构唯一权威在 `backend/src/main/resources/db/migration/`。
- `deploy/`：Docker Compose、Nginx、镜像构建和环境变量模板。
- `docs/`：业务基线、详细设计索引、接口数据规范、测试验收与运维记录。
- `scripts/`：全量检查、构建、骨架校验和本地启停入口。

## 环境要求

- JDK 17
- Maven 3.9+（本机可直接使用 IntelliJ IDEA 内置 Maven）
- Node.js 22、npm 10+
- Docker Engine 26+ 与 Docker Compose v2（全栈联调时需要）

## 首次构建

```bash
./scripts/maven-command.sh -f backend/pom.xml -B clean test
npm ci
npm run lint
npm run typecheck
npm test
npm run build
npm run openapi:check
node scripts/validate-skeleton.mjs
./scripts/check.sh
```

## 本地全栈启动

1. 复制环境变量模板：`cp .env.example .env`。
2. 将模板中的占位值替换为仅用于本机的强密码和至少 48 字符 JWT 密钥；不要提交 `.env`。
3. 执行 `./scripts/start-local.sh`。
4. 等待健康检查通过后访问：
   - 统一入口/internal-web：`http://localhost:8088/`
   - supplier-web：`http://localhost:8088/supplier/`
   - 后端健康：`http://localhost:8088/actuator/health/readiness`
5. 内部端使用 `SRM_BOOTSTRAP_ADMIN_USERNAME` 或 `SRM_BOOTSTRAP_VIEWER_USERNAME` 对应账号登录；密码取自本机 `.env`。

停止服务：`./scripts/stop-local.sh`。普通停止不会删除 MySQL 持久卷。开发数据重置属于破坏性操作，必须由操作者确认并显式执行 `docker compose down --volumes`，执行前自行备份。

Windows 建议在 Git Bash 或 WSL 中直接运行上述脚本。仅使用 PowerShell 时，可执行 `Copy-Item .env.example .env`，填写秘密后以 `docker compose up --build --detach --wait` 启动、以 `docker compose down` 停止；检查命令按顺序运行 `mvn -f backend/pom.xml -B clean test`、`npm ci` 和根 `package.json` 中的各个 npm 脚本。IntelliJ IDEA 内置 Maven 的 Windows 可执行文件位于 IDEA 安装目录的 `plugins\maven\lib\maven3\bin\mvn.cmd`。

## 后端单独运行

后端没有仓库内置密码或 JWT 密钥。先提供 `SRM_DB_*`、`SRM_JWT_SECRET`、`SRM_BOOTSTRAP_ADMIN_PASSWORD` 与 `SRM_BOOTSTRAP_VIEWER_PASSWORD`，然后执行：

```bash
./scripts/maven-command.sh -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

接口统一前缀为 `/api/v1`。登录、刷新、退出、当前用户、动态菜单、模块元数据和健康检查见 [API规范](docs/04-接口与数据设计/API规范.md)。

开发环境默认开放内部端`/v3/api-docs/internal`、供应商端`/v3/api-docs/supplier`和
`/swagger-ui.html`，可用
`SRM_OPENAPI_ENABLED=false`关闭；测试默认关闭、按测试显式开启；生产固定默认关闭。
工作台首页会调用`/api/v1/workbench/baseline`，真实穿过Controller、Application
Service、Domain Repository和MyBatis-Plus实现，仅执行数据库连接探测，不读取业务数据。

受控 OpenAPI 位于 `docs/04-接口与数据设计/openapi/`。后端测试从实际 Controller 导出
双域规范，`npm run openapi:check` 执行规范校验、相对基线的破坏性变更检查、静态文档生成、
TypeScript 类型与 `openapi-fetch` 客户端漂移检查。评审契约变更后使用
`npm run openapi:update` 更新规范与生成代码；只有明确接受新的兼容性起点时才运行
`npm run openapi:baseline`。

## 生产部署模板

- 模板：`deploy/docker-compose.prod.yml`
- 环境变量样例：`deploy/prod.env.example`
- 操作与安全说明：`deploy/README.md`

生产模板包含MySQL 8.4、backend、internal-web、supplier-web和nginx五个服务，使用
不可变应用镜像、服务健康依赖和稳定MySQL命名卷。所有数据库密码、JWT秘密及初始账号
密码均为必填环境变量，不含固定值；MySQL入口和后端启动还会拒绝占位或弱值。

2026-07-24 已在 Apple Silicon Docker Desktop 上完成五服务、真实 MySQL 8.4.10、
Flyway 11.15.0、Nginx 代理及数据卷持久化验收；复验记录见
`docs/05-测试验收与运维/`。正式部署仍须使用私有环境变量文件和经过审批的不可变镜像。

## 常见故障

- Flyway 无法连接：确认 MySQL 健康、数据库名和 `SRM_DB_*` 一致。
- 生产配置拒绝启动：生产模式必须显式提供数据库密码、JWT 密钥和两个种子账号密码，不接受不安全默认值。
- 占位秘密拒绝启动：`CHANGE_ME`、`REPLACE_WITH`、示例密码、短密码及相同的管理员/查看者密码均会被拒绝。
- 前端刷新 404：请通过本仓库 Nginx 配置启动；两个 SPA 均已配置 history fallback。
- 菜单或路由不一致：执行 `node scripts/validate-skeleton.mjs`，不要手工绕过 `frontend/module-manifest.json`。
- 401：访问令牌过期后客户端会使用 HttpOnly 刷新 Cookie 尝试一次刷新；刷新会话撤销后需重新登录。

## 阶段边界与协作

开发前必须阅读 [AGENTS.md](AGENTS.md)。共享基线、模块边界、权限格式、迁移规则和验收报告格式均已冻结。阶段0使用进程内 Caffeine，不包含微服务、Redis、消息中间件、搜索引擎、Kubernetes、真实外部系统集成或具体业务功能。
