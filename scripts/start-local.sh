#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

[[ -f .env ]] || { echo "请先执行 cp .env.example .env，并替换全部 CHANGE_ME 值。" >&2; exit 1; }
if rg -q 'CHANGE_ME' .env; then
  echo ".env 仍包含 CHANGE_ME，占位秘密不得用于启动。" >&2
  exit 1
fi

docker compose up --build --detach --wait
gateway_port="$(sed -n 's/^SRM_GATEWAY_PORT=//p' .env | tail -n 1)"
gateway_port="${gateway_port:-8088}"
echo "内部端: http://localhost:${gateway_port}/"
echo "供应商端: http://localhost:${gateway_port}/supplier/"
echo "健康检查: http://localhost:${gateway_port}/healthz"
