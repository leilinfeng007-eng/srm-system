# SRM 多 Agent 工程规则

## 适用范围与业务边界

本文件适用于仓库全部目录。阶段0只允许实现工程底座、最小内部认证/RBAC、动态菜单、页面骨架、部署与验证；禁止增加真实业务 CRUD、业务表、模拟交易、伪造 KPI、完整流程引擎、供应商真实身份或数据隔离。

## 受保护的共享基线

以下文件只能由主架构维护，模块开发者修改前必须先获得明确授权：

- `backend/pom.xml`
- `backend/src/main/java/com/srm/common/`、`config/`、`security/`、`platform/`
- `backend/src/main/resources/db/migration/` 中既有迁移
- 根 `package.json`、`package-lock.json`、`tsconfig.base.json`
- `frontend/module-manifest.json`
- `frontend/supplier-portal-manifest.json`
- `frontend/apps/internal-web/src/router/route-registry.ts`
- 根 `docker-compose.yml`、`deploy/docker/`、`deploy/nginx/`
- 根 `AGENTS.md`、`.editorconfig`、`.gitignore`

不得覆盖、删除或改写 `docs/01-业务蓝图与总体设计/` 与 `docs/02-业务需求与功能规划/` 中的用户基线文件。

## 模块边界

- 后端领域包固定为 `workbench`、`supplier`、`sourcing`、`contract`、`source`、`procurement`、`delivery`、`quality`、`settlement`、`performance`、`masterdata`、`system`。
- `common`、`config`、`security`、`platform` 不得依赖业务领域包。
- 业务模块只依赖共享契约；不得引用其他模块的 `infrastructure` 或 `repository` 实现，不得直接更新其他模块数据表。
- 跨模块查询使用公开 QueryService/DTO；跨模块变更使用 ApplicationService 或领域事件契约。
- Controller 只做协议转换、校验、鉴权入口和响应；禁止写 SQL、状态机或返回 `Map<String,Object>`。
- 数据访问统一使用 MyBatis-Plus：Entity、Mapper、Repository 适配器、领域 DTO/模型分离；
  复杂查询可使用 XML，禁止业务层直接依赖 Mapper 或持久化 Entity。
- Flyway 是数据库结构和种子数据的唯一管理工具；不得使用 MyBatis-Plus 自动建表。
- 禁止 MyBatis 二级缓存。缓存统一使用 Spring Cache；当前实现为 Caffeine，必须声明容量、
  TTL 和精确失效入口，不得缓存实时交易状态。
- 新业务表只能由对应模块在详细设计通过后以新 Flyway 版本创建；禁止改写已发布迁移。

## 前端规则

- 内部端侧栏必须来自后端菜单树；`route-registry` 只解析清单中的安全组件键。
- `menuCode`、`route`、`componentKey`、`permission` 不得在运行时猜测或拼接。
- 供应商端不得导入内部端认证 store、内部菜单接口或内部权限判断。
- 共享包不得包含领域业务权限或内部/供应商身份混用逻辑。
- 内部端与供应商端 OpenAPI 契约必须分离；共享 TypeScript 类型和 API 客户端只能从受控
  OpenAPI 生成，不得手改 `frontend/packages/*/src/generated/`。

## 编码与安全

- 权限码格式：`domain:resource:action`，resource 使用 kebab-case。
- 所有 API 使用统一 `ApiResponse`；分页使用 `PageResult`；异常由全局处理器转换。
- SQL 必须参数化；状态入库存稳定英文 code；金额不得使用 float/double。
- 日志必须带 `traceId`，不得记录密码、令牌、完整银行账号、身份证明或附件内容。
- 秘密只允许从环境变量或外部秘密管理注入；禁止提交 `.env`、真实地址、密码或密钥。

## 必跑命令

```bash
./scripts/maven-command.sh -f backend/pom.xml -B clean test
npm ci && npm run lint && npm run typecheck && npm test && npm run build
npm run openapi:check
node scripts/validate-architecture.mjs
node scripts/validate-skeleton.mjs
./scripts/check.sh
docker compose --env-file .env.example config
```

模块变更至少运行本模块测试；修改共享基线必须运行全量检查。

## 交付报告格式

每次交付至少报告：

1. 结论与范围。
2. 新增/修改文件及原因。
3. 实际执行命令和结果。
4. 菜单、页面、权限、迁移等可机器统计的数量。
5. 安全与边界检查。
6. 遗留问题、影响、原因和后续责任阶段。
7. Git 状态；未经授权不得提交、推送、部署或删除数据。
