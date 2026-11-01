#!/usr/bin/env bash
# Smart Fire and Rescue Response System - Git Bash Runner

# Move to the directory where this script is located
cd "$(dirname "$0")"

echo "=========================================================="
echo " Starting Smart Fire & Rescue Response System..."
echo "=========================================================="

mkdir -p out
echo "[BUILD] Compiling Java source files..."
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar" -d out src/*.java
if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed. Please check errors above."
    exit 1
fi

# Run using semicolon classpath (required for Windows Java)
java -cp "out;lib/mysql-connector-j.jar" Main
