#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/restore.sh --backup-dir DIR --env-file FILE [options]

Restores a verified SRM backup into an isolated Docker Compose project using
credentials from an existing external env file. No env file is generated.
The target project and both named volumes must use a restore-only name; a
production project or a mismatched env file is always rejected.

Options:
  --backup-dir DIR       Backup bundle (required)
  --env-file FILE        Existing external target env file (required)
  --target-project NAME  Isolated target project (default: timestamped)
  --compose-file FILE    Compose file (default: docker-compose.yml)
  --evidence-dir DIR     Restore logs/evidence root (default: ./restore-evidence)
  --no-attachments       Explicitly omit attachment restore
  --force-nonempty       Explicitly authorize restore into a non-empty schema

The command validates checksums and archives before starting MySQL. Without
--force-nonempty, any existing target table causes an immediate refusal.
EOF
}

BACKUP_DIR=""
ENV_FILE=""
TARGET_PROJECT=""
COMPOSE_FILE="docker-compose.yml"
EVIDENCE_ROOT="./restore-evidence"
SKIP_ATTACHMENTS=false
FORCE_NONEMPTY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backup-dir) BACKUP_DIR="${2:?missing backup directory}"; shift 2 ;;
    --env-file) ENV_FILE="${2:?missing env file}"; shift 2 ;;
    --target-project) TARGET_PROJECT="${2:?missing target project}"; shift 2 ;;
    --compose-file|--compose-template) COMPOSE_FILE="${2:?missing compose file}"; shift 2 ;;
    --evidence-dir) EVIDENCE_ROOT="${2:?missing evidence directory}"; shift 2 ;;
    --no-attachments) SKIP_ATTACHMENTS=true; shift ;;
    --force-nonempty|--force) FORCE_NONEMPTY=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "ERROR: Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

[[ -n "$BACKUP_DIR" && -d "$BACKUP_DIR" ]] || { echo "ERROR: Valid --backup-dir is required" >&2; exit 2; }
[[ -n "$ENV_FILE" && -f "$ENV_FILE" ]] || { echo "ERROR: Existing --env-file is required" >&2; exit 2; }
[[ -f "$COMPOSE_FILE" ]] || { echo "ERROR: Compose file not found: $COMPOSE_FILE" >&2; exit 2; }
[[ -f "$BACKUP_DIR/checksums.sha256" && -f "$BACKUP_DIR/metadata.env" ]] || { echo "ERROR: Backup manifest is incomplete" >&2; exit 2; }
command -v docker >/dev/null || { echo "ERROR: docker is required" >&2; exit 2; }
[[ -n "$TARGET_PROJECT" ]] || TARGET_PROJECT="srm-restore-$(date -u +%Y%m%d%H%M%S)"
[[ "$TARGET_PROJECT" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "ERROR: Invalid target project" >&2; exit 2; }
[[ "$TARGET_PROJECT" =~ ^srm-(restore|stage1-acceptance-restore)-[A-Za-z0-9_.-]+$ ]] \
  || { echo "ERROR: Target project must use a restore-only prefix" >&2; exit 2; }

env_value() {
  sed -n "s/^${1}=//p" "$ENV_FILE" | tail -n 1
}
ENV_PROJECT="$(env_value COMPOSE_PROJECT_NAME)"
ENV_MYSQL_VOLUME="$(env_value SRM_MYSQL_VOLUME_NAME)"
ENV_ATTACHMENT_VOLUME="$(env_value SRM_ATTACHMENT_VOLUME_NAME)"
[[ "$ENV_PROJECT" == "$TARGET_PROJECT" ]] \
  || { echo "ERROR: COMPOSE_PROJECT_NAME must exactly match the isolated target" >&2; exit 2; }
[[ "$ENV_MYSQL_VOLUME" == "${TARGET_PROJECT}-mysql-data" ]] \
  || { echo "ERROR: MySQL volume must be ${TARGET_PROJECT}-mysql-data" >&2; exit 2; }
[[ "$ENV_ATTACHMENT_VOLUME" == "${TARGET_PROJECT}-attachments" ]] \
  || { echo "ERROR: Attachment volume must be ${TARGET_PROJECT}-attachments" >&2; exit 2; }

EVIDENCE_DIR="${EVIDENCE_ROOT}/${TARGET_PROJECT}"
mkdir -p "$EVIDENCE_DIR"
LOG_FILE="${EVIDENCE_DIR}/restore.log"
exec > >(tee -a "$LOG_FILE") 2>&1
trap 'code=$?; if [[ $code -ne 0 ]]; then printf "status=FAILED\nexit_code=%s\n" "$code" > "${EVIDENCE_DIR}/status.env"; echo "ERROR: Restore failed; evidence retained at ${EVIDENCE_DIR}" >&2; fi' EXIT

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'
  else shasum -a 256 "$1" | awk '{print $1}'; fi
}
metadata() { grep -E "^${1}=" "$BACKUP_DIR/metadata.env" | head -1 | cut -d= -f2-; }

echo "=== SRM Restore preflight ==="
echo "Backup: ${BACKUP_DIR}"
echo "Target: ${TARGET_PROJECT}"
echo "[1/8] Verifying manifest and SHA-256"
while read -r expected file; do
  [[ "$file" != */* && "$file" != *..* ]] || { echo "ERROR: Unsafe manifest entry: $file" >&2; exit 3; }
  [[ -f "$BACKUP_DIR/$file" ]] || { echo "ERROR: Manifest file missing: $file" >&2; exit 3; }
  [[ "$(sha256_file "$BACKUP_DIR/$file")" == "$expected" ]] || { echo "ERROR: SHA-256 mismatch: $file" >&2; exit 3; }
done < "$BACKUP_DIR/checksums.sha256"

SQL_GZ="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name '*.sql.gz' -print | head -1)"
[[ -n "$SQL_GZ" ]] || { echo "ERROR: SQL archive missing" >&2; exit 3; }
gzip -t "$SQL_GZ"
ATTACH_GZ="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name '*-attachments.tar.gz' -print | head -1)"
if [[ -n "$ATTACH_GZ" && "$SKIP_ATTACHMENTS" == false ]]; then
  while IFS= read -r entry; do
    [[ "$entry" != /* && "$entry" != *../* ]] || { echo "ERROR: Unsafe attachment archive entry" >&2; exit 3; }
  done < <(tar -tzf "$ATTACH_GZ")
fi

COMPOSE=(docker compose -p "$TARGET_PROJECT" -f "$COMPOSE_FILE" --env-file "$ENV_FILE")
echo "[2/8] Refusing active target project"
RUNNING="$("${COMPOSE[@]}" ps -q)"
[[ -z "$RUNNING" ]] || { echo "ERROR: Target project already has running containers" >&2; exit 4; }

echo "[3/8] Starting target MySQL and waiting for health"
"${COMPOSE[@]}" up -d mysql
READY=false
for _ in $(seq 1 60); do
  if "${COMPOSE[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqladmin ping -h 127.0.0.1 -uroot --silent' >/dev/null 2>&1; then READY=true; break; fi
  sleep 2
done
[[ "$READY" == true ]] || { echo "ERROR: MySQL readiness timed out" >&2; exit 5; }

mysql_query() {
  "${COMPOSE[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$MYSQL_DATABASE" -e "$1"' sh "$1"
}

echo "[4/8] Checking target schema emptiness"
TARGET_TABLES="$("${COMPOSE[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=\"$MYSQL_DATABASE\";"')"
if [[ "$TARGET_TABLES" != "0" && "$FORCE_NONEMPTY" != true ]]; then
  echo "ERROR: Target schema contains ${TARGET_TABLES} tables; pass --force-nonempty only after explicit review" >&2
  exit 6
fi

echo "[5/8] Importing verified logical dump"
SQL_NAME="$(basename "$SQL_GZ")"
MYSQL_CID="$("${COMPOSE[@]}" ps -q mysql)"
docker cp "$SQL_GZ" "$MYSQL_CID:/tmp/$SQL_NAME"
"${COMPOSE[@]}" exec -T -e RESTORE_SQL="$SQL_NAME" mysql bash -o pipefail -c \
  'gunzip -c "/tmp/$RESTORE_SQL" | MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"'
"${COMPOSE[@]}" exec -T mysql rm -f "/tmp/$SQL_NAME"

echo "[6/8] Validating tables, roles, users and Flyway"
TABLE_COUNT="$("${COMPOSE[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=\"$MYSQL_DATABASE\";"')"
ROLE_COUNT="$(mysql_query 'SELECT COUNT(*) FROM sys_role;')"
USER_COUNT="$(mysql_query 'SELECT COUNT(*) FROM sys_user;')"
FLYWAY_VERSION="$(mysql_query 'SELECT COALESCE(MAX(CAST(version AS UNSIGNED)),0) FROM flyway_schema_history WHERE success=1;')"
[[ "$TABLE_COUNT" -gt 0 && "$ROLE_COUNT" -gt 0 && "$USER_COUNT" -gt 0 && "$FLYWAY_VERSION" -gt 0 ]] || { echo "ERROR: Post-restore validation returned empty governance data" >&2; exit 7; }
[[ "$TABLE_COUNT" == "$(metadata TABLE_COUNT)" ]] || { echo "ERROR: Table count differs from backup metadata" >&2; exit 7; }
[[ "$ROLE_COUNT" == "$(metadata ROLE_COUNT)" ]] || { echo "ERROR: Role count differs from backup metadata" >&2; exit 7; }
[[ "$USER_COUNT" == "$(metadata USER_COUNT)" ]] || { echo "ERROR: User count differs from backup metadata" >&2; exit 7; }
[[ "$FLYWAY_VERSION" == "$(metadata FLYWAY_VERSION)" ]] || { echo "ERROR: Flyway version differs from backup metadata" >&2; exit 7; }

echo "[7/8] Restoring attachments"
if [[ "$SKIP_ATTACHMENTS" == true ]]; then
  echo "Attachments explicitly omitted"
elif [[ "$(metadata ATTACHMENTS_INCLUDED)" == "true" ]]; then
  [[ -n "$ATTACH_GZ" ]] || { echo "ERROR: Metadata requires attachment archive" >&2; exit 8; }
  "${COMPOSE[@]}" up -d --wait backend
  BACKEND_CID="$("${COMPOSE[@]}" ps -q backend)"
  [[ -n "$BACKEND_CID" ]] || { echo "ERROR: Backend unavailable for attachment restore" >&2; exit 8; }
  docker cp "$ATTACH_GZ" "$BACKEND_CID:/tmp/restore-attachments.tar.gz"
  "${COMPOSE[@]}" exec -T backend sh -c 'root="${SRM_ATTACHMENT_ROOT:-/var/lib/srm/attachments}"; mkdir -p "$root"; tar -xzf /tmp/restore-attachments.tar.gz -C "$root"; rm -f /tmp/restore-attachments.tar.gz'
else
  echo "Backup intentionally contains no attachments"
fi

echo "[8/8] Starting and health-checking the complete stack"
"${COMPOSE[@]}" up -d --wait
RUNNING_SERVICES="$("${COMPOSE[@]}" ps --status running --services | wc -l | tr -d ' ')"
[[ "$RUNNING_SERVICES" -eq 5 ]] || { echo "ERROR: Expected 5 running services, found $RUNNING_SERVICES" >&2; exit 9; }
"${COMPOSE[@]}" ps > "${EVIDENCE_DIR}/compose-ps.txt"
cp "$BACKUP_DIR/metadata.env" "${EVIDENCE_DIR}/backup-metadata.env"
cp "$BACKUP_DIR/checksums.sha256" "${EVIDENCE_DIR}/verified-checksums.sha256"
cat > "${EVIDENCE_DIR}/validation.env" <<EOF
TABLE_COUNT=${TABLE_COUNT}
ROLE_COUNT=${ROLE_COUNT}
USER_COUNT=${USER_COUNT}
FLYWAY_VERSION=${FLYWAY_VERSION}
RUNNING_SERVICES=${RUNNING_SERVICES}
EOF
printf 'status=COMPLETE\ncompleted_at=%s\n' "$(date -u -Iseconds)" > "${EVIDENCE_DIR}/status.env"

echo "=== Restore complete ==="
echo "Evidence: ${EVIDENCE_DIR}"
echo "Tables=${TABLE_COUNT}, roles=${ROLE_COUNT}, users=${USER_COUNT}, Flyway=${FLYWAY_VERSION}, services=${RUNNING_SERVICES}"
