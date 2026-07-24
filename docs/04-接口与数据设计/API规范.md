# API 工程规范

- 基础路径：`/api/v1`；REST JSON；UTF-8；时间使用 ISO 8601 UTC。
- Controller 使用显式 Request/Response DTO，不返回数据库行、实体或 `Map<String,Object>`。
- 成功与失败统一返回 `ApiResponse<T>`：`code`、`message`、`data`、`traceId`、`timestamp`。
- HTTP 状态表达协议结果；`code` 是稳定平台/业务错误码。生产环境不返回堆栈。
- 分页数据固定为 `items`、`page`、`pageSize`、`total`、`totalPages`。
- 校验错误返回字段级 `details`；401/403 同样遵守统一响应。
- 写入接口预留 `Idempotency-Key`；MyBatis 参数必须使用 `#{...}` 绑定，禁止 `${...}` 拼接用户输入。
- 请求可传 `X-Trace-Id`，服务端校验后复用，否则生成新值，并在响应头和响应体返回。

## OpenAPI

- 开发环境默认启用，可通过`SRM_OPENAPI_ENABLED=false`关闭。
- 测试环境默认关闭，单项测试可显式开启。
- 生产环境固定默认关闭`/v3/api-docs`和Swagger UI。
- 开启时内部端规范位于`/v3/api-docs/internal`，供应商端规范位于
  `/v3/api-docs/supplier`，界面入口为`/swagger-ui.html`。
- internal 分组只匹配`/api/v1/**`；supplier 分组只匹配`/supplier-api/v1/**`；
  两者不展示Actuator。阶段0供应商端尚无真实业务接口，其契约路径为空。
- 实际 Controller 规范由后端测试导出；受控规范、Redocly 配置和兼容基线位于
  `docs/04-接口与数据设计/openapi/`。
- `npm run openapi:check` 必须同时通过实际导出漂移、规范 lint、相对 baseline 的破坏性
  变更、静态文档和前端 TypeScript 类型/API 客户端生成一致性检查。
- `npm run openapi:update` 只在契约评审后更新规范与生成产物，不移动既有兼容基线；
  `npm run openapi:baseline` 只能在明确接受新的兼容性起点后执行。
- OpenAPI不能替代接口鉴权、统一响应或自动化测试；生产开关关闭时所有全局及分组文档
  地址和 Swagger UI 必须不可访问。

阶段0接口：

| 方法 | 路径 | 权限/用途 |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | 匿名；内部本地账号登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Cookie；旋转访问令牌 |
| POST | `/api/v1/auth/logout` | 已登录；撤销刷新会话及关联访问令牌 |
| GET | `/api/v1/auth/me` | 已登录；当前用户/角色/权限摘要 |
| GET | `/api/v1/navigation/menus` | 已登录；数据库与角色授权过滤后的菜单树 |
| GET | `/api/v1/meta/modules` | `system:permission:view`；12 域元数据 |
| GET | `/api/v1/workbench/baseline` | `workbench:home:view`；标准分层与数据库连接只读验证 |
| GET | `/actuator/health/liveness` | 健康检查，不泄露配置 |
| GET | `/actuator/health/readiness` | 应用与数据库就绪检查 |
