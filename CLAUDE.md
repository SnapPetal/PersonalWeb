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

|     Module     |     Public Facade      |                                       Purpose                                        |
|----------------|------------------------|--------------------------------------------------------------------------------------|
| `foosball`     | `FoosballFacade`       | Table soccer game tracking, stats, tournaments, ELO rating                           |
| `trivia`       | `TriviaFacade`         | AI-powered FPU trivia, WebSocket multiplayer                                         |
| `skatetricks`  | `SkateTricksFacade`    | YOLO pose estimation + Bedrock AI trick detection                                    |
| `tankgame`     | `TankGameFacade`       | WebSocket tank game with player progression                                          |
| `user`         | `UserFacade`           | User management                                                                      |
| `notification` | `NotificationFacade`   | Notification delivery                                                                |
| `content`      | _(no external facade)_ | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter                      |
| `shared`       | _(configuration only)_ | SecurityConfig, CacheConfig, AwsConfig, WebSocketConfig, RetryConfig, ShedlockConfig |
| `platform`     | _(internal)_           | EventLoggingListener for Spring Modulith events                                      |

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

Always run `mvn spotless:apply` before committing Java, JS, or Markdown changes.

### Java Coding Conventions

- Use `var` for local variable type inference when the type is clear from context.
- Declare local variables and parameters as `final` where they are not reassigned.
- Use `Objects.isNull()` and `Objects.nonNull()` instead of `== null` / `!= null` checks.

### Infrastructure Notes

#### S3 Vectors Integration (Skatetricks RAG Pipeline)

The `skatetricks` module uses **AWS S3 Vectors** as a vector store for Retrieval-Augmented Generation (RAG) to improve trick detection accuracy over time.

**Two-model architecture:**
- **Claude Opus 4.6** (`us.anthropic.claude-opus-4-6-v1`) — visual trick analysis via Spring AI / Bedrock Converse API
- **Titan Text Embeddings V2** (`amazon.titan-embed-text-v2:0`) — 1024-dimension normalized float embeddings via `BedrockRuntimeClient.invokeModel()` directly (no Spring AI wrapper)

**Store flow** (verified attempt → vector store):
1. Trick analyzed by Claude; result saved to DB via `SkateTricksFacadeImpl.saveResult()`
2. Auto-verified if `confidence >= 80`; otherwise surfaced to user for confirmation/correction
3. On verification: `writeToVectorStore()` embeds `"trick:{name} feedback:{feedback}"` via `EmbeddingService`, stores with `PutInputVector` keyed `attempt-{id}` and `Document` metadata (`trickName`, `confidence`, `formScore`, `attemptId`)

**RAG query flow** (frame analysis path only):
1. `BedrockTrickAnalyzer.fetchSimilarExamples()` embeds the YOLO pose data text
2. Queries `queryVectors` top-3 by cosine similarity
3. Similar verified past attempts are injected as few-shot examples into Claude's system prompt

**Key classes:**
- `EmbeddingService` — wraps Titan Embed V2, returns `List<Float>` (1024 dims)
- `VectorStoreInitializer` — `@PostConstruct` creates the index (`DataType.FLOAT32`, `DistanceMetric.COSINE`, dim=1024); silently skips `ConflictException`
- `writeToVectorStore()` in `SkateTricksFacadeImpl` — upserts (same key replaces on correction)
- `fetchSimilarExamples()` in `BedrockTrickAnalyzer` — RAG retrieval for frame analysis

**Configuration** (`application.yml`):
```yaml
skatetricks:
  vectorstore:
    bucket: thonbecker-vectors
    index: skatetricks-tricks
    dimension: 1024
```

**AWS prerequisite:** `amazon.titan-embed-text-v2:0` must be enabled in the Bedrock Model Access console in `us-east-1`.

**Known gap:** `analyzeVideo()` (direct video path) does not query the vector store — no pose data is available in that path to use as a query embedding.

### Deployment

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/aws-deploy.yml`), which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to **AWS Lightsail** container service (`personal-service`).
