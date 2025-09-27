# Environment Setup

## Development with Cognito

### 1. Copy the environment template
```bash
cp .env.example .env
```

### 2. Fill in your Cognito credentials in `.env`:
- `COGNITO_CLIENT_ID`: Your AWS Cognito App Client ID
- `COGNITO_CLIENT_SECRET`: Your AWS Cognito App Client Secret  
- `COGNITO_TOKEN_URL`: Your Cognito Token URL (format: https://your-domain.auth.region.amazoncognito.com/oauth2/token)

### 3. Run the application with dev profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Getting Cognito Credentials

1. **Log into AWS Console** → Navigate to Amazon Cognito
2. **Select your User Pool** (or create one)
3. **App Integration** → App Clients → Create or select your app client
4. **Note down**:
   - Client ID
   - Client Secret (if using confidential client)
   - Domain (under App Integration → Domain)

### Example Token URL Format:
```
https://your-cognito-domain.auth.us-east-1.amazoncognito.com/oauth2/token
```

### Security Notes:
- Never commit `.env` files to version control
- Use `.env.example` as a template for team members
- For production, use proper secret management (AWS Secrets Manager, etc.)