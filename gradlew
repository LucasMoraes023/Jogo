#!/bin/sh
# Bootstrap wrapper for environments where the standard wrapper JAR is not vendored.
set -eu
GRADLE_VERSION=8.9
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/bootstrap/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME_DIR/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME_DIR")"
  ZIP="${TMPDIR:-/tmp}/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else echo 'Erro: curl ou wget é necessário para baixar o Gradle.' >&2; exit 1; fi
  unzip -q "$ZIP" -d "$(dirname "$GRADLE_HOME_DIR")"
  rm -f "$ZIP"
fi
exec "$GRADLE_BIN" "$@"
