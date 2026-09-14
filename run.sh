#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "      Starting BuyIt Full-Stack Marketplace       "
echo "=================================================="

if [ -f "out/buyit.jar" ]; then
    java -Djava.awt.headless=true -cp "out/buyit.jar:backend/lib/postgresql-42.7.4.jar" Main "$@"
elif [ -d "out" ]; then
    java -Djava.awt.headless=true -cp "out:backend/lib/postgresql-42.7.4.jar" Main "$@"
else
    echo "[ERROR] No compiled classes found. Please run ./build.sh first."
    exit 1
fi
