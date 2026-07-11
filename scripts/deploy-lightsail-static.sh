#!/usr/bin/env bash
set -euo pipefail

DEPLOY_HOST="${DEPLOY_HOST:-18.213.161.133}"
DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
SSH_KEY_PATH="${SSH_KEY_PATH:-${HOME}/.ssh/lightsail.pem}"
REMOTE_STATIC_DIR="${REMOTE_STATIC_DIR:-/var/www/thonbecker-static}"

SSH_ARGS=(-o BatchMode=yes -i "${SSH_KEY_PATH}")

tar -czf - -C static-site . -C ../src/main/resources/static images/profile.png images/favicon.svg | ssh "${SSH_ARGS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" \
    "sudo install -d -o ${DEPLOY_USER} -g ${DEPLOY_USER} '${REMOTE_STATIC_DIR}' && tar -xzf - -C '${REMOTE_STATIC_DIR}'"

echo "Static site deployed to ${DEPLOY_HOST}:${REMOTE_STATIC_DIR}"
