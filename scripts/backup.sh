#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./scripts/backup.sh [options]

Creates a traceable SRM MySQL and attachment backup from an already-running
Docker Compose project. This command never builds or starts services.

Options:
  --project NAME        Compose project name (default: srm-stage1)
  --compose-file FILE   Compose file (default: docker-compose.yml)
  --env-file FILE       Existing external env file (default: .env)
  --output-dir DIR      Backup output directory (default: ./backups)
  --db-name NAME        Database name (default: srm)
  --no-attachments      Explicitly omit attachments
EOF
}

PROJECT="${COMPOSE_PROJECT_NAME:-srm-stage1}"
COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"
OUTPUT_DIR="./backups"
DB_NAME="${SRM_DB_NAME:-srm}"
SKIP_ATTACHMENTS=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="${2:?missing project}"; shift 2 ;;
    --compose-file) COMPOSE_FILE="${2:?missing compose file}"; shift 2 ;;
    --env-file) ENV_FILE="${2:?missing env file}"; shift 2 ;;
    --output-dir) OUTPUT_DIR="${2:?missing output directory}"; shift 2 ;;
    --db-name) DB_NAME="${2:?missing database name}"; shift 2 ;;
    --no-attachments) SKIP_ATTACHMENTS=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "ERROR: Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

[[ -f "$COMPOSE_FILE" ]] || { echo "ERROR: Compose file not found: $COMPOSE_FILE" >&2; exit 2; }
[[ -f "$ENV_FILE" ]] || { echo "ERROR: External env file not found: $ENV_FILE" >&2; exit 2; }
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || { echo "ERROR: Invalid database name" >&2; exit 2; }
command -v docker >/dev/null || { echo "ERROR: docker is required" >&2; exit 2; }

COMPOSE=(docker compose -p "$PROJECT" -f "$COMPOSE_FILE" --env-file "$ENV_FILE")
TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
BACKUP_NAME="srm-backup-${TIMESTAMP}"
BACKUP_DIR="${OUTPUT_DIR}/${BACKUP_NAME}"
mkdir -p "$BACKUP_DIR"
LOG_FILE="${BACKUP_DIR}/backup.log"
exec > >(tee -a "$LOG_FILE") 2>&1
trap 'code=$?; if [[ $code -ne 0 ]]; then printf "status=FAILED\nexit_code=%s\n" "$code" > "${BACKUP_DIR}/status.env"; echo "ERROR: Backup failed; evidence retained at ${BACKUP_DIR}" >&2; fi' EXIT

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'
  else shasum -a 256 "$1" | awk '{print $1}'; fi
}

MYSQL_CONTAINER="$("${COMPOSE[@]}" ps -q mysql)"
[[ -n "$MYSQL_CONTAINER" ]] || { echo "ERROR: MySQL service is not running" >&2; exit 3; }
MYSQL_HEALTH="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$MYSQL_CONTAINER")"
[[ "$MYSQL_HEALTH" == "healthy" ]] || { echo "ERROR: MySQL is not healthy (state=$MYSQL_HEALTH)" >&2; exit 3; }

mysql_query() {
  docker exec -e TARGET_DB="$DB_NAME" "$MYSQL_CONTAINER" sh -c \
    'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N "$TARGET_DB" -e "$1"' sh "$1"
}

echo "=== SRM Backup ${TIMESTAMP} ==="
echo "Project: ${PROJECT}"
echo "Database: ${DB_NAME}"
echo "Output: ${BACKUP_DIR}"

SQL_GZ="${BACKUP_DIR}/${BACKUP_NAME}.sql.gz"
echo "[1/6] Creating consistent logical dump"
docker exec -e TARGET_DB="$DB_NAME" -e BACKUP_NAME="$BACKUP_NAME" "$MYSQL_CONTAINER" sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u "$MYSQL_USER" --single-transaction --routines --triggers --skip-lock-tables --default-character-set=utf8mb4 --result-file="/tmp/${BACKUP_NAME}.sql" "$TARGET_DB" && gzip -f "/tmp/${BACKUP_NAME}.sql"'
docker cp "$MYSQL_CONTAINER:/tmp/${BACKUP_NAME}.sql.gz" "$SQL_GZ"
docker exec "$MYSQL_CONTAINER" rm -f "/tmp/${BACKUP_NAME}.sql.gz"
gzip -t "$SQL_GZ"

echo "[2/6] Capturing exact validation counts"
ROW_COUNTS_FILE="${BACKUP_DIR}/row-counts.txt"
TABLES="$(mysql_query "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${DB_NAME}' ORDER BY TABLE_NAME;")"
: > "$ROW_COUNTS_FILE"
while IFS= read -r table; do
  [[ -z "$table" ]] && continue
  count="$(mysql_query "SELECT COUNT(*) FROM \`${table}\`;")"
  printf '%s=%s\n' "$table" "$count" >> "$ROW_COUNTS_FILE"
done <<< "$TABLES"
TABLE_COUNT="$(mysql_query "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${DB_NAME}';")"
ROLE_COUNT="$(mysql_query 'SELECT COUNT(*) FROM sys_role;')"
USER_COUNT="$(mysql_query 'SELECT COUNT(*) FROM sys_user;')"
FLYWAY_VERSION="$(mysql_query "SELECT COALESCE(MAX(CAST(version AS UNSIGNED)),0) FROM flyway_schema_history WHERE success=1;")"

echo "[3/6] Archiving attachments"
ATTACH_GZ="${BACKUP_DIR}/${BACKUP_NAME}-attachments.tar.gz"
ATTACH_INCLUDED=false
if [[ "$SKIP_ATTACHMENTS" == true ]]; then
  echo "Attachments explicitly omitted"
else
  BACKEND_CONTAINER="$("${COMPOSE[@]}" ps -q backend)"
  [[ -n "$BACKEND_CONTAINER" ]] || { echo "ERROR: Backend is not running; use --no-attachments only when omission is intentional" >&2; exit 4; }
  ATTACH_ROOT="$(docker exec "$BACKEND_CONTAINER" printenv SRM_ATTACHMENT_ROOT)"
  [[ -n "$ATTACH_ROOT" ]] || ATTACH_ROOT="/var/lib/srm/attachments"
  docker exec "$BACKEND_CONTAINER" test -d "$ATTACH_ROOT" || { echo "ERROR: Attachment root not found" >&2; exit 4; }
  docker exec -e BACKUP_NAME="$BACKUP_NAME" -e ATTACH_ROOT="$ATTACH_ROOT" "$BACKEND_CONTAINER" sh -c \
    'tar -czf "/tmp/${BACKUP_NAME}-attachments.tar.gz" -C "$ATTACH_ROOT" .'
  docker cp "$BACKEND_CONTAINER:/tmp/${BACKUP_NAME}-attachments.tar.gz" "$ATTACH_GZ"
  docker exec "$BACKEND_CONTAINER" rm -f "/tmp/${BACKUP_NAME}-attachments.tar.gz"
  tar -tzf "$ATTACH_GZ" >/dev/null
  ATTACH_INCLUDED=true
fi

echo "[4/6] Writing machine-readable metadata"
cat > "${BACKUP_DIR}/metadata.env" <<EOF
BACKUP_NAME=${BACKUP_NAME}
TIMESTAMP_UTC=$(date -u -Iseconds)
PROJECT=${PROJECT}
DATABASE=${DB_NAME}
TABLE_COUNT=${TABLE_COUNT}
ROLE_COUNT=${ROLE_COUNT}
USER_COUNT=${USER_COUNT}
FLYWAY_VERSION=${FLYWAY_VERSION}
ATTACHMENTS_INCLUDED=${ATTACH_INCLUDED}
EOF

echo "[5/6] Writing SHA-256 manifest"
CHECKSUMS="${BACKUP_DIR}/checksums.sha256"
printf '%s  %s\n' "$(sha256_file "$SQL_GZ")" "$(basename "$SQL_GZ")" > "$CHECKSUMS"
printf '%s  %s\n' "$(sha256_file "$ROW_COUNTS_FILE")" "$(basename "$ROW_COUNTS_FILE")" >> "$CHECKSUMS"
printf '%s  %s\n' "$(sha256_file "${BACKUP_DIR}/metadata.env")" "metadata.env" >> "$CHECKSUMS"
if [[ "$ATTACH_INCLUDED" == true ]]; then
  printf '%s  %s\n' "$(sha256_file "$ATTACH_GZ")" "$(basename "$ATTACH_GZ")" >> "$CHECKSUMS"
fi

echo "[6/6] Final verification"
while read -r expected file; do
  actual="$(sha256_file "${BACKUP_DIR}/${file}")"
  [[ "$actual" == "$expected" ]] || { echo "ERROR: Checksum mismatch for $file" >&2; exit 5; }
done < "$CHECKSUMS"
printf 'status=COMPLETE\ncompleted_at=%s\n' "$(date -u -Iseconds)" > "${BACKUP_DIR}/status.env"

echo "=== Backup complete ==="
echo "Location: ${BACKUP_DIR}"
echo "Tables=${TABLE_COUNT}, roles=${ROLE_COUNT}, users=${USER_COUNT}, Flyway=${FLYWAY_VERSION}"
