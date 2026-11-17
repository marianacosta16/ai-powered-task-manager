#!/usr/bin/env bash
# Empties the projects, the teams and the tasks of the TEST database and keeps the demo accounts, so a scenario can start
# from "nobody is in an open project" (the demo data of the backend puts the members in projects).
set -euo pipefail
cd "$(dirname "$0")/../../matriz"
set -a
. ./.env
set +a
case "$TEST_DB_URL" in
  *_test) ;;
  *) echo "Refusing to empty $TEST_DB_URL: only a database whose name ends in _test." >&2; exit 1 ;;
esac
PGPASSWORD="$TEST_DB_PASSWORD" psql -q -U "$TEST_DB_USER" "${TEST_DB_URL#jdbc:}" \
  -c "TRUNCATE task_comment, task_assignee, task, project_member, project RESTART IDENTITY CASCADE"
