# Sourced by the other scripts: loads the settings of the backend and gives `psql_test`, which talks to the TEST database.
# The safety rule is here, once: nothing is emptied in a database whose name does not end in _test.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../matriz"
# The settings come from matriz/.env on a developer's machine; where there is none (the CI) they come from the environment.
# A TEST_DB_* value that is already exported wins over the one in the file, so the tests can be pointed at another _test database.
saved=()
for name in TEST_DB_URL TEST_DB_USER TEST_DB_PASSWORD; do
  [ -n "${!name-}" ] && saved+=("$name=${!name}")
done
if [ -f ./.env ]; then
  set -a
  . ./.env
  set +a
fi
for pair in ${saved[@]+"${saved[@]}"}; do export "$pair"; done
hint="(matriz/.env or the environment)"
: "${TEST_DB_URL:?TEST_DB_URL is not set $hint}" "${TEST_DB_USER:?TEST_DB_USER is not set $hint}" "${TEST_DB_PASSWORD:?TEST_DB_PASSWORD is not set $hint}"
case "$TEST_DB_URL" in
  *_test) ;;
  *) echo "Refusing to use $TEST_DB_URL: the end-to-end tests only run against a database whose name ends in _test." >&2; exit 1 ;;
esac

psql_test() {
  PGPASSWORD="$TEST_DB_PASSWORD" psql -q -v ON_ERROR_STOP=1 -U "$TEST_DB_USER" "${TEST_DB_URL#jdbc:}" "$@"
}
