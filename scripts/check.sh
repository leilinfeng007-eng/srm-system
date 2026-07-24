#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"

./scripts/maven-command.sh -f backend/pom.xml -B clean test
npm ci --ignore-scripts
npm run openapi:check
npm run lint
npm run typecheck
npm test
npm run build
node scripts/validate-architecture.mjs
node scripts/generate-skeleton.mjs
./scripts/check-migrations.sh
node scripts/validate-deploy.mjs
node scripts/validate-security.mjs

if command -v docker >/dev/null 2>&1; then
  docker compose --env-file .env.example config --quiet
  if docker compose --env-file deploy/prod.env.example -f deploy/docker-compose.prod.yml config --quiet >/dev/null 2>&1; then
    echo "生产环境样例必须因必填值为空而拒绝Compose解析。" >&2
    exit 1
  fi
else
  if command -v ruby >/dev/null 2>&1; then
    ruby -e 'require "yaml"; YAML.load_file("docker-compose.yml"); YAML.load_file("deploy/docker-compose.prod.yml"); puts "Local and production Compose YAML syntax checks passed."'
  fi
  echo "提示: 当前环境无 Docker，已完成本地/生产Compose静态与YAML检查，未执行Docker CLI或容器运行验收。" >&2
fi

echo "阶段0全量工程检查通过。"
