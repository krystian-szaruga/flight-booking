#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
    echo "Missing infra/.env (see infra/.env.example)" >&2
    exit 1
fi

set -a
. ./.env
set +a

for name in POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD KC_BOOTSTRAP_ADMIN_PASSWORD FEED_CLIENT_SECRET; do
    if [[ -z "${!name:-}" ]]; then
        echo "Environment variable $name is not set in infra/.env" >&2
        exit 1
    fi
done

envsubst '${POSTGRES_DB} ${POSTGRES_USER} ${POSTGRES_PASSWORD} ${KC_BOOTSTRAP_ADMIN_PASSWORD} ${FEED_CLIENT_SECRET}' \
    < infrastructure.yaml | podman kube play - "$@"