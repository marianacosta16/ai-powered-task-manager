#!/usr/bin/env bash
# Smoke test of the Compose stack: starts it, signs in over HTTPS, creates a task and reads it back, and always stops it.
# Needs in the environment: DB_PASSWORD, DEMO_PASSWORD (fake values in the CI), and optionally WEB_HTTP_PORT and WEB_HTTPS_PORT.
set -euo pipefail

: "${DB_PASSWORD:?}" "${DEMO_PASSWORD:?}"
http_port="${WEB_HTTP_PORT:-8080}"
https_port="${WEB_HTTPS_PORT:-8443}"
base="https://localhost:${https_port}"
jar="$(mktemp)"

cleanup() {
  status=$?
  if [ "$status" -ne 0 ]; then docker compose logs --no-color --tail=80 || true; fi
  docker compose down -v || true
  rm -f "$jar"
  exit "$status"
}
trap cleanup EXIT

fail() { echo "FAILED: $*" >&2; exit 1; }
check() { [ "$2" = "$3" ] || fail "$1: expected '$3', got '$2'"; }

docker compose up -d --build --wait --wait-timeout 300

# -k: the certificate of the stack is self-signed
check "health through /api" "$(curl -ksf "$base/api/actuator/health" | grep -o '"status":"UP"' | head -1)" '"status":"UP"'
check "HTTP redirects to HTTPS, keeping the port" "$(curl -s -o /dev/null -w '%{redirect_url}' "http://localhost:${http_port}/projects/1")" "$base/projects/1"
check "a page of the interface (router fallback)" "$(curl -ks -o /dev/null -w '%{http_code}' "$base/projects/1")" 200
check "no session" "$(curl -ks -o /dev/null -w '%{http_code}' "$base/api/projects")" 401
check "wrong password" "$(curl -ks -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' -d '{"username":"ana","password":"wrong-password"}' "$base/api/auth/login")" 401

headers="$(curl -ks -c "$jar" -D - -o /dev/null -H 'Content-Type: application/json' \
  -d "{\"username\":\"ana\",\"password\":\"${DEMO_PASSWORD}\"}" "$base/api/auth/login")"
check "sign in" "$(printf '%s' "$headers" | head -1 | tr -d '\r' | cut -d' ' -f2)" 200
printf '%s' "$headers" | grep -i '^set-cookie: SESSION=' | grep -qi 'secure' || fail "the session cookie is not Secure"

deadline="$(date -d '+7 days' +%F)"
created="$(curl -ks -b "$jar" -H 'Content-Type: application/json' -w '\n%{http_code}' \
  -d "{\"title\":\"Docker smoke task\",\"deadline\":\"${deadline}\",\"urgency\":3,\"importance\":4,\"assigneeIds\":[1]}" "$base/api/projects/1/tasks")"
check "create a task" "$(printf '%s' "$created" | tail -1)" 201
id="$(printf '%s' "$created" | head -1 | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)"
[ -n "$id" ] || fail "no id in the created task"
check "read the task back" "$(curl -ks -b "$jar" "$base/api/tasks/${id}" | grep -o '"title":"[^"]*"')" '"title":"Docker smoke task"'

echo "Docker smoke test passed."
