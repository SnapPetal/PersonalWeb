#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "$script_dir/.." && pwd)"
project_dir="$project_root/godot/tankgame"
output_dir="$project_root/src/main/resources/static/tankgame"

rm -rf "$output_dir"
mkdir -p "$output_dir"
godot --headless --path "$project_dir" --export-release "Web" "$output_dir/index.html"
