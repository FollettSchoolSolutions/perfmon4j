#!/usr/bin/env bash
# Builds and runs the standalone target JVM used to exercise hawtio-plugin's local
# dev harness (`npm start`) against real perfmon4j MBeans, without WildFly.
#
# Prerequisites:
#   - `mvn clean install` (or at least `mvn compile`) already run in ../../base, so
#     ../../base/target/classes exists.
#   - A Jolokia JVM agent jar with the "javaagent" classifier, version 2.x+ (NOT the
#     old jolokia-jvm 1.x artifact - its response format doesn't match the jolokia.js
#     client bundled in this project's @hawtio/react version, and reads will silently
#     resolve as `undefined` instead of throwing). Fetch one into the local Maven repo
#     with, e.g.:
#       mvn dependency:get -Dartifact=org.jolokia:jolokia-agent-jvm:2.1.0:jar:javaagent
#
# Usage:
#   ./run.sh [port]        # defaults to 8778
#
# Once running, `npm start`'s webpack devServer.proxy config (see webpack.config.js)
# forwards /jolokia and /hawtio/jolokia to this JVM automatically - no Hawtio "Connect"
# step needed.

set -euo pipefail

PORT="${1:-8778}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_CLASSES="$SCRIPT_DIR/../../base/target/classes"
JOLOKIA_JAR="$(find ~/.m2/repository/org/jolokia/jolokia-agent-jvm -name '*-javaagent.jar' 2>/dev/null | sort -V | tail -1)"

if [ ! -d "$BASE_CLASSES" ]; then
  echo "error: $BASE_CLASSES not found - run 'mvn clean install' in base/ first" >&2
  exit 1
fi

if [ -z "$JOLOKIA_JAR" ]; then
  echo "error: no org.jolokia:jolokia-agent-jvm:*:javaagent jar found in ~/.m2/repository" >&2
  echo "fetch one with: mvn dependency:get -Dartifact=org.jolokia:jolokia-agent-jvm:2.1.0:jar:javaagent" >&2
  exit 1
fi

echo "Using Jolokia agent: $JOLOKIA_JAR"
javac -cp "$BASE_CLASSES" -d "$SCRIPT_DIR" "$SCRIPT_DIR/TargetMain.java"
exec java -cp "$SCRIPT_DIR:$BASE_CLASSES" \
  -javaagent:"$JOLOKIA_JAR=port=$PORT,host=localhost" \
  TargetMain
