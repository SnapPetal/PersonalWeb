#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export DEPLOY_HOST="${DEPLOY_HOST:-18.213.161.133}"
export DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
export SSH_KEY_PATH="${SSH_KEY_PATH:-${HOME}/.ssh/lightsail.pem}"
export IMAGE_REF="${IMAGE_REF:-public.ecr.aws/p0w8z2j2/personal:latest}"
export REMOTE_APP_DIR="${REMOTE_APP_DIR:-/home/ubuntu/nextcloud-aws}"
export REMOTE_COMPOSE_FILE="${REMOTE_COMPOSE_FILE:-docker-compose.yml}"
export REMOTE_COMPOSE_SERVICE="${REMOTE_COMPOSE_SERVICE:-personal-website}"

exec "${SCRIPT_DIR}/deploy-personalweb.sh"
