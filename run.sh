#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

# Build if JAR does not exist or classes are missing
if [ ! -f "build/libs/snet-viewer.jar" ]; then
    bash "$DIR/build.sh"
fi

echo "Launching SnetViewer..."
java -jar build/libs/snet-viewer.jar "$@"
