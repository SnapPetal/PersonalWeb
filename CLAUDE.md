# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Run the application (dev profile uses local Docker PostgreSQL)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run all tests
./mvnw test

# Run integration/verification tests
./mvnw verify

# Run a single test class
./mvnw test -Dtest=ModuleStructureTest

# Apply code formatting (must pass before committing)
./mvnw spotless:apply

# Check formatting without applying
./mvnw spotless:check

# Build production jar
./mvnw clean package

# Build Docker image (used by CI)
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=personal
```

## Local Development Setup

1. Copy `.env.example` to `.env` and fill in AWS credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`).
2. Spring Boot Docker Compose integration auto-starts PostgreSQL from `docker-compose.yml` when running locally—no manual `docker-compose up` needed.
3. The `dev` profile (`application-dev.yml`) overrides the datasource to `localhost:5432/dbmaster` with user `dbmasteruser` and no password.

## Architecture

### Spring Modulith Modular Monolith

The application is a **modular monolith** enforced by Spring Modulith. Module boundaries are validated at startup and by `ModuleStructureTest`. Cross-module communication must go through public facades or Spring Application Events—never by directly referencing internal packages.

Each module follows this internal package convention:
- `api/` — Public facade interface and domain events exposed to other modules
- `domain/` — Domain model objects (pure Java, no persistence annotations)
- `platform/` — All implementation details: persistence entities, repositories, services, web controllers

### Modules

| Module | Public Facade | Purpose |
|---|---|---|
| `foosball` | `FoosballFacade` | Table soccer game tracking, stats, tournaments, ELO rating |
| `trivia` | `TriviaFacade` | AI-powered FPU trivia, WebSocket multiplayer |
| `skatetricks` | `SkateTricksFacade` | YOLO pose estimation + Bedrock AI trick detection |
| `tankgame` | `TankGameFacade` | WebSocket tank game with player progression |
| `user` | `UserFacade` | User management |
| `notification` | `NotificationFacade` | Notification delivery |
| `content` | _(no external facade)_ | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter |
| `shared` | _(configuration only)_ | SecurityConfig, CacheConfig, AwsConfig, WebSocketConfig, RetryConfig, ShedlockConfig |
| `platform` | _(internal)_ | EventLoggingListener for Spring Modulith events |

### Key Technical Patterns

- **Database schema**: Managed exclusively by Liquibase (`src/main/resources/db/changelog/`). Hibernate DDL is set to `none`. Add new changesets; never alter existing ones.
- **Caching**: Caffeine (`CacheConfig`). Used for Bible verse and Dad joke responses (24-hour TTL).
- **Retry**: Spring Retry (`RetryConfig`) for fault-tolerant external API calls.
- **Scheduled jobs**: ShedLock (`ShedlockConfig`) prevents duplicate execution in distributed environments.
- **AI**: Spring AI with AWS Bedrock Converse API (`us.anthropic.claude-opus-4-6-v1`) for trivia question generation. DJL (Deep Java Library) with PyTorch for local YOLO pose estimation in skatetricks.
- **WebSockets**: STOMP over SockJS for trivia and tank game real-time communication.
- **Frontend**: Thymeleaf templates + HTMX for partial page updates + Bootstrap 5. Frontend libraries served as WebJars.
- **CSRF**: Cookie-based CSRF tokens (`CookieCsrfTokenRepository`). All HTMX POST requests include the CSRF token from the cookie.

### Code Formatting

Spotless enforces formatting as part of the build (`spotless:check` runs on `mvn verify`):
- **Java**: Palantir Java Format
- **JavaScript** (`src/main/resources/static/js/*.js`): Prettier
- **Markdown**: Flexmark
- **POM**: SortPom (dependencies sorted by `scope,groupId,artifactId`)

Always run `./mvnw spotless:apply` before committing Java, JS, or Markdown changes.

### Deployment

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/aws-deploy.yml`), which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to **AWS Lightsail** container service (`personal-service`).
