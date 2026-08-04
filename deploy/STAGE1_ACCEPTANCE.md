# SRM V1.0 正式部署时运行验收参考（原阶段1隔离验收手册）

> **当前停止执行：** 阶段1临时 `/opt/srm-stage1-acceptance` 验收环境已取消，不再复制环境
> 样例、不再配置五项临时秘密，也不再执行本文任何服务器命令。本文只保留为历史安全约束
> 与正式部署变更窗口参考；届时必须重新评审目标、备份、回滚和审批后另行执行。

阶段1源码、自动化门禁、GitHub Actions、MySQL 8.4 空库 V1→V12 及三个
`linux/amd64` 业务镜像构建推送已通过，结论为“阶段1源码与CI验收通过”。服务器部署、
现有数据库升级、备份恢复和运行验证尚未执行，不计为已通过，但不再阻断阶段1代码合并和
阶段2任务书编制。任何正式部署操作都不得未经审批操作生产容器、生产数据库卷或
`/opt/srm-system` 现有正式数据。

五个运行服务是 MySQL、backend、internal-web、supplier-web、nginx。只有后三个 SRM
业务镜像由 GitHub Actions 构建并推送 TCR；MySQL 固定 `mysql:8.4`，网关固定
`nginx:1.27-alpine`。

## 0. 不可变输入与首轮只读预检

将报告中的完整候选 SHA 填入第一行。所有源码和证据仅放在
`/opt/srm-stage1-acceptance`，不得在 `/opt/srm-system` 下执行任何命令。

```bash
set -euo pipefail
export SRM_CANDIDATE_SHA='CANDIDATE_FULL_SHA_FROM_REPORT'
export SRM_CANDIDATE_BRANCH='agent/configure-tcr-release'
export SRM_ACCEPTANCE_ROOT='/opt/srm-stage1-acceptance'
export SRM_ACCEPTANCE_PROJECT='srm-stage1-acceptance'
export SRM_ACCEPTANCE_ENV='deploy/.env.stage1-acceptance'
export SRM_ACCEPTANCE_COMPOSE='deploy/docker-compose.prod.yml'

[[ "$SRM_CANDIDATE_SHA" =~ ^[0-9a-f]{40}$ ]]
[[ "$SRM_ACCEPTANCE_ROOT" == '/opt/srm-stage1-acceptance' ]]
[[ "$SRM_ACCEPTANCE_ROOT" != '/opt/srm-system' ]]
[[ "$SRM_ACCEPTANCE_PROJECT" == 'srm-stage1-acceptance' ]]
command -v git
command -v docker
command -v curl
command -v node
command -v sha256sum
command -v jq
docker compose version
docker compose ls
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
docker volume ls
```

上一步只盘点，不停止或删除任何对象。确认服务器资源充足后，在独立目录检出候选源码：

```bash
set -euo pipefail
sudo install -d -m 0750 -o "$(id -un)" -g "$(id -gn)" "$SRM_ACCEPTANCE_ROOT"
cd "$SRM_ACCEPTANCE_ROOT"
if [[ ! -d .git ]]; then
  git clone --no-checkout https://github.com/leilinfeng007-eng/srm-system.git .
fi
git fetch origin "$SRM_CANDIDATE_BRANCH"
git checkout --detach "$SRM_CANDIDATE_SHA"
[[ "$(git rev-parse HEAD)" == "$SRM_CANDIDATE_SHA" ]]
[[ "$(pwd -P)" == '/opt/srm-stage1-acceptance' ]]
git status --short
mkdir -p evidence
git show --no-patch --format='commit=%H%ncommitted_at=%cI%nsubject=%s' HEAD | tee evidence/candidate.txt
```

## 1. 准备独立环境文件

样例文件没有秘密且无法直接启动。复制后只在服务器本机填写秘密，不要把内容贴到聊天、
日志或验收报告。JWT 至少 32 字节；数据库和初始账号密码应满足入口脚本强度要求。

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
[[ "$(git rev-parse HEAD)" == "$SRM_CANDIDATE_SHA" ]]
install -m 0600 deploy/stage1-acceptance.env.example "$SRM_ACCEPTANCE_ENV"
short_sha="${SRM_CANDIDATE_SHA:0:12}"
sed -i "s/^SRM_IMAGE_TAG=.*/SRM_IMAGE_TAG=sha-${short_sha}/" "$SRM_ACCEPTANCE_ENV"
${EDITOR:-vi} "$SRM_ACCEPTANCE_ENV"
chmod 0600 "$SRM_ACCEPTANCE_ENV"

env_value() { sed -n "s/^${1}=//p" "$SRM_ACCEPTANCE_ENV" | tail -n 1; }
[[ "$(env_value COMPOSE_PROJECT_NAME)" == 'srm-stage1-acceptance' ]]
[[ "$(env_value SRM_GATEWAY_PORT)" == '18088' ]]
[[ "$(env_value SRM_MYSQL_VOLUME_NAME)" == 'srm-stage1-acceptance-mysql-data' ]]
[[ "$(env_value SRM_ATTACHMENT_VOLUME_NAME)" == 'srm-stage1-acceptance-attachments' ]]
[[ "$(env_value SRM_DB_NAME)" == 'srm_stage1_acceptance' ]]
[[ "$(env_value SRM_IMAGE_TAG)" == "sha-${short_sha}" ]]
for key in SRM_DB_PASSWORD SRM_DB_ROOT_PASSWORD SRM_JWT_SECRET \
  SRM_BOOTSTRAP_ADMIN_PASSWORD SRM_BOOTSTRAP_VIEWER_PASSWORD; do
  [[ -n "$(env_value "$key")" ]]
done
if grep -E '=(CHANGE_ME|REPLACE_WITH|EXAMPLE_PASSWORD|PLACEHOLDER)' "$SRM_ACCEPTANCE_ENV" >/dev/null; then
  echo '验收环境仍包含占位秘密，拒绝继续。' >&2
  exit 1
fi
```

环境文件只允许首次创建一个全新的验收项目。若以下检查发现同名容器或卷，立即停止并人工
确认来源，不要自动清理：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
[[ -z "$(docker ps -aq --filter 'label=com.docker.compose.project=srm-stage1-acceptance')" ]]
if docker volume inspect srm-stage1-acceptance-mysql-data >/dev/null 2>&1; then
  echo '验收MySQL卷已存在；首次空库验收拒绝复用。' >&2
  exit 1
fi
if docker volume inspect srm-stage1-acceptance-attachments >/dev/null 2>&1; then
  echo '验收附件卷已存在；首次空库验收拒绝复用。' >&2
  exit 1
fi
```

## 2. 登录 TCR、拉取并核对三个业务镜像

TCR 密码用标准输入传给 Docker，不写入命令历史。不要回传用户名或密码值。

```bash
set -euo pipefail
read -r -p 'TCR username: ' TCR_USERNAME
read -r -s -p 'TCR password: ' TCR_PASSWORD
printf '\n'
printf '%s' "$TCR_PASSWORD" | docker login ccr.ccs.tencentyun.com --username "$TCR_USERNAME" --password-stdin
unset TCR_PASSWORD TCR_USERNAME

cd /opt/srm-stage1-acceptance
ACC=(docker compose -p srm-stage1-acceptance --env-file "$SRM_ACCEPTANCE_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
"${ACC[@]}" config --quiet
"${ACC[@]}" config --images | tee evidence/compose-images.txt
"${ACC[@]}" pull

tag="sha-${SRM_CANDIDATE_SHA:0:12}"
for repo in srm-backend srm-internal-web srm-supplier-web; do
  image="ccr.ccs.tencentyun.com/leizi114/${repo}:${tag}"
  [[ "$(docker image inspect "$image" --format '{{.Architecture}}')" == 'amd64' ]]
  docker image inspect "$image" --format '{{range .RepoDigests}}{{println .}}{{end}}' \
    | tee "evidence/${repo}-digests.txt"
done
```

`evidence/compose-images.txt` 应正好列出五个运行镜像：三个 `sha-<短SHA>` 业务镜像、
`mysql:8.4`、`nginx:1.27-alpine`。

## 3. 五服务、空库 V1→V12 与生产安全开关

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
ACC=(docker compose -p srm-stage1-acceptance --env-file "$SRM_ACCEPTANCE_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
"${ACC[@]}" up -d --wait
"${ACC[@]}" ps | tee evidence/compose-ps-initial.txt

mapfile -t services < <("${ACC[@]}" ps --status running --services | sort)
[[ "${#services[@]}" -eq 5 ]]
[[ "${services[*]}" == 'backend internal-web mysql nginx supplier-web' ]]
[[ -n "$(docker network ls -q --filter name='^srm-stage1-acceptance_')" ]]
docker volume inspect srm-stage1-acceptance-mysql-data >/dev/null
docker volume inspect srm-stage1-acceptance-attachments >/dev/null

curl --fail --silent --show-error http://127.0.0.1:18088/healthz
curl --fail --silent --show-error http://127.0.0.1:18088/actuator/health/readiness | tee evidence/readiness.json
[[ "$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:18088/)" == '200' ]]
[[ "$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:18088/supplier/)" == '200' ]]

"${ACC[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT VERSION();"' \
  | tee evidence/mysql-version.txt
versions="$("${ACC[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version FROM flyway_schema_history WHERE type=\"SQL\" AND success=1 ORDER BY installed_rank;"' \
  | paste -sd, -)"
[[ "$versions" == '1,2,3,4,5,6,7,8,9,10,11,12' ]]
printf 'flyway_versions=%s\n' "$versions" | tee evidence/flyway-empty-db.txt
```

再执行登录、RBAC、18 个开放页面菜单、8 个关键 API、令牌刷新/吊销、Secure Cookie 和
生产 OpenAPI 404 联调。密码仅交互输入：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
read -r -p 'Acceptance admin username: ' SRM_SMOKE_ADMIN_USERNAME
read -r -s -p 'Acceptance admin password: ' SRM_SMOKE_ADMIN_PASSWORD; printf '\n'
read -r -p 'Acceptance viewer username: ' SRM_SMOKE_VIEWER_USERNAME
read -r -s -p 'Acceptance viewer password: ' SRM_SMOKE_VIEWER_PASSWORD; printf '\n'
export SRM_SMOKE_BASE_URL='http://127.0.0.1:18088'
export SRM_SMOKE_ADMIN_USERNAME SRM_SMOKE_ADMIN_PASSWORD
export SRM_SMOKE_VIEWER_USERNAME SRM_SMOKE_VIEWER_PASSWORD
node scripts/live-smoke.mjs | tee evidence/live-smoke.txt
unset SRM_SMOKE_ADMIN_PASSWORD SRM_SMOKE_VIEWER_PASSWORD
```

## 4. 整套重启、卷持久化与无待执行迁移

先记录关键数据数量，再只执行不带 `-v` 的验收项目停止和重启。此步骤不得调用
`docker compose down -v`。

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
ACC=(docker compose -p srm-stage1-acceptance --env-file "$SRM_ACCEPTANCE_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
count_sql='SELECT "sys_user",COUNT(*) FROM sys_user UNION ALL SELECT "sys_role",COUNT(*) FROM sys_role UNION ALL SELECT "sys_permission",COUNT(*) FROM sys_permission UNION ALL SELECT "sys_menu",COUNT(*) FROM sys_menu UNION ALL SELECT "md_organization",COUNT(*) FROM md_organization UNION ALL SELECT "md_material",COUNT(*) FROM md_material UNION ALL SELECT "sys_operation_log",COUNT(*) FROM sys_operation_log ORDER BY 1;'
"${ACC[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "$1"' sh "$count_sql" \
  | tee evidence/counts-before-restart.txt

"${ACC[@]}" down
[[ -z "$(docker ps -q --filter 'label=com.docker.compose.project=srm-stage1-acceptance')" ]]
docker volume inspect srm-stage1-acceptance-mysql-data >/dev/null
"${ACC[@]}" up -d --wait
"${ACC[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "$1"' sh "$count_sql" \
  | tee evidence/counts-after-restart.txt
diff -u evidence/counts-before-restart.txt evidence/counts-after-restart.txt

versions="$("${ACC[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version FROM flyway_schema_history WHERE type=\"SQL\" AND success=1 ORDER BY installed_rank;"' \
  | paste -sd, -)"
[[ "$versions" == '1,2,3,4,5,6,7,8,9,10,11,12' ]]
"${ACC[@]}" logs --no-color backend | tail -n 300 > evidence/backend-after-restart.log
curl --fail --silent --show-error http://127.0.0.1:18088/actuator/health/readiness
```

## 5. 阶段0 V3 隔离基线升级到 V12

本场景用另一个 Compose 项目、端口、数据库和卷生成 V3 基线，不读取、停止或复制生产库。
先从同一受限环境文件复制秘密，再只改非秘密隔离标识：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
UPGRADE_PROJECT='srm-stage1-acceptance-upgrade-v3'
UPGRADE_ENV='deploy/.env.stage1-acceptance-upgrade-v3'
UPGRADE_OVERRIDE='deploy/.stage1-acceptance-v3.override.yml'
cp "$SRM_ACCEPTANCE_ENV" "$UPGRADE_ENV"
chmod 0600 "$UPGRADE_ENV"
sed -i \
  -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=${UPGRADE_PROJECT}/" \
  -e 's/^SRM_GATEWAY_PORT=.*/SRM_GATEWAY_PORT=18090/' \
  -e "s/^SRM_MYSQL_VOLUME_NAME=.*/SRM_MYSQL_VOLUME_NAME=${UPGRADE_PROJECT}-mysql-data/" \
  -e "s/^SRM_ATTACHMENT_VOLUME_NAME=.*/SRM_ATTACHMENT_VOLUME_NAME=${UPGRADE_PROJECT}-attachments/" \
  -e 's/^SRM_DB_NAME=.*/SRM_DB_NAME=srm_stage1_upgrade_v3/' \
  "$UPGRADE_ENV"
cat > "$UPGRADE_OVERRIDE" <<'YAML'
services:
  backend:
    environment:
      SPRING_FLYWAY_TARGET: "3"
YAML
[[ -z "$(docker ps -aq --filter "label=com.docker.compose.project=${UPGRADE_PROJECT}")" ]]
if docker volume inspect "${UPGRADE_PROJECT}-mysql-data" >/dev/null 2>&1; then
  echo 'V3升级隔离卷已存在，拒绝复用。' >&2
  exit 1
fi

V3=(docker compose -p "$UPGRADE_PROJECT" --env-file "$UPGRADE_ENV" \
  -f "$SRM_ACCEPTANCE_COMPOSE" -f "$UPGRADE_OVERRIDE")
"${V3[@]}" up -d mysql backend
for _ in $(seq 1 90); do
  versions="$("${V3[@]}" exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version FROM flyway_schema_history WHERE type=\"SQL\" AND success=1 ORDER BY installed_rank;"' \
    2>/dev/null | paste -sd, - || true)"
  [[ "$versions" == '1,2,3' ]] && break
  sleep 2
done
[[ "$versions" == '1,2,3' ]]
"${V3[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version,checksum FROM flyway_schema_history WHERE type=\"SQL\" ORDER BY installed_rank; SELECT COUNT(*) FROM sys_menu; SELECT COUNT(*) FROM sys_menu WHERE menu_code=\"MENU_WORKBENCH_HOME\";"' \
  | tee evidence/v3-baseline.txt
"${V3[@]}" stop backend

UP=(docker compose -p "$UPGRADE_PROJECT" --env-file "$UPGRADE_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
"${UP[@]}" up -d --wait
versions="$("${UP[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version FROM flyway_schema_history WHERE type=\"SQL\" AND success=1 ORDER BY installed_rank;"' \
  | paste -sd, -)"
[[ "$versions" == '1,2,3,4,5,6,7,8,9,10,11,12' ]]
"${UP[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "SELECT version,checksum FROM flyway_schema_history WHERE type=\"SQL\" ORDER BY installed_rank; SELECT COUNT(*) FROM sys_menu; SELECT COUNT(*) FROM sys_menu WHERE menu_code=\"MENU_WORKBENCH_HOME\";"' \
  | tee evidence/v12-after-v3-upgrade.txt
"${UP[@]}" ps | tee evidence/v3-upgrade-compose-ps.txt
```

若 V3 后端因阶段1代码访问尚不存在的表而暂时重启，只要 V1–V3 已完整成功写入且随后停止
该容器，这是预期的基线制作过程；V4–V12 完成后五服务必须全部健康。不得执行 Flyway
`repair`，不得改写 `flyway_schema_history`。

## 6. 真实备份、SHA-256、非空拒绝和隔离恢复

备份源只允许是 `srm-stage1-acceptance` 验收项目：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
[[ "$SRM_ACCEPTANCE_PROJECT" == 'srm-stage1-acceptance' ]]
./scripts/backup.sh \
  --project srm-stage1-acceptance \
  --compose-file "$SRM_ACCEPTANCE_COMPOSE" \
  --env-file "$SRM_ACCEPTANCE_ENV" \
  --output-dir evidence/backups \
  --db-name srm_stage1_acceptance
BACKUP_DIR="$(find evidence/backups -mindepth 1 -maxdepth 1 -type d -name 'srm-backup-*' | sort | tail -n 1)"
[[ -n "$BACKUP_DIR" && -f "$BACKUP_DIR/checksums.sha256" ]]
(cd "$BACKUP_DIR" && sha256sum -c checksums.sha256) | tee evidence/backup-sha256.txt
grep -E '^(TABLE_COUNT|ROLE_COUNT|USER_COUNT|FLYWAY_VERSION|ATTACHMENTS_INCLUDED)=' "$BACKUP_DIR/metadata.env" \
  | tee evidence/backup-counts.txt
```

先建立一个专用的非空恢复目标，确认脚本必须以退出码 6 拒绝。下列辅助函数只复制环境文件
并改写项目、端口、卷和数据库名，不输出秘密：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
make_restore_env() {
  local project="$1" port="$2" database="$3" target_env="$4"
  [[ "$project" == srm-stage1-acceptance-restore-* ]]
  cp "$SRM_ACCEPTANCE_ENV" "$target_env"
  chmod 0600 "$target_env"
  sed -i \
    -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=${project}/" \
    -e "s/^SRM_GATEWAY_PORT=.*/SRM_GATEWAY_PORT=${port}/" \
    -e "s/^SRM_MYSQL_VOLUME_NAME=.*/SRM_MYSQL_VOLUME_NAME=${project}-mysql-data/" \
    -e "s/^SRM_ATTACHMENT_VOLUME_NAME=.*/SRM_ATTACHMENT_VOLUME_NAME=${project}-attachments/" \
    -e "s/^SRM_DB_NAME=.*/SRM_DB_NAME=${database}/" \
    "$target_env"
}

REJECT_PROJECT='srm-stage1-acceptance-restore-reject'
REJECT_ENV='deploy/.env.stage1-acceptance-restore-reject'
make_restore_env "$REJECT_PROJECT" 18091 srm_stage1_restore_reject "$REJECT_ENV"
REJECT=(docker compose -p "$REJECT_PROJECT" --env-file "$REJECT_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
[[ -z "$(docker ps -aq --filter "label=com.docker.compose.project=${REJECT_PROJECT}")" ]]
if docker volume inspect "${REJECT_PROJECT}-mysql-data" >/dev/null 2>&1; then
  echo '非空拒绝测试卷已存在，拒绝复用。' >&2
  exit 1
fi
"${REJECT[@]}" up -d --wait mysql
"${REJECT[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE" -e "CREATE TABLE acceptance_nonempty_guard(id BIGINT PRIMARY KEY);"'
"${REJECT[@]}" down
set +e
./scripts/restore.sh --backup-dir "$BACKUP_DIR" --env-file "$REJECT_ENV" \
  --target-project "$REJECT_PROJECT" --compose-file "$SRM_ACCEPTANCE_COMPOSE" \
  --evidence-dir evidence/restore-reject
reject_code=$?
set -e
[[ "$reject_code" -eq 6 ]]
printf 'nonempty_refusal_exit=%s\n' "$reject_code" | tee evidence/nonempty-refusal.txt
[[ "$REJECT_PROJECT" == 'srm-stage1-acceptance-restore-reject' ]]
"${REJECT[@]}" down -v
```

最后恢复到另一个全新项目和全新卷，核对脚本生成的表、角色、用户、Flyway 和五服务证据：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
CLEAN_PROJECT='srm-stage1-acceptance-restore-clean'
CLEAN_ENV='deploy/.env.stage1-acceptance-restore-clean'
make_restore_env "$CLEAN_PROJECT" 18092 srm_stage1_restore_clean "$CLEAN_ENV"
[[ -z "$(docker ps -aq --filter "label=com.docker.compose.project=${CLEAN_PROJECT}")" ]]
if docker volume inspect "${CLEAN_PROJECT}-mysql-data" >/dev/null 2>&1; then
  echo '隔离恢复卷已存在，拒绝复用。' >&2
  exit 1
fi
./scripts/restore.sh --backup-dir "$BACKUP_DIR" --env-file "$CLEAN_ENV" \
  --target-project "$CLEAN_PROJECT" --compose-file "$SRM_ACCEPTANCE_COMPOSE" \
  --evidence-dir evidence/restore-clean
cat "evidence/restore-clean/${CLEAN_PROJECT}/status.env"
cat "evidence/restore-clean/${CLEAN_PROJECT}/validation.env"
CLEAN=(docker compose -p "$CLEAN_PROJECT" --env-file "$CLEAN_ENV" -f "$SRM_ACCEPTANCE_COMPOSE")
"${CLEAN[@]}" ps | tee evidence/restore-clean-compose-ps.txt
curl --fail --silent --show-error http://127.0.0.1:18092/actuator/health/readiness
```

## 7. 证据回传与受控清理

回传时只提供以下无秘密证据，不提供任何 `.env*` 内容：候选 SHA、Workflow Run URL、
三个镜像 tag/digest、Compose 服务状态、MySQL 版本、Flyway 版本序列、联调摘要、重启前后
数量 diff、V3→V12 记录、备份 SHA-256、非空拒绝退出码、恢复数量。

基础验收项目在结论确认前保持运行。若需要清理临时升级/恢复项目，必须逐个执行以下形式，
并把项目名与卷名保持为明确的验收专用字面量；禁止变量通配、`docker system prune`、
`docker volume prune` 或任何 `/opt/srm-system` 路径操作：

```bash
set -euo pipefail
cd /opt/srm-stage1-acceptance
[[ "$UPGRADE_PROJECT" == 'srm-stage1-acceptance-upgrade-v3' ]]
UP=(docker compose -p srm-stage1-acceptance-upgrade-v3 \
  --env-file deploy/.env.stage1-acceptance-upgrade-v3 -f deploy/docker-compose.prod.yml)
"${UP[@]}" down -v

[[ "$CLEAN_PROJECT" == 'srm-stage1-acceptance-restore-clean' ]]
CLEAN=(docker compose -p srm-stage1-acceptance-restore-clean \
  --env-file deploy/.env.stage1-acceptance-restore-clean -f deploy/docker-compose.prod.yml)
"${CLEAN[@]}" down -v
```

不得对 `srm-production`、任何生产容器或生产卷执行 `down`、`stop`、`rm`、`migrate`、
`restore` 或卷删除操作。
