#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maven_cache="${HOME}/.m2"

docker run --rm --ipc=host \
    -e PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    -v "${repository_root}:/work" \
    -v "${maven_cache}:/root/.m2" \
    -w /work \
    mcr.microsoft.com/playwright/java:v1.60.0-noble \
    mvn -B -Dtest=StaticSiteBrowserTest test
