#!/bin/sh
# Runs Maven and tries again when it fails, because a download from Maven Central fails now and then
# ("Non-resolvable parent POM ... (absent)"). -U makes Maven ask again for what it has remembered as missing, and the
# aether property makes its own HTTP client retry too (Maven 3.9 no longer uses Wagon, so the wagon properties do nothing).
attempts=4
n=1
while [ "$n" -le "$attempts" ]; do
  mvn -B -q -U -Daether.connector.http.retryHandler.count=5 "$@" && exit 0
  echo "Maven failed (attempt $n of $attempts)." >&2
  [ "$n" -lt "$attempts" ] && sleep $((n * 10))
  n=$((n + 1))
done
exit 1
