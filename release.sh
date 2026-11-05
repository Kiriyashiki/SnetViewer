#!/usr/bin/env bash
#
# Build a production release archive for SnetViewer.
#
# The archive contains the runnable JAR plus launcher scripts for Linux,
# macOS, and Windows.
#
# Usage:
#   ./release.sh                 # use the version from pom.xml
#   ./release.sh 1.2.3           # build a specific version
#   ./release.sh v1.2.3 -o out   # custom output directory
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
APP_NAME="SnetViewer"
JAR_NAME="snet-viewer.jar"
BUILD_DIR="$ROOT_DIR/build/release"
DIST_DIR="$ROOT_DIR/dist"

usage() {
    cat <<'EOF'
Usage: ./release.sh [version] [options]

Options:
  -o, --output-dir DIR   Directory for the generated zip (default: ./dist)
  -h, --help             Show this help

If no version is given, the version is read from pom.xml.
EOF
}

VERSION=""

while [ "$#" -gt 0 ]; do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        -o|--output-dir)
            if [ "$#" -lt 2 ]; then
                echo "Error: $1 requires a directory argument." >&2
                exit 1
            fi
            DIST_DIR="$2"
            shift 2
            ;;
        -*)
            echo "Error: unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
        *)
            if [ -n "$VERSION" ]; then
                echo "Error: only one version may be specified." >&2
                exit 1
            fi
            VERSION="$1"
            shift
            ;;
    esac
done

if [ -z "$VERSION" ]; then
    VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$ROOT_DIR/pom.xml" | head -n 1)"
fi

# Normalize a leading "v" so the archive name is consistent with CI tags.
VERSION="${VERSION#v}"

if [ -z "$VERSION" ]; then
    echo "Error: could not determine a version. Pass one explicitly, e.g. ./release.sh 1.0.0" >&2
    exit 1
fi

if ! printf '%s' "$VERSION" | grep -Eq '^[A-Za-z0-9._-]+$'; then
    echo "Error: invalid version '$VERSION'. Use letters, numbers, dots, underscores, or dashes." >&2
    exit 1
fi

for tool in javac jar zip java; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Error: '$tool' was not found on PATH. A JDK 17+ and zip are required." >&2
        exit 1
    fi
done

# Resolve a relative output directory against the caller's working directory.
mkdir -p "$DIST_DIR"
DIST_DIR="$(cd "$DIST_DIR" >/dev/null 2>&1 && pwd)"

PKG_NAME="$APP_NAME-$VERSION"
STAGE_DIR="$BUILD_DIR/stage"
CLASSES_DIR="$BUILD_DIR/classes"
PKG_DIR="$STAGE_DIR/$PKG_NAME"
ARCHIVE_NAME="$PKG_NAME.zip"

echo "Building $APP_NAME $VERSION..."

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$PKG_DIR" "$DIST_DIR"

# Compile against Java 17 so the release runs on Java 17 and newer.
find "$ROOT_DIR/src/main/java" -name '*.java' -print | sort > "$BUILD_DIR/sources.txt"
if [ ! -s "$BUILD_DIR/sources.txt" ]; then
    echo "Error: no Java sources found under src/main/java." >&2
    exit 1
fi

javac --release 17 -encoding UTF-8 -d "$CLASSES_DIR" @"$BUILD_DIR/sources.txt"

# Copy classpath resources to the root of the JAR. The repository keeps some
# resources beside the package sources, so support both standard locations.
for resource_dir in \
    "$ROOT_DIR/src/main/resources" \
    "$ROOT_DIR/src/main/java/xyz/hainaut/snetviewer/resources"
do
    if [ -d "$resource_dir" ]; then
        cp -R "$resource_dir"/. "$CLASSES_DIR"/
    fi
done

cat > "$BUILD_DIR/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Main-Class: xyz.hainaut.snetviewer.App
Implementation-Title: $APP_NAME
Implementation-Version: $VERSION
EOF

jar cfm "$PKG_DIR/$JAR_NAME" "$BUILD_DIR/MANIFEST.MF" -C "$CLASSES_DIR" .

# Linux / macOS launcher.
cat > "$PKG_DIR/run-linux.sh" <<'LAUNCHER'
#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java 17 or newer is required, but 'java' was not found on PATH." >&2
    exit 1
fi

exec java -jar "snet-viewer.jar" "$@"
LAUNCHER

# macOS launcher; the .command extension makes it double-clickable in Finder.
cp "$PKG_DIR/run-linux.sh" "$PKG_DIR/run-macos.command"

# Windows launcher (written with CRLF line endings).
cat > "$PKG_DIR/run-windows.bat" <<'LAUNCHER'
@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo Error: Java 17 or newer is required, but 'java' was not found on PATH.
    pause
    exit /b 1
)

java -jar "snet-viewer.jar" %*
if errorlevel 1 pause
LAUNCHER

awk '{ sub(/\r$/, ""); printf "%s\r\n", $0 }' \
    "$PKG_DIR/run-windows.bat" > "$PKG_DIR/run-windows.bat.tmp"
mv "$PKG_DIR/run-windows.bat.tmp" "$PKG_DIR/run-windows.bat"

chmod +x "$PKG_DIR/run-linux.sh" "$PKG_DIR/run-macos.command"

# Build the distributable zip.
rm -f "$DIST_DIR/$ARCHIVE_NAME"
(
    cd "$STAGE_DIR"
    zip -q -r "$DIST_DIR/$ARCHIVE_NAME" "$PKG_NAME"
)

echo
echo "Release archive created:"
echo "  $DIST_DIR/$ARCHIVE_NAME"
echo
echo "Contents:"
unzip -l "$DIST_DIR/$ARCHIVE_NAME" | sed -n '1,20p'
