#!/usr/bin/env bash
set -euo pipefail

DEPLOY_HOST="${DEPLOY_HOST:-}"
DEPLOY_USER="${DEPLOY_USER:-}"
IMAGE_REF="${IMAGE_REF:-public.ecr.aws/p0w8z2j2/personal:latest}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-~/nextcloud-aws}"
REMOTE_COMPOSE_FILE="${REMOTE_COMPOSE_FILE:-docker-compose.yml}"
REMOTE_COMPOSE_SERVICE="${REMOTE_COMPOSE_SERVICE:-personal-website}"
REMOTE_DEPLOY_COMMAND="${REMOTE_DEPLOY_COMMAND:-}"

if [[ -z "${DEPLOY_HOST}" ]]; then
  echo "DEPLOY_HOST is required" >&2
  exit 1
fi

SSH_TARGET="${DEPLOY_HOST}"
if [[ -n "${DEPLOY_USER}" ]]; then
  SSH_TARGET="${DEPLOY_USER}@${DEPLOY_HOST}"
fi

if [[ -n "${REMOTE_DEPLOY_COMMAND}" ]]; then
  REMOTE_COMMAND="${REMOTE_DEPLOY_COMMAND}"
else
  REMOTE_COMMAND=$(cat <<EOF
set -euo pipefail
cd "${REMOTE_APP_DIR}"
docker pull "${IMAGE_REF}"
docker compose -f "${REMOTE_COMPOSE_FILE}" up -d --no-deps "${REMOTE_COMPOSE_SERVICE}"
docker image prune -f
EOF
)
fi

echo "Deploying ${IMAGE_REF} to ${SSH_TARGET}"
ssh "${SSH_TARGET}" "${REMOTE_COMMAND}"
