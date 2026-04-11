# Deployment

This repository builds and publishes the PersonalWeb container image from pushes to `main`.
The published image is:

- `public.ecr.aws/p0w8z2j2/personal:latest`

The GitHub Actions workflow in `.github/workflows/aws-deploy.yml` currently handles image build and push.
It does **not** SSH into the Lightsail Linux instance. The instance rollout is handled separately with the helper script below.

## Lightsail Linux Instance

Use [`scripts/deploy-personalweb.sh`](../scripts/deploy-personalweb.sh) to deploy the latest published image to the Lightsail host over SSH.

### Required inputs

- `DEPLOY_HOST`
  The SSH host or alias for the Lightsail instance.

### Optional inputs

- `DEPLOY_USER`
  SSH user. If omitted, the current user is used.
- `IMAGE_REF`
  Container image to deploy.
  Default: `public.ecr.aws/p0w8z2j2/personal:latest`
- `REMOTE_APP_DIR`
  Remote application directory for compose-based deployment.
  Default: `/opt/personalweb`
- `REMOTE_COMPOSE_FILE`
  Compose file path relative to `REMOTE_APP_DIR`.
  Default: `docker-compose.yml`
- `REMOTE_DEPLOY_COMMAND`
  Exact remote shell command to run instead of the default compose rollout.

## Default deployment mode

If `REMOTE_DEPLOY_COMMAND` is not provided, the script assumes the server uses `docker compose` and that the compose file consumes `PERSONALWEB_IMAGE`.

The default remote rollout is:

```bash
cd "$REMOTE_APP_DIR"
export PERSONALWEB_IMAGE="$IMAGE_REF"
docker compose -f "$REMOTE_COMPOSE_FILE" pull
docker compose -f "$REMOTE_COMPOSE_FILE" up -d
docker image prune -f
```

## Examples

Deploy the latest image with the default compose flow:

```bash
DEPLOY_HOST=personal-lightsail ./scripts/deploy-personalweb.sh
```

Deploy a specific image tag:

```bash
DEPLOY_HOST=personal-lightsail \
IMAGE_REF=public.ecr.aws/p0w8z2j2/personal:d052ba2 \
./scripts/deploy-personalweb.sh
```

Use an explicit remote command instead of compose:

```bash
DEPLOY_HOST=personal-lightsail \
REMOTE_DEPLOY_COMMAND='sudo systemctl restart personalweb' \
./scripts/deploy-personalweb.sh
```

## Recommended server setup

For the default compose mode, keep a compose file on the Lightsail host that references the image through an environment variable:

```yaml
services:
  personalweb:
    image: ${PERSONALWEB_IMAGE:-public.ecr.aws/p0w8z2j2/personal:latest}
```

That keeps the server-side deployment stable while allowing the image tag to be overridden from the script.
