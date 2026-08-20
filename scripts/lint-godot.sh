#!/usr/bin/env bash

set -euo pipefail

line_length=100

mapfile -t gd_files < <(find godot -type f -name '*.gd' -print | sort)
if ((${#gd_files[@]} == 0)); then
  echo "No GDScript files found."
  exit 0
fi

gdlint "${gd_files[@]}"
gdformat --line-length="$line_length" --check "${gd_files[@]}"
