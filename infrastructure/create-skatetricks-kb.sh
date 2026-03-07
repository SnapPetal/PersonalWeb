#!/bin/bash
set -e

REGION="us-east-1"
KB_NAME="skatetricks-verified-attempts"
KB_ROLE_NAME="BedrockKnowledgeBaseRole-SkateTricks"
S3_BUCKET="thonbecker-skatetricks-kb"
EMBEDDING_MODEL="amazon.titan-embed-text-v2:0"

echo "Creating S3 bucket for Knowledge Base documents..."
aws s3 mb s3://$S3_BUCKET --region $REGION 2>/dev/null || echo "Bucket already exists"

echo "Creating IAM role for Knowledge Base..."
ROLE_ARN=$(aws iam create-role \
  --role-name $KB_ROLE_NAME \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "bedrock.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }' \
  --query 'Role.Arn' \
  --output text 2>/dev/null || \
  aws iam get-role --role-name $KB_ROLE_NAME --query 'Role.Arn' --output text)

echo "Attaching S3 policy to role..."
aws iam put-role-policy \
  --role-name $KB_ROLE_NAME \
  --policy-name S3Access \
  --policy-document "{
    \"Version\": \"2012-10-17\",
    \"Statement\": [{
      \"Effect\": \"Allow\",
      \"Action\": [\"s3:GetObject\", \"s3:ListBucket\"],
      \"Resource\": [
        \"arn:aws:s3:::$S3_BUCKET\",
        \"arn:aws:s3:::$S3_BUCKET/*\"
      ]
    }]
  }"

echo "Attaching Bedrock model policy to role..."
aws iam put-role-policy \
  --role-name $KB_ROLE_NAME \
  --policy-name BedrockModelAccess \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": ["bedrock:InvokeModel"],
      "Resource": "arn:aws:bedrock:*::foundation-model/*"
    }]
  }'

echo "Waiting for role to propagate..."
sleep 10

echo "Creating Knowledge Base..."
KB_ID=$(aws bedrock-agent create-knowledge-base \
  --name $KB_NAME \
  --description "Verified skateboard trick attempts for RAG-enhanced detection" \
  --role-arn $ROLE_ARN \
  --knowledge-base-configuration '{
    "type": "VECTOR",
    "vectorKnowledgeBaseConfiguration": {
      "embeddingModelArn": "arn:aws:bedrock:'$REGION'::foundation-model/'$EMBEDDING_MODEL'"
    }
  }' \
  --storage-configuration '{
    "type": "OPENSEARCH_SERVERLESS",
    "opensearchServerlessConfiguration": {
      "collectionArn": "auto-create",
      "vectorIndexName": "skatetricks-index",
      "fieldMapping": {
        "vectorField": "embedding",
        "textField": "text",
        "metadataField": "metadata"
      }
    }
  }' \
  --region $REGION \
  --query 'knowledgeBase.knowledgeBaseId' \
  --output text)

echo "Knowledge Base created: $KB_ID"

echo "Creating Data Source..."
DS_ID=$(aws bedrock-agent create-data-source \
  --knowledge-base-id $KB_ID \
  --name skatetricks-s3-datasource \
  --data-source-configuration '{
    "type": "S3",
    "s3Configuration": {
      "bucketArn": "arn:aws:s3:::'$S3_BUCKET'"
    }
  }' \
  --region $REGION \
  --query 'dataSource.dataSourceId' \
  --output text)

echo "Data Source created: $DS_ID"

echo ""
echo "=========================================="
echo "✅ Bedrock Knowledge Base Created!"
echo "=========================================="
echo ""
echo "Add to application.yml:"
echo ""
echo "skatetricks:"
echo "  knowledgebase:"
echo "    id: $KB_ID"
echo "    bucket: $S3_BUCKET"
echo "    datasource-id: $DS_ID"
echo ""
echo "Add to .env:"
echo "SKATETRICKS_KB_ID=$KB_ID"
echo "SKATETRICKS_KB_BUCKET=$S3_BUCKET"
echo "SKATETRICKS_KB_DATASOURCE_ID=$DS_ID"
echo ""
echo "=========================================="
