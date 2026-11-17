#!/usr/bin/env bash
# Usage: scripts/install-to-tv.sh <TV_IP_ADDRESS> <path/to/app.apk>
set -euo pipefail
[ $# -ge 2 ] || { echo "Usage: $0 TV_IP APK_PATH"; exit 1; }
adb connect "$1:5555"
adb -s "$1:5555" install -r "$2"      # -r keeps saved favorites/progress (same signing key required)
adb -s "$1:5555" shell am start -n com.dramaease.tv/.MainActivity
