#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

BASE_URL=${BASE_URL:-http://localhost:8080}
MAILPIT_URL=${MAILPIT_URL:-http://localhost:8025}
LOAD_EMAIL=${LOAD_EMAIL:-load@example.com}
LOAD_PASSWORD=${LOAD_PASSWORD:-Password123!}
SCENARIO=${1:-smoke.js}
COOKIE_JAR=$(mktemp)

cleanup() {
    rm -f "$COOKIE_JAR"
}

trap cleanup EXIT

api() {
    curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$@"
}

login_status() {
    curl -s -o /dev/null -w '%{http_code}' -c "$COOKIE_JAR" \
        -X POST "$BASE_URL/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"$LOAD_EMAIL\",\"password\":\"$LOAD_PASSWORD\"}"
}

latest_code() {
    local messages id
    messages=$(curl -s "$MAILPIT_URL/api/v1/messages")
    id=$(printf '%s' "$messages" | python3 -c '
import json, sys
data = json.load(sys.stdin)
for message in data.get("messages", []):
    recipients = [entry.get("Address", "") for entry in message.get("To", [])]
    if sys.argv[1] in recipients:
        print(message["ID"])
        break
' "$LOAD_EMAIL")
    [ -n "$id" ] || return 1
    curl -s "$MAILPIT_URL/api/v1/message/$id" | python3 -c '
import json, re, sys
body = json.load(sys.stdin)
text = (body.get("Text") or "") + " " + (body.get("HTML") or "")
match = re.search(r"\b(\d{6})\b", text)
print(match.group(1) if match else "")
'
}

register_account() {
    api -X POST "$BASE_URL/api/auth/register" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"$LOAD_EMAIL\",\"password\":\"$LOAD_PASSWORD\"}" > /dev/null
}

verify_account() {
    local code=""
    for _ in $(seq 1 30); do
        code=$(latest_code || true)
        [ -n "$code" ] && break
        sleep 1
    done
    if [ -z "$code" ]; then
        echo "No verification code found in Mailpit for $LOAD_EMAIL" >&2
        exit 1
    fi
    api -X POST "$BASE_URL/api/auth/verify" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"$LOAD_EMAIL\",\"code\":\"$code\"}" > /dev/null
}

release_reservations() {
    local response status body reservations
    response=$(curl -s -w '\n%{http_code}' -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
        "$BASE_URL/api/v1/reservations" || true)
    status=${response##*$'\n'}
    body=${response%$'\n'*}
    if [ "$status" != "200" ]; then
        echo "Skipping cleanup: GET /api/v1/reservations answered ${status:-no response}" >&2
        return 0
    fi

    reservations=$(printf '%s' "$body" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
except json.JSONDecodeError:
    data = []
for reservation in data:
    seats = [seat["seat"] for seat in reservation.get("seats", [])]
    if seats:
        print(reservation["reservationId"], " ".join(seats))
' 2>/dev/null || true)

    printf '%s\n' "$reservations" | while read -r id seats; do
        [ -n "${id:-}" ] || continue
        body=$(python3 -c 'import json,sys; print(json.dumps({"seats": sys.argv[1:]}))' $seats)
        curl -s -o /dev/null -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
            -X DELETE "$BASE_URL/api/v1/reservations/$id/seats" \
            -H 'Content-Type: application/json' -d "$body" || true
        curl -s -o /dev/null -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
            -X DELETE "$BASE_URL/api/v1/reservations/$id" || true
    done
}

if [ "$(login_status)" != "200" ]; then
    echo "Creating load-test account $LOAD_EMAIL"
    register_account
    verify_account
    if [ "$(login_status)" != "200" ]; then
        echo "Could not sign in as $LOAD_EMAIL" >&2
        exit 1
    fi
fi

if [ "$SCENARIO" = "cleanup" ]; then
    release_reservations
    echo "Reservations released"
    exit 0
fi

echo "Running $SCENARIO"
set +e
k6 run -e LOAD_EMAIL="$LOAD_EMAIL" -e LOAD_PASSWORD="$LOAD_PASSWORD" "$SCENARIO"
status=$?
set -e

if [ "${KEEP_DATA:-0}" != "1" ]; then
    login_status > /dev/null || true
    release_reservations
fi

exit "$status"
