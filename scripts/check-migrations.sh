#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
migration_dir="$root_dir/backend/src/main/resources/db/migration"

expected="V1__create_system_baseline.sql V2__seed_security_roles.sql V3__seed_menu_permissions.sql"
for migration in $expected; do
  [[ -s "$migration_dir/$migration" ]] || { echo "迁移缺失或为空: $migration" >&2; exit 1; }
done

table_count="$(rg -c '^CREATE TABLE sys_' "$migration_dir/V1__create_system_baseline.sql")"
[[ "$table_count" == "10" ]] || { echo "系统表应为10张，实际$table_count" >&2; exit 1; }

rg -q '<h2.version>2\.2\.224</h2.version>' "$root_dir/backend/pom.xml" \
  || { echo "H2测试版本必须固定为Flyway明确支持的2.2.224" >&2; exit 1; }

rg -q '<flyway.version>11\.15\.0</flyway.version>' "$root_dir/backend/pom.xml" \
  || { echo "Flyway必须固定为首个覆盖MySQL 8.4的稳定版本11.15.0" >&2; exit 1; }

node "$root_dir/scripts/validate-skeleton.mjs"
echo "Flyway迁移文件静态检查通过（Flyway 11.15.0、3个迁移版本、10张系统表、H2 2.2.224）。"
