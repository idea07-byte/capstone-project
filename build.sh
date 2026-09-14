#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "      Building BuyIt Full-Stack Marketplace       "
echo "=================================================="

# 1. Build React Frontend
if [ -d "frontend" ] && [ -f "frontend/package.json" ]; then
    echo "[1/2] Building React Frontend..."
    cd frontend
    if [ -f "package-lock.json" ]; then
        npm ci || npm install
    else
        npm install
    fi
    npm run build
    cd "$SCRIPT_DIR"
else
    echo "[1/2] Skipping Frontend build (directory not found)..."
fi

# 2. Build Java Backend
echo "[2/2] Building Java Backend..."
LIB_DIR="backend/lib"
OUT_DIR="out"
RES_DIR="backend/resources"

mkdir -p "$LIB_DIR"
mkdir -p "$OUT_DIR"

# Download PostgreSQL JDBC driver if missing (ensures clean clone builds)
PG_JAR="$LIB_DIR/postgresql-42.7.4.jar"
if [ ! -f "$PG_JAR" ]; then
    echo "Downloading PostgreSQL JDBC driver..."
    curl -fsSL -o "$PG_JAR" "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar" || \
    wget -q -O "$PG_JAR" "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar"
fi

echo "Compiling Java source files..."
javac -cp "$PG_JAR" -d "$OUT_DIR" \
    backend/*.java \
    backend/model/*.java \
    backend/service/*.java \
    backend/db/*.java \
    backend/util/*.java

echo "Copying resources and libraries..."
mkdir -p "$OUT_DIR/resources"
mkdir -p "$OUT_DIR/lib"
if [ -d "$RES_DIR" ]; then
    cp -r "$RES_DIR"/* "$OUT_DIR/resources/" 2>/dev/null || true
    if [ -f "$RES_DIR/database.properties" ]; then
        cp "$RES_DIR/database.properties" "$OUT_DIR/database.properties"
    fi
fi
cp "$LIB_DIR"/*.jar "$OUT_DIR/lib/" 2>/dev/null || true

echo "Creating executable buyit.jar..."
echo "Main-Class: Main" > "$OUT_DIR/manifest.txt"
echo "Class-Path: lib/postgresql-42.7.4.jar resources/" >> "$OUT_DIR/manifest.txt"
jar cfm "$OUT_DIR/buyit.jar" "$OUT_DIR/manifest.txt" -C "$OUT_DIR" .
rm -f "$OUT_DIR/manifest.txt"

echo "=================================================="
echo "[SUCCESS] BuyIt Marketplace build completed!"
echo "To launch the application, run: ./run.sh"
echo "=================================================="
