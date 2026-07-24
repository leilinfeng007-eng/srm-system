# 数据库基线

数据库结构由后端 Flyway 单一管理，迁移位于 `backend/src/main/resources/db/migration`：

- `V1__create_system_baseline.sql`：10 张平台最小系统表；
- `V2__seed_security_roles.sql`：`SUPER_ADMIN` 与 `SKELETON_VIEWER`；
- `V3__seed_menu_permissions.sql`：12 个一级域、85 个二级占位页、工作台首页、隐藏预留及角色授权。

阶段 0 不创建任何业务表。执行 `scripts/check-migrations.sh` 可做离线结构和菜单一致性检查；空 MySQL 验证由 `docker compose up` 触发，Flyway 负责初始化。重复启动由 Flyway 历史表保证不会重复执行版本迁移。

开发数据重置会删除本地命名卷，属于破坏性操作，不封装进自动脚本。确需重置时先停止服务并确认无须保留的数据，再由操作者显式执行 `docker compose down --volumes`，之后重新启动。
