#!/usr/bin/env bash
set -euo pipefail

fail() {
    echo "Design-system validation failed: $1" >&2
    exit 1
}

required_files=(
    src/main/resources/static/css/core/tokens.css
    src/main/resources/static/css/core/base.css
    src/main/resources/static/css/core/components.css
    design-system/src/main/resources/design-system.css
    static-site/design-system.css
    static-site/site.css
)

for file in "${required_files[@]}"; do
    [[ -f "$file" ]] || fail "missing $file"
done

rg -q -- '--app-body-bg|--surface-page' src/main/resources/static/css/core/tokens.css || fail "app tokens are missing"
rg -q -- 'data-bs-theme="dark"' src/main/resources/static/css/core/tokens.css || fail "app dark theme selector is missing"
rg -q -- 'data-theme="dark"' static-site/design-system.css || fail "static-site dark theme selector is missing"
rg -q -- 'focus-visible|a:focus|button:focus' src/main/resources/static/css/core/base.css || fail "app focus styles are missing"
rg -q -- 'data-menu-toggle|id="primary-navigation"' static-site/index.html || fail "static-site navigation hooks are missing"

if rg -n 'style="' static-site --glob '*.html'; then
    fail "static-site HTML contains inline styles"
fi

echo "Design-system validation passed."
