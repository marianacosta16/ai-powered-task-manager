#!/usr/bin/env bash
# Starts the backend for the end-to-end tests, on the TEST database, with the demo accounts of this run.
set -euo pipefail
cd "$(dirname "$0")/../../matriz"
set -a
. ./.env
set +a
case "$TEST_DB_URL" in
  *_test) ;;
  *) echo "Refusing to empty $TEST_DB_URL: the end-to-end tests only run against a database whose name ends in _test." >&2; exit 1 ;;
esac
PGPASSWORD="$TEST_DB_PASSWORD" psql -q -U "$TEST_DB_USER" "${TEST_DB_URL#jdbc:}" \
  -c "TRUNCATE task_comment, task_assignee, task, project_member, project, app_user RESTART IDENTITY CASCADE" 2>/dev/null \
  || true # the very first run: the schema does not exist yet, Flyway creates it
export DB_URL="$TEST_DB_URL" DB_USER="$TEST_DB_USER" DB_PASSWORD="$TEST_DB_PASSWORD"
export DEMO_PASSWORD="$E2E_PASSWORD" SERVER_PORT=8081
exec mvn -q spring-boot:run
