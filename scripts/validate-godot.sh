#!/usr/bin/env bash

set -euo pipefail

log_file="$(mktemp)"
trap 'rm -f "$log_file"' EXIT

if ! godot --headless --path godot/tankgame --editor --quit >"$log_file" 2>&1; then
  cat "$log_file"
  exit 1
fi

cat "$log_file"
if grep -Eq "SCRIPT ERROR|Parse Error|Failed to load script" "$log_file"; then
  exit 1
fi
