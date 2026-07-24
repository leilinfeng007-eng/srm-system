#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

docker compose down
echo "阶段0服务已停止；MySQL命名卷保留。"
