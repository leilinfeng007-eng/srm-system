#!/usr/bin/env bash
set -euo pipefail

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

idea_maven="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
if [[ -x "$idea_maven" ]]; then
  exec "$idea_maven" "$@"
fi

echo "未找到 Maven。请使用 IntelliJ IDEA 内置 Maven，或将 Maven 3.9+ 加入 PATH。" >&2
exit 127
