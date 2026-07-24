# 阶段0 Docker 专项验收报告

验收日期：2026-07-23～2026-07-24（Asia/Shanghai）  
验收平台：Apple Silicon Mac，Docker Engine 29.6.2（linux/arm64），Docker Compose 5.3.1  
验收依据：阶段0任务书、实际代码、实际命令输出；不直接沿用原开发报告结论

## 1. 最终结论

五服务构建、启动与健康检查、真实 MySQL 8.4 空库迁移、重复启动、Nginx 双前端与 API
代理、认证/RBAC/动态菜单、命名卷持久化和生产秘密/OpenAPI 约束均已实际通过。验收中发现并
修复了后端镜像依赖 Ubuntu 软件源、后端调试端口绑定范围过宽、Alpine `localhost` 健康检查
误用 IPv6，以及 Maven `dependency:go-offline` 拉取大量无关插件依赖四项部署问题。

本报告首次验收时发现的 Flyway 10.20.1 / MySQL 8.4.10 兼容性 P1，已于 2026-07-24
升级至首个满足范围的稳定版本 11.15.0，并完成独立空库、重复启动、数据卷、五服务及权限
专项复验。实际 MySQL 为 8.4.10，兼容性警告扫描为 0。以《阶段0 Flyway P1 整改复验报告》
作为关闭证据后，当前 P0、P1 均为 0，阶段0可以正式签收并具备首次 Git 基线提交条件。

## 2. 实际构建与五服务状态

Docker Hub 直连多次出现 EOF/short read。验收仅在本机从 Google 的 Docker Hub 只读缓存
`mirror.gcr.io/library/*` 拉取官方基础镜像，再按原 Compose/Dockerfile 名称本地打标签；仓库
未写入镜像代理配置。所有实际运行镜像均核验为 `linux/arm64`。

| 服务 | 实际镜像/版本 | 健康状态 | 宿主端口 |
| --- | --- | --- | --- |
| mysql | MySQL 8.4.10，arm64 | Healthy | 无 |
| backend | Spring Boot 3.4.1 / Java 17，arm64 | Healthy | `127.0.0.1:18080` |
| internal-web | Nginx 1.27.5，arm64 | Healthy | 无 |
| supplier-web | Nginx 1.27.5，arm64 | Healthy | 无 |
| nginx | Nginx 1.27.5，arm64 | Healthy | `18088` |

三个 Nginx 实例均执行 `nginx -t`，结果为 syntax ok / test successful。验收结束后执行
`docker compose down`，本次项目容器和项目网络已停止并移除；三个项目应用镜像和
`srm-stage0-docker-acceptance_mysql-data` 数据卷保留。未执行 `docker system prune`，未删除
任何数据卷或无关 Docker 资源。

## 3. MySQL、Flyway 与数据卷

- 空命名卷首次启动后，数据库为 MySQL 8.4.10。
- `flyway_schema_history` 实际包含并成功执行 V1、V2、V3，`success=1`。
- 数据库实际为 11 张表：10 张阶段0系统表，加 1 张 Flyway 历史表。
- 初始数据实测：99 条菜单、98 条启用菜单、87 条权限、2 个角色、2 个用户。
- 停止并删除五个容器但不删除数据卷，再执行 `docker compose up -d --wait`，五服务再次
  Healthy；Flyway 日志为 `Successfully validated 3 migrations`、`Current version ... 3`、
  `Schema ... is up to date. No migration necessary`。
- 在 `sys_operation_log` 写入唯一 traceId 的验收临时记录，容器重建前为 1 条、重建后仍为
  1 条，证明数据卷持久化；结束时按 traceId 和 actionCode 精确删除，复核为 0。
- 历史发现：Flyway 10.20.1 日志提示 MySQL 8.4 新于其已测试范围（最高 8.1）。该 P1
  已由 Flyway 11.15.0 隔离空库复验关闭；新日志中的同类警告为 0。

## 4. Nginx、前后端与权限联调

| 检查 | 实际结果 |
| --- | --- |
| 内部端 `/` | HTTP 200 |
| 供应商端 `/supplier/` | HTTP 200 |
| 网关 `/healthz` | HTTP 200 |
| 网关代理后端 readiness | HTTP 200，状态 UP |
| 未登录 `/api/v1/auth/me` | HTTP 401 |
| 管理员登录与菜单 | 12 个一级域、86 个启用页面 |
| 模块与工作台 | 12 个模块、workbench=READY |
| JWT 刷新与退出 | refresh rotation、logout revocation 均通过 |
| viewer RBAC | 仅 2 个一级域；越权 HTTP 403 |

MySQL、internal-web、supplier-web 未发布宿主端口；生产 Compose 不发布 backend 端口，只有
统一 Nginx 网关发布端口。本地 Compose 的 backend 调试口整改为仅绑定 `127.0.0.1`。

## 5. 生产安全验证

- `docker compose --env-file /dev/null -f deploy/docker-compose.prod.yml config` 退出码 1，
  首个错误为必需的 `COMPOSE_PROJECT_NAME` 缺失。
- 使用长度足够但含 `CHANGE_ME` 的 JWT 运行 production 后端，容器退出码 1，明确错误为
  `SRM_JWT_SECRET must not use an example or placeholder value`。
- 使用有效临时秘密启动独立 production 后端：readiness=200，正常鉴权业务接口=401，
  `/v3/api-docs`=404，`/swagger-ui/index.html`=404；随后已停止并自动删除临时容器。
- `node scripts/validate-security.mjs` 最终扫描 285 个文本文件通过；无私钥、访问密钥、固定密码、
  JWT 或本地 `.env`。

## 6. 实际执行命令与结果

| 命令/命令组 | 结果 |
| --- | --- |
| `docker version`、`docker compose version`、`docker info` | Engine 29.6.2，Compose 5.3.1，arm64 |
| `docker compose config --quiet` | 本地五服务配置通过 |
| 空环境 production `docker compose ... config` | 退出 1，必需变量缺失时明确失败 |
| `BUILDKIT_PROGRESS=plain ./scripts/start-local.sh` | 首次发现并定位镜像构建/健康检查问题；整改后五服务启动 |
| `docker compose up -d --wait --wait-timeout 180` | 五服务全部 Healthy |
| `docker compose ps`、`docker inspect`、`docker compose logs` | 状态、端口、关键日志符合本报告 |
| MySQL CLI 查询 | 8.4.10；V1～V3 成功；10 系统表；种子数量符合 |
| `curl` 内部端、供应商端、health、API | 200/200/200/200，未登录接口 401 |
| `node scripts/live-smoke.mjs` | 登录、菜单、工作台、刷新、退出、RBAC 全通过 |
| 三次 `nginx -t` | 全部成功 |
| 临时记录 + `docker compose down/up` | 重建前后 1/1；清理后 0；卷保留 |
| production 临时后端 curl | readiness 200、业务鉴权 401、OpenAPI/Swagger 404/404 |
| production `CHANGE_ME` 后端 | 退出 1，占位秘密被拒绝 |
| `node scripts/validate-deploy.mjs` | 通过 |
| `./scripts/check-migrations.sh` | 3 版本、10 系统表、H2 2.2.224，通过 |
| `node scripts/validate-security.mjs` | 最终工作区文本文件扫描通过 |
| `./scripts/check.sh` | 退出 0：后端 18/18、前端 5/5、lint/typecheck/build、骨架和静态检查全通过 |
| `docker compose down` | 项目容器/网络停止移除，镜像和数据卷保留 |

为保证记录完整，验收中的失败尝试如下：

- Docker Hub 直拉 MySQL 出现 `short read`/EOF；改用 Google 的 Docker Hub 官方镜像只读缓存
  拉取相同 arm64 镜像并本地打原标签后继续。
- 后端运行镜像为安装 curl 执行 `apt-get update` 时，Ubuntu ports 镜像返回 HTTP 500/EOF；
  改为 JRE 自带 bash TCP 健康探测。
- `dependency:go-offline` 超过 8 分钟仍在拉取与运行无关的 Flyway 云插件依赖，主动中止；
  改为 BuildKit Maven 缓存加实际 `clean package`，随后构建及 18 项测试成功。
- 两个 Web 容器首次为 Unhealthy，日志为 `localhost` 连接拒绝；定位为 Alpine 解析到 IPv6，
  固定 `127.0.0.1` 后三个 Web/Nginx 健康检查全部通过。
- 一次冒烟命令因手工猜测验收密码而登录失败；改为从临时、被 Git 忽略的 `.env` 安全注入
  后完整冒烟通过。临时 `.env` 已删除，安全扫描复核通过。

内部端生产构建仍有约 1.06 MB 主包的 500KB 警告，按既定范围留作后续性能优化。

## 7. S0-01～S0-08 Docker 专项对照

| 任务包 | 结论 | Docker 专项证据 |
| --- | --- | --- |
| S0-01 | 通过 | arm64 环境可构建；全量门禁通过；受保护文档未改变 |
| S0-02 | 通过 | backend 镜像构建、18 项测试、健康检查和模块边界通过 |
| S0-03 | 通过 | MySQL 8.4.10 空库 V1～V3、幂等重启成功；Flyway 11.15.0 复验警告为 0 |
| S0-04 | 通过 | 实库 99/98 菜单、87 权限；联调 12 域/86 页面 |
| S0-05 | 通过 | internal-web Healthy，内部端及动态菜单/API 联通 |
| S0-06 | 通过 | supplier-web Healthy，`/supplier/` 200，未混用内部认证 |
| S0-07 | 通过 | 五服务 Healthy、Nginx 三层语法、端口收敛、命名卷持久化均实测 |
| S0-08 | 通过 | Docker/安全/迁移/全量脚本及 Flyway P1 复验均有实际证据 |

## 8. 问题分级

- P0：无。
- P1：无。首次专项发现的 Flyway 10.20.1 兼容性告警已由 11.15.0 与真实 MySQL 8.4.10
  复验关闭。
- P2：internal-web 主包超过 500KB，不影响阶段0功能和本次验收。官方 MySQL 镜像的
  自签名 CA 及 pid-file 目录提示属于镜像运行信息，不列为阶段0遗留缺陷。

验收中已关闭的问题：后端镜像在线安装 curl、Maven 无关依赖全量预取、本地 backend 绑定
所有地址、Alpine Web 健康检查使用 `localhost` 导致 IPv6 误判。

## 9. Git 与文件保护

仓库为 `No commits yet on main`，因此 `git diff` 为空，`git status --short` 将整个工程显示为
未跟踪；这不是“无修改”的证据。本次永久修改文件见开发报告和文件清单。未执行 Git add、
commit、push、reset、checkout 或任何远程操作。

## 10. 架构优化后的 Docker 回归补充

2026-07-24 在 Spring JDBC 全量迁移 MyBatis-Plus、加入 Caffeine 和 OpenAPI 契约生成后，
使用新的隔离项目 `srm-stage0-architecture-acceptance` 再次执行本报告的关键验收：

- 五服务全部 Healthy，MySQL 仍为 8.4.10，空库 V1～V3 迁移及 10 张系统表通过。
- backend 实际日志出现 MyBatis-Plus ApplicationContext 初始化；Spring Boot 仍为 3.4.1。
- backend 重启为 version 3 / no migration，Flyway 兼容性警告为 0。
- MySQL 容器保留卷重建前后唯一测试记录为 1/1，精确清理后为 0。
- 三个 Nginx `nginx -t`、双前端、API 代理、登录、菜单、工作台、JWT 与 RBAC 均通过。
- 另以 `srm-stage0-architecture-prodcheck` 启动生产 Compose 五服务，全部 Healthy；
  仅 Nginx 发布宿主端口，生产网络内部 internal/supplier API docs 与 Swagger UI 均 404。
- 两个隔离项目容器/网络与临时卷验收后精确清理，项目镜像和既有正式验收卷保留。

详细命令、依赖版本和三项优化证据见《阶段0架构优化与独立复验报告》。该回归没有改变
本报告 P0/P1 为 0 的结论。
