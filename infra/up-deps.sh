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

vars=(
    POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
    KAFKA_NODE_ID KAFKA_PROCESS_ROLES KAFKA_LISTENERS KAFKA_ADVERTISED_LISTENERS
    KAFKA_CONTROLLER_LISTENER_NAMES KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
    KAFKA_CONTROLLER_QUORUM_VOTERS KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
    KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS
)

for name in "${vars[@]}"; do
    if [[ -z "${!name:-}" ]]; then
        echo "Environment variable $name is not set in infra/.env" >&2
        exit 1
    fi
done

subst=""
for name in "${vars[@]}"; do
    subst+="\${$name} "
done

envsubst "$subst" < dependencies.yaml | podman kube play - "$@"
