#!/usr/bin/env bash
# Starts the backend for the end-to-end tests, on the TEST database, with the demo accounts of this run.
. "$(dirname "$0")/test-db.sh"

# Empty the database so the backend seeds the accounts with the password of this run. The only error that is expected is the
# very first run, when the schema does not exist yet (Flyway creates it): any other one (password, server stopped...) stops here.
if ! result=$(psql_test -c "TRUNCATE task_comment, task_assignee, task, project_member, project, app_user RESTART IDENTITY CASCADE" 2>&1); then
  case "$result" in
    *"does not exist"*) ;;
    *) echo "$result" >&2; exit 1 ;;
  esac
fi
export DB_URL="$TEST_DB_URL" DB_USER="$TEST_DB_USER" DB_PASSWORD="$TEST_DB_PASSWORD"
export DEMO_PASSWORD="$E2E_PASSWORD" SERVER_PORT=8081
exec mvn -q spring-boot:run
