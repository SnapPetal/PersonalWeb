#!/usr/bin/env bash

set -euo pipefail

project_dir="godot/tankgame"
output_dir="src/main/resources/static/tankgame"
godot_data_dir="${TMPDIR:-/tmp}/personal-web-godot-data"

mkdir -p "$godot_data_dir/data" "$godot_data_dir/config" "$godot_data_dir/cache"
export XDG_DATA_HOME="$godot_data_dir/data"
export XDG_CONFIG_HOME="$godot_data_dir/config"
export XDG_CACHE_HOME="$godot_data_dir/cache"

rm -rf "$output_dir"
mkdir -p "$output_dir"
godot --headless --path "$project_dir" --export-release "Web" "$output_dir/index.html"
