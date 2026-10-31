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
    DB_NAME DB_USER DB_PASSWORD
    KAFKA_NODE_ID KAFKA_PROCESS_ROLES KAFKA_LISTENERS KAFKA_ADVERTISED_LISTENERS
    KAFKA_CONTROLLER_LISTENER_NAMES KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
    KAFKA_CONTROLLER_QUORUM_VOTERS KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
    KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS
    KAFKA_BOOTSTRAP_SERVERS NOTIFICATION_TOPIC NOTIFICATION_CONSUMER_GROUP
    NOTIFICATION_KAFKA_ENABLED
    PAYMENT_REMINDER_ENABLED PAYMENT_REMINDER_LEAD_MINUTES PAYMENT_REMINDER_CRON
    DEPARTURE_REMINDER_ENABLED DEPARTURE_REMINDER_LEAD_HOURS DEPARTURE_REMINDER_CRON
    FILE_PROVIDER_ENABLED FILE_PROVIDER_LOCATION
    JWT_SECRET PASSPORT_ENCRYPTION_KEY COOKIE_SECURE SWAGGER_UI_ENABLED
    PULL_CRON PULL_ENABLED PULL_HORIZON_DAYS PULL_ON_STARTUP
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

podman build -t localhost/flight-booking-backend:latest ../backend
podman build -t localhost/flight-booking-frontend:latest ../frontend

envsubst "$subst" < infrastructure.yaml | podman kube play - "$@"
