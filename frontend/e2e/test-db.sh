# Sourced by the other scripts: loads the settings of the backend and gives `psql_test`, which talks to the TEST database.
# The safety rule is here, once: nothing is emptied in a database whose name does not end in _test.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../matriz"
# The settings come from matriz/.env on a developer's machine; where there is none (the CI) they come from the environment
if [ -f ./.env ]; then
  set -a
  . ./.env
  set +a
fi
: "${TEST_DB_URL:?TEST_DB_URL is not set (matriz/.env or the environment)}" "${TEST_DB_USER:?}" "${TEST_DB_PASSWORD:?}"
case "$TEST_DB_URL" in
  *_test) ;;
  *) echo "Refusing to use $TEST_DB_URL: the end-to-end tests only run against a database whose name ends in _test." >&2; exit 1 ;;
esac

psql_test() {
  PGPASSWORD="$TEST_DB_PASSWORD" psql -q -v ON_ERROR_STOP=1 -U "$TEST_DB_USER" "${TEST_DB_URL#jdbc:}" "$@"
}
