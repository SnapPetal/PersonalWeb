# Deployment

This repository builds and publishes the PersonalWeb container image from pushes to `main`.
The published image is:

- `public.ecr.aws/p0w8z2j2/personal:latest`

The GitHub Actions workflow in `.github/workflows/aws-deploy.yml` handles image build and push.
It authenticates to AWS with GitHub OIDC and does **not** SSH into the Lightsail Linux instance. The instance rollout is handled separately with the helper script below.

## GitHub Actions OIDC

The deploy workflow expects a GitHub secret named `AWS_ROLE_TO_ASSUME` that contains the IAM role ARN to assume through OIDC.

AWS trust policy for the role should restrict access to this repository and branch:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::664759038511:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:SnapPetal/PersonalWeb:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

The role also needs permissions to authenticate to ECR Public and push the image used by the deploy workflow.
At minimum, AWS requires `ecr-public:GetAuthorizationToken` and `sts:GetServiceBearerToken` for the login step, plus push permissions on the public repository.

## Lightsail Linux Instance

Use [`scripts/deploy-lightsail-personalweb.sh`](../scripts/deploy-lightsail-personalweb.sh) to deploy the latest published image to the Lightsail host over SSH.

Use [`scripts/deploy-personalweb.sh`](../scripts/deploy-personalweb.sh) for custom hosts, alternate compose paths, or other SSH targets.

Known production target from `nextcloud-aws`:

- SSH: `ssh -i ~/.ssh/lightsail.pem ubuntu@18.213.161.133`
- Remote app repo: `~/nextcloud-aws`
- Compose service: `personal-website`
- Published port: `127.0.0.1:3003 -> 8080`
- Public site: `https://thonbecker.biz`

### Domain split

The production host already runs host-managed nginx in the `nextcloud-aws` repository; do not add another Caddy or nginx container. The intended routing is:

- `thonbecker.biz` and `www.thonbecker.biz` serve the files from `static-site/` at `/var/www/thonbecker-static`.
- `booking.thonbecker.biz` proxies only booking and shared asset paths to `127.0.0.1:3003`.
- `app.thonbecker.biz` proxies the complete Spring Boot application to `127.0.0.1:3003`.

An nginx reference is available at [`deploy/lightsail/nginx-domains.conf.example`](../deploy/lightsail/nginx-domains.conf.example). The authoritative production virtual hosts must live in `nextcloud-aws/nginx` so its deployment workflow and Certbot manage them.

Deploy the apex static files independently with:

```bash
./scripts/deploy-lightsail-static.sh
```

Landscape projects use a secure, anonymous browser cookie as their owner. Set `PERSONAL_ADMIN_USERNAME` and `PERSONAL_ADMIN_PASSWORD` in the production compose environment for HTTP Basic protection of booking administration.

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
  Default: `~/nextcloud-aws`
- `REMOTE_COMPOSE_FILE`
  Compose file path relative to `REMOTE_APP_DIR`.
  Default: `docker-compose.yml`
- `REMOTE_COMPOSE_SERVICE`
  Compose service to refresh.
  Default: `personal-website`
- `REMOTE_DEPLOY_COMMAND`
  Exact remote shell command to run instead of the default compose rollout.

## Default deployment mode

If `REMOTE_DEPLOY_COMMAND` is not provided, the script assumes the server uses `docker compose` and that the compose file consumes `PERSONALWEB_IMAGE`.

The default remote rollout is:

```bash
cd "$REMOTE_APP_DIR"
docker pull "$IMAGE_REF"
docker compose -f "$REMOTE_COMPOSE_FILE" up -d --no-deps "$REMOTE_COMPOSE_SERVICE"
docker image prune -f
```

This matches the current `nextcloud-aws` production setup, where the `personal-website` service is pinned to `public.ecr.aws/p0w8z2j2/personal:latest` inside the compose file.

## Examples

Deploy the latest image to the current Lightsail host:

```bash
./scripts/deploy-lightsail-personalweb.sh
```

Deploy a specific image tag:

```bash
IMAGE_REF=public.ecr.aws/p0w8z2j2/personal:d052ba2 \
./scripts/deploy-lightsail-personalweb.sh
```

Deploy to the current production IP directly:

```bash
DEPLOY_HOST=18.213.161.133 \
DEPLOY_USER=ubuntu \
./scripts/deploy-lightsail-personalweb.sh
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
    env_file:
      - .env.personalweb
```

That keeps the server-side deployment stable while allowing the image tag to be overridden from the script.

The PersonalWeb container must receive these runtime AI variables:

```bash
PERSONAL_OPENAI_API_KEY=
PERSONAL_OPENAI_CHAT_MODEL=gpt-4o
PERSONAL_OPENAI_TRIVIA_MODEL=gpt-4o-mini
PERSONAL_OPENAI_EMBEDDING_MODEL=text-embedding-3-small
PERSONAL_OPENAI_EMBEDDING_DIMENSIONS=1024
PERSONAL_OPENAI_IMAGE_MODEL=dall-e-3
```

For production, store the OpenAI key in Secrets Manager as `personalweb/openai-api-key` from the HomeWeb CDK stack. The `nextcloud-aws` deployment runs `scripts/sync-personalweb-openai-secret.sh` before restarting containers, which writes `PERSONAL_OPENAI_API_KEY` and the default model variables into the private server-side `.env` consumed by compose. The Spring app still reads `PERSONAL_OPENAI_API_KEY`; Secrets Manager is the source of truth for the secret value.

The current production compose file in `nextcloud-aws` does not use an image variable for the personal site. It currently references:

```yaml
personal-website:
  image: public.ecr.aws/p0w8z2j2/personal:latest
```

That means the default deployment path here is intended for `latest`. If you want tagged-image rollouts from this repo, the remote compose file should be updated to accept a variable-driven image reference.
