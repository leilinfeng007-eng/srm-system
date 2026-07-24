#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

npm ci --ignore-scripts
./scripts/maven-command.sh -f backend/pom.xml -B clean package
npm run openapi:check
npm run typecheck
npm run build
node scripts/generate-skeleton.mjs
echo "后端与双前端生产构建完成。"
