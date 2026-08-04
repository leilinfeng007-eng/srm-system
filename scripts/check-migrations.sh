#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
migration_dir="$root_dir/backend/src/main/resources/db/migration"
latest_version=12

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'
  else shasum -a 256 "$1" | awk '{print $1}'; fi
}

for version in $(seq 1 "$latest_version"); do
  matches=("$migration_dir"/V"$version"__*.sql)
  [[ ${#matches[@]} -eq 1 && -s "${matches[0]}" ]] \
    || { echo "迁移V${version}缺失、重复或为空" >&2; exit 1; }
done

actual_count="$(find "$migration_dir" -maxdepth 1 -type f -name 'V*__*.sql' | wc -l | tr -d ' ')"
[[ "$actual_count" == "$latest_version" ]] \
  || { echo "迁移版本必须从V1连续到V${latest_version}，实际${actual_count}个文件" >&2; exit 1; }

declare -a released_files=(
  "V1__create_system_baseline.sql"
  "V2__seed_security_roles.sql"
  "V3__seed_menu_permissions.sql"
  "V6__stage1_workflow_task_message.sql"
  "V7__stage1_common_governance.sql"
)
declare -a released_hashes=(
  "7b90828bdba322e3f659184155e2bd18bbd943d8d7283696e9fc9ba6ce5e76c3"
  "377116eba8f6316ec04a73ffada8b5aae1f0fafdb134fad94558f77e525b48e4"
  "01541e0d839f926ae2f2b26479dcf38dc012c01e363abf375483cadc3aa011f3"
  "68df4d4e4c16bba35b4d1d833c04f7a9049416c7723e42f765cfab007160aa68"
  "eb75805fb45e4aff5db3ca00f8864fe7c55808d1eab29a9b0cd05ae9ea7b6de0"
)
for index in "${!released_files[@]}"; do
  actual="$(sha256_file "$migration_dir/${released_files[$index]}")"
  [[ "$actual" == "${released_hashes[$index]}" ]] \
    || { echo "已发布迁移被改写: ${released_files[$index]}" >&2; exit 1; }
done

system_table_count="$(grep -h -c '^CREATE TABLE sys_' "$migration_dir"/V*.sql | awk '{sum += $1} END {print sum}')"
masterdata_table_count="$(grep -h -c '^CREATE TABLE md_' "$migration_dir"/V*.sql | awk '{sum += $1} END {print sum}')"
[[ "$system_table_count" -gt 10 && "$masterdata_table_count" -gt 0 ]] \
  || { echo "阶段1系统表或主数据表迁移不完整" >&2; exit 1; }

grep -q 'uk_sys_doc_tmpl_code_ver UNIQUE (template_code, template_version)' \
  "$migration_dir/V7__stage1_common_governance.sql" \
  || { echo "模板版本唯一约束缺失" >&2; exit 1; }
grep -q "system:batch-job:retry" "$migration_dir/V11__stage1_batch_authorization.sql" \
  || { echo "批任务权限迁移缺失" >&2; exit 1; }
grep -q "system:message:manage" "$migration_dir/V12__stage1_message_action_authorization.sql" \
  || { echo "消息处理权限迁移缺失" >&2; exit 1; }

grep -q '<h2.version>2\.2\.224</h2.version>' "$root_dir/backend/pom.xml" \
  || { echo "H2测试版本必须固定为2.2.224" >&2; exit 1; }
grep -q '<flyway.version>11\.15\.0</flyway.version>' "$root_dir/backend/pom.xml" \
  || { echo "Flyway必须固定为11.15.0" >&2; exit 1; }

node "$root_dir/scripts/validate-skeleton.mjs"
echo "Flyway迁移静态检查通过（V1-V${latest_version}连续、V1-V3及V6-V7哈希受保护、系统表${system_table_count}、主数据表${masterdata_table_count}）。"
