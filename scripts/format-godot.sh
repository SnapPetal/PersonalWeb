#!/usr/bin/env bash

set -euo pipefail

mapfile -t gd_files < <(find godot -type f -name '*.gd' -print | sort)
if ((${#gd_files[@]} == 0)); then
  echo "No GDScript files found."
  exit 0
fi

gdformat "${gd_files[@]}"
