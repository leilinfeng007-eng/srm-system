# 阶段0 Flyway P1 整改复验报告

复验日期：2026-07-24（Asia/Shanghai）  
复验平台：Apple Silicon Mac，Docker Engine 29.6.2（linux/arm64），Docker Compose 5.3.1  
复验范围：Flyway / MySQL 8.4 兼容性整改，以及受影响的阶段0 Docker 专项回归

## 1. 结论

阶段0 Docker 专项遗留的 Flyway/MySQL 兼容性 P1 已清零。项目在不升级 Java、Spring Boot
或 MySQL、不改写 V1～V3 迁移的前提下，将 `flyway-core` 与 `flyway-mysql` 从 Spring Boot
3.4.1 依赖管理的 10.20.1 显式对齐为 11.15.0。

Flyway 11.15.0 是逐版本核验后首个把 MySQL 已测试上限从 8.1 提升至 9.4 的稳定版本；其
class 文件版本为 61，可由当前 Java 17 运行。真实 MySQL 8.4.10 隔离空库完成 V1～V3，
重启 backend 未重复迁移，重建 MySQL 容器并保留命名卷后迁移历史与验收临时数据均保留。
首次启动与重启日志中的原兼容性警告扫描结果均为 0。

本次回归中五服务、Nginx 双前端与 API 代理、登录、JWT 刷新/退出、动态菜单和 viewer
越权 403 均通过。当前无 P0、P1；仅保留既有 Vite 分包警告 P2。阶段0可以正式验收，并已
具备首次 Git 基线提交条件，但本次未执行 Git add、commit、push 或远程操作。

## 2. 原因与版本选择

### 2.1 整改前依赖来源

- Java：Eclipse Temurin 17.0.19。
- Maven：IntelliJ IDEA 内置 Maven 3.9.11。
- Spring Boot Parent：3.4.1。
- `backend/pom.xml` 整改前未声明 `flyway.version`。
- Maven `help:evaluate -Dexpression=flyway.version` 返回 `10.20.1`，证明版本来自 Spring
  Boot 3.4.1 BOM。
- 整改前有效依赖树为：

```text
org.flywaydb:flyway-core:jar:10.20.1:compile
org.flywaydb:flyway-mysql:jar:10.20.1:compile
\- org.flywaydb:flyway-core:jar:10.20.1:compile (version managed from 10.20.1)
com.mysql:mysql-connector-j:jar:9.1.0:runtime
```

10.20.1 在 MySQL 8.4.10 启动时提示实际数据库版本高于其已测试范围 8.1。该提示不能通过
过滤日志、降低日志级别、修改 Flyway 源码或降级 MySQL 处理。

### 2.2 最小稳定版本判定

使用 Maven Central 正式制品逐个检查 `flyway-mysql` 的
`MySQLDatabase.ensureSupported()`：

| 版本范围/代表版本 | 已测试上限 | 判定 |
| --- | --- | --- |
| 10.20.1、10.21.0、10.22.0 | MySQL 8.1 | 不满足 |
| 11.0.0～11.14.1 | MySQL 8.1 | 不满足 |
| 11.15.0 | MySQL 9.4 | 首个满足 |

选择 11.15.0，而不是盲目采用最新版本。Maven Central 中
`flyway-mysql:11.15.0` 依赖同一 parent version 的 `flyway-core`；项目通过同一个
`flyway.version` 属性同时管理二者。`flyway-core:11.15.0` class major version 为 61，
与 Java 17 兼容；Spring Boot 3.4.1 的测试、打包和实际应用启动均通过，因此无需升级 Java
或 Spring Boot。版本发布和制品依据见
[Redgate Flyway Engine release notes](https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine)
与 [Maven Central flyway-mysql metadata](https://repo.maven.apache.org/maven2/org/flywaydb/flyway-mysql/maven-metadata.xml)。

## 3. 修改文件

| 文件 | 修改 |
| --- | --- |
| `backend/pom.xml` | 显式固定 `flyway.version=11.15.0`，确保 core/mysql 同版 |
| `scripts/check-migrations.sh` | 增加版本锁定静态门禁，保留 V1～V3、10 表和 H2 版本检查 |
| `scripts/generate-file-inventory.mjs` | 增加本次 P1 整改文件分组 |
| `docs/04-接口与数据设计/数据库规范.md` | 记录运行版本对齐和升级验证规则 |
| `docs/05-测试验收与运维/构建与恢复说明.md` | 增加有效依赖树与真实库复验要求 |
| `docs/05-测试验收与运维/阶段0Docker专项验收报告.md` | 增加 P1 关闭补充结论 |
| `docs/05-测试验收与运维/阶段0开发报告.md` | 更新实际版本、复验结果和遗留项 |
| `docs/05-测试验收与运维/阶段0验收记录.md` | 更新 S0-03/S0-08、问题级别和验收结论 |
| `docs/05-测试验收与运维/阶段0文件清单.md` | 由脚本按最终工作区重新生成 |
| `docs/05-测试验收与运维/阶段0FlywayP1整改复验报告.md` | 新增本报告 |

未修改 V1、V2、V3 的文件内容、名称、顺序或校验和。整改前后 SHA-256 均为：

| 迁移 | SHA-256 |
| --- | --- |
| V1 | `7b90828bdba322e3f659184155e2bd18bbd943d8d7283696e9fc9ba6ce5e76c3` |
| V2 | `377116eba8f6316ec04a73ffada8b5aae1f0fafdb134fad94558f77e525b48e4` |
| V3 | `01541e0d839f926ae2f2b26479dcf38dc012c01e363abf375483cadc3aa011f3` |

## 4. Maven 依赖与全量构建

实际执行：

```bash
./scripts/maven-command.sh -Dmaven.repo.local=/private/tmp/m2-idea-cache \
  -f backend/pom.xml -B dependency:tree \
  -Dincludes=org.flywaydb:flyway-core,org.flywaydb:flyway-mysql,com.mysql:mysql-connector-j
./scripts/maven-command.sh -Dmaven.repo.local=/private/tmp/m2-idea-cache \
  -f backend/pom.xml -B clean test
./scripts/maven-command.sh -Dmaven.repo.local=/private/tmp/m2-idea-cache \
  -f backend/pom.xml -B -DskipTests package
```

整改后有效依赖树：

```text
org.flywaydb:flyway-core:jar:11.15.0:compile
org.flywaydb:flyway-mysql:jar:11.15.0:compile
\- org.flywaydb:flyway-core:jar:11.15.0:compile (version managed from 11.15.0)
com.mysql:mysql-connector-j:jar:9.1.0:runtime
```

结果：后端 18/18 测试通过，0 failure/error/skipped；打包 `BUILD SUCCESS`。从实际运行
backend 容器复制应用 JAR 并检查，包含：

```text
BOOT-INF/lib/flyway-core-11.15.0.jar
BOOT-INF/lib/flyway-mysql-11.15.0.jar
BOOT-INF/lib/mysql-connector-j-9.1.0.jar
```

首次使用用户 Maven 缓存的只读依赖检查因沙箱不能写入 `~/.m2` 失败，随后改用隔离的
`/private/tmp/m2-idea-cache`；首次受限网络下载发生 DNS 失败，批准访问 Maven Central 后
成功。这两次属于工具环境失败，不是项目构建失败。

## 5. 隔离空库、迁移与兼容性日志

使用独立 Compose project `srm-stage0-flyway-p1-retest`、独立临时命名卷和隔离端口构建并
启动 mysql、backend、internal-web、supplier-web、nginx 五服务：

```bash
BUILDKIT_PROGRESS=plain docker compose \
  --env-file /private/tmp/srm-stage0-flyway-p1.env \
  up --build --detach --wait --wait-timeout 240
docker compose --env-file /private/tmp/srm-stage0-flyway-p1.env ps
docker compose --env-file /private/tmp/srm-stage0-flyway-p1.env logs backend
```

空库首次启动日志和查询证据：

- 实际数据库：MySQL 8.4.10。
- Flyway：11.15.0，成功校验 3 个迁移。
- 创建 `flyway_schema_history` 后依次执行 V1、V2、V3。
- 日志为 `Successfully applied 3 migrations ... now at version v3`。
- 10 张 `sys_*` 平台表，加 1 张 Flyway 历史表。
- 历史表三条记录均 `success=1`：

| installed_rank | version | checksum | success |
| ---: | ---: | ---: | ---: |
| 1 | 1 | 900759506 | 1 |
| 2 | 2 | 506332707 | 1 |
| 3 | 3 | 2076615535 | 1 |

能够证明真实数据库与迁移过程的原始日志关键行已固化如下（容器日志为 UTC）：

```text
2026-07-24T02:06:47.265Z ... FlywayExecutor : Database: jdbc:mysql://mysql:3306/srm_flyway_p1_retest ... (MySQL 8.4)
2026-07-24T02:06:47.312Z ... DbValidate     : Successfully validated 3 migrations
2026-07-24T02:06:47.381Z ... DbMigrate      : Current version ... << Empty Schema >>
2026-07-24T02:06:47.399Z ... DbMigrate      : Migrating schema ... version "1 - create system baseline"
2026-07-24T02:06:47.514Z ... DbMigrate      : Migrating schema ... version "2 - seed security roles"
2026-07-24T02:06:47.565Z ... DbMigrate      : Migrating schema ... version "3 - seed menu permissions"
2026-07-24T02:06:47.641Z ... DbMigrate      : Successfully applied 3 migrations ... now at version v3
```

同一运行实例的 MySQL CLI 查询保存结果：

```text
SELECT VERSION();
8.4.10

SELECT installed_rank, version, checksum, success
FROM flyway_schema_history ORDER BY installed_rank;
1  1  900759506   1
2  2  506332707   1
3  3  2076615535  1
```

对首次启动和 backend 重启日志扫描以下原警告特征：

```text
Flyway upgrade recommended
newer than this version of Flyway
support has not been tested
Unsupported Database
upgrade.*Flyway
```

两次扫描均为 0。未过滤日志、未降低日志级别，也未修改 Flyway 源码。

## 6. 重复启动与数据卷持久化

重启 backend 后容器恢复 Healthy，日志为：

```text
Successfully validated 3 migrations
Current version of schema `srm_flyway_p1_retest`: 3
Schema `srm_flyway_p1_retest` is up to date. No migration necessary.
```

日志不存在 `Migrating schema`，迁移历史仍为三条。

第一次重启后的轮询脚本使用了 zsh 只读变量名 `status`，脚本自身报错但 backend 容器实际
正常；将局部变量改为 `health_state` 后重新执行完整重启验证，得到上述 Healthy、版本 3、
无重复迁移和警告为 0 的结果。该失败属于验收命令书写问题，不是应用或容器失败。

随后在 `sys_operation_log` 写入唯一 traceId
`S0-FLYWAY-P1-RETEST-20260724` 的验收记录，重建 MySQL 容器但保留命名卷：

```bash
docker compose --env-file /private/tmp/srm-stage0-flyway-p1.env \
  up -d --force-recreate mysql
```

重建前后该记录数量为 1/1；MySQL 仍为 8.4.10，三条迁移和校验和不变。验收结束时按
traceId 精确删除，复核数量为 0。

## 7. 五服务与业务关键回归

| 服务 | 状态 | 宿主端口 |
| --- | --- | --- |
| mysql | Healthy | 无 |
| backend | Healthy | 仅 `127.0.0.1:18180` |
| internal-web | Healthy | 无 |
| supplier-web | Healthy | 无 |
| nginx | Healthy | `18188` |

三个 Nginx 实例 `nginx -t` 均为 syntax ok / test successful。HTTP 与认证回归：

| 用例 | 结果 |
| --- | --- |
| 内部端 `/` | HTTP 200 |
| 供应商端 `/supplier/` | HTTP 200 |
| Nginx `/healthz` | HTTP 200 |
| `/api` 代理 readiness | HTTP 200，UP |
| 未登录 `/api/v1/auth/me` | HTTP 401 |
| 管理员登录/动态菜单 | 12 个一级域、86 个启用页面 |
| 模块/工作台 | 12 个模块、workbench=READY |
| JWT 刷新/退出 | rotation、revocation 通过 |
| viewer RBAC | 2 个一级域，越权 HTTP 403 |

`node scripts/live-smoke.mjs` 输出
`refreshRotation=passed, logoutRevocation=passed, viewerForbidden=403`。

## 8. 全量门禁

```bash
MAVEN_OPTS='-Dmaven.repo.local=/private/tmp/m2-idea-cache' ./scripts/check.sh
```

退出码 0，实际包含：

- 后端 18/18 测试及可执行 JAR 打包通过。
- `npm ci` 安装 199 个包，0 vulnerabilities。
- 双前端及共享包 lint、TypeScript 检查通过。
- 前端/清单自动化测试 5/5 通过。
- internal-web、supplier-web 生产构建通过。
- 12 个一级域、85 个二级占位页、86 个启用路由、1 个隐藏预留、9 个供应商入口通过。
- Flyway 11.15.0、V1～V3、10 张系统表、H2 2.2.224 静态检查通过。
- 本地/生产 Compose、Nginx、部署与生产秘密约束检查通过。
- 安全扫描未发现真实秘密或本地 `.env`。
- 最终工作区安全扫描统计为 285 个文本文件。

internal-web 主 JavaScript 包约 1.06 MB 的 Vite 500KB 警告仍存在，构建成功，按既定范围
保留为后续性能优化 P2。

## 9. 清理、资源保护与 Git

复验结束后执行隔离项目 `docker compose down`，本次容器和项目网络均停止并移除；临时
空库测试卷经确认后精确删除，临时环境文件删除。未执行 `down -v` 作用于正式项目卷，
未执行 `docker system prune`，未触碰无关 Docker 资源。

保留本次构建的三个项目应用镜像，以及此前正式专项验收卷
`srm-stage0-docker-acceptance_mysql-data`，便于用户复查。

最终只读资源复核：

| 资源 | 状态 |
| --- | --- |
| `srm-stage0-flyway-p1-retest` 容器 | 0 |
| `srm-stage0-flyway-p1-retest` 网络 | 0 |
| 临时 `srm-stage0-flyway-p1-retest_mysql-data` | 已精确删除 |
| 正式 `srm-stage0-docker-acceptance_mysql-data` | 保留 |
| P1 backend 镜像 | `32824993a19c`，保留 |
| P1 internal-web 镜像 | `896e43d7ff8f`，保留 |
| P1 supplier-web 镜像 | `840e291f7898`，保留 |

Git 仍为 `No commits yet on main`，全工程显示未跟踪，`git diff` 因无已跟踪基线而为空。
本次未执行 add、commit、push、reset、checkout 或远程操作。

## 10. P0 / P1 / P2 与验收判定

- P0：无。
- P1：无。原 Flyway 10.20.1 / MySQL 8.4.10 兼容性问题已由 11.15.0 和真实库复验关闭。
- P2：internal-web 生产主包超过 500KB；不影响阶段0功能、构建或部署验收，后续性能阶段
  负责。

最终判定：S0-01～S0-08 均可正式验收；阶段0具备首次 Git 基线提交条件。本报告不构成
提交授权，本次未进行任何 Git 提交。

## 11. 架构优化后的 P1 再复核

2026-07-24 数据访问层迁移 MyBatis-Plus 后，又以新的隔离空卷运行 MySQL 8.4.10 和
Flyway 11.15.0：

- V1～V3 再次依序成功，checksum 仍为 `900759506`、`506332707`、`2076615535`。
- backend 重启日志为 schema version 3 / no migration。
- 首启和重启日志的兼容性警告模式计数仍为 0。
- MySQL 容器保留卷重建后迁移历史和唯一验收记录均保留，临时记录随后精确清理。
- 新增 MyBatis-Plus/Caffeine 后 Maven 22/22、五服务、认证/RBAC 与 Nginx 回归通过。
- V1～V3 文件 SHA-256 未改变。

因此 Flyway P1 继续保持关闭；详细证据见《阶段0架构优化与独立复验报告》。
