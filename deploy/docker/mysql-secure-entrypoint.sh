#!/bin/sh
set -eu

require_secret() {
  variable_name="$1"
  minimum_length="$2"
  eval "secret_value=\${$variable_name:-}"

  if [ -z "$secret_value" ]; then
    echo "$variable_name must be configured." >&2
    exit 64
  fi
  if [ "${#secret_value}" -lt "$minimum_length" ]; then
    echo "$variable_name must contain at least $minimum_length characters." >&2
    exit 64
  fi

  normalized="$(printf '%s' "$secret_value" | tr '[:lower:] -' '[:upper:]__')"
  case "$normalized" in
    *CHANGE_ME*|*CHANGEME*|*REPLACE_ME*|*REPLACE_WITH*|*EXAMPLE_ONLY*|*EXAMPLE_PASSWORD*|*YOUR_PASSWORD*|*PLACEHOLDER*)
      echo "$variable_name must not use an example or placeholder value." >&2
      exit 64
      ;;
  esac
  case "$secret_value" in
    *"<"*|*">"*)
      echo "$variable_name must not use an example or placeholder value." >&2
      exit 64
      ;;
  esac
}

require_secret MYSQL_PASSWORD 12
require_secret MYSQL_ROOT_PASSWORD 16

if [ "${SRM_VALIDATE_ONLY:-false}" = "true" ]; then
  echo "MySQL secret policy check passed."
  exit 0
fi

exec /usr/local/bin/docker-entrypoint.sh "$@"
