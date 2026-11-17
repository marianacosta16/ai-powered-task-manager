#!/usr/bin/env bash
# Empties the projects, the teams and the tasks of the TEST database and keeps the demo accounts, so a scenario can start
# from "nobody is in an open project" (the demo data of the backend puts the members in projects).
. "$(dirname "$0")/test-db.sh"
psql_test -c "TRUNCATE task_comment, task_assignee, task, project_member, project RESTART IDENTITY CASCADE"
