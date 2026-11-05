#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "Building SnetViewer..."

# Create output dirs
mkdir -p build/classes
mkdir -p build/libs

# Compile Java sources
javac -d build/classes \
    src/main/java/xyz/hainaut/snetviewer/model/*.java \
    src/main/java/xyz/hainaut/snetviewer/service/*.java \
    src/main/java/xyz/hainaut/snetviewer/ui/*.java \
    src/main/java/xyz/hainaut/snetviewer/App.java

# Copy resources
cp -r src/main/resources/* build/classes/ 2>/dev/null || true

# Package JAR with Manifest
cat << 'EOF' > build/manifest.txt
Manifest-Version: 1.0
Main-Class: xyz.hainaut.snetviewer.App
EOF

jar cfm build/libs/snet-viewer.jar build/manifest.txt -C build/classes .

echo "Build successful! JAR created at build/libs/snet-viewer.jar"
