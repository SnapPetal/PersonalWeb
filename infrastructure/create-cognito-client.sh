#!/bin/bash
set -e

USER_POOL_ID="us-east-1_pHStskbGS"
REGION="us-east-1"

echo "Creating new app client for personal-web..."
CLIENT_OUTPUT=$(aws cognito-idp create-user-pool-client \
  --user-pool-id "$USER_POOL_ID" \
  --client-name personal-web-oauth \
  --generate-secret \
  --callback-urls "http://localhost:8080/login/oauth2/code/cognito" "https://thonbecker.com/login/oauth2/code/cognito" \
  --logout-urls "http://localhost:8080/" "https://thonbecker.com/" \
  --allowed-oauth-flows-user-pool-client \
  --allowed-oauth-flows "code" \
  --allowed-oauth-scopes "openid" "profile" "email" \
  --supported-identity-providers "COGNITO" \
  --region "$REGION" \
  --output json)

CLIENT_ID=$(echo "$CLIENT_OUTPUT" | jq -r '.UserPoolClient.ClientId')
CLIENT_SECRET=$(echo "$CLIENT_OUTPUT" | jq -r '.UserPoolClient.ClientSecret')

echo ""
echo "=========================================="
echo "✅ Personal Web OAuth Client Created!"
echo "=========================================="
echo ""
echo "Add these to your .env file:"
echo ""
echo "COGNITO_USER_POOL_ID=$USER_POOL_ID"
echo "COGNITO_CLIENT_ID=$CLIENT_ID"
echo "COGNITO_CLIENT_SECRET=$CLIENT_SECRET"
echo "AWS_REGION=$REGION"
echo ""
echo "Issuer URI: https://cognito-idp.$REGION.amazonaws.com/$USER_POOL_ID"
echo "Login URL: https://thonbecker-biz.auth.$REGION.amazoncognito.com/login?client_id=$CLIENT_ID&response_type=code&scope=openid+profile+email&redirect_uri=http://localhost:8080/login/oauth2/code/cognito"
echo ""
echo "=========================================="
