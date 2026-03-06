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
| `landscape`    | `LandscapeFacade`      | AI-powered landscape planning with USDA plant database integration                   |
| `tankgame`     | `TankGameFacade`       | WebSocket tank game with player progression                                          |
| `user`         | `UserFacade`           | User management                                                                      |
| `notification` | `NotificationFacade`   | Notification delivery                                                                |
| `content`      | _(no external facade)_ | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter                      |
| `shared`       | _(configuration only)_ | SecurityConfig, CacheConfig, AwsConfig, WebSocketConfig, RetryConfig, ShedlockConfig |
| `platform`     | _(internal)_           | EventLoggingListener for Spring Modulith events                                      |

### Key Technical Patterns

- **Database schema**: Managed exclusively by Liquibase (`src/main/resources/db/changelog/`). Hibernate DDL is set to `none`. Add new changesets; never alter existing ones.
- **Caching**: Caffeine (`CacheConfig`). Used for Bible verse, Dad joke responses, and plant data (24-hour TTL).
- **Retry**: Spring Retry (`RetryConfig`) for fault-tolerant external API calls.
- **Scheduled jobs**: ShedLock (`ShedlockConfig`) prevents duplicate execution in distributed environments.
- **AI**: Spring AI with AWS Bedrock Converse API (`anthropic.claude-sonnet-4-6`) for trivia question generation and landscape recommendations. DJL (Deep Java Library) with PyTorch for local YOLO pose estimation in skatetricks.
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

### AWS Bedrock Configuration

#### Model Identifiers

AWS Bedrock model identifiers follow the pattern `anthropic.claude-<model>-<version>`. **Critical**: Not all models use the same versioning suffix.

**Working model identifiers** (verified with `aws bedrock list-foundation-models`):
- **Claude Sonnet 4.6**: `anthropic.claude-sonnet-4-6` (no version suffix)
- **Claude Opus 4.6**: `anthropic.claude-opus-4-6-v1` (has `-v1` suffix)
- **Claude Sonnet 4.5**: `anthropic.claude-sonnet-4-5-20250929-v1:0`
- **Claude Haiku 4.5**: `anthropic.claude-haiku-4-5-20251001-v1:0`

**To list available models**:

```bash
aws-vault exec <profile> -- aws bedrock list-foundation-models \
  --query 'modelSummaries[?contains(modelId, `anthropic`)].[modelId,modelName]' \
  --output table --region us-east-1
```

**Current configuration** (`application.yml`):

```yaml
spring:
  ai:
    bedrock:
      converse:
        chat:
          options:
            model: anthropic.claude-sonnet-4-6
            temperature: 0.3
            max-tokens: 1500
```

#### Spring AI Multimodal Messages (Images + Text)

When sending images to Claude via Spring AI, use the `UserMessage` builder pattern with `Media`:

```java
// Encode image to Base64
final var base64Image = Base64.getEncoder().encodeToString(imageData);

// Create Media object with image
final var imageMedia = Media.builder()
    .mimeType(MimeTypeUtils.IMAGE_JPEG)
    .data(base64Image)
    .build();

// Create UserMessage with both text and image
final var userMessage = UserMessage.builder()
    .text(promptText)
    .media(imageMedia)
    .build();

// Create Prompt and call model
final var prompt = new Prompt(List.of(userMessage));
final var response = chatModel.call(prompt).getResult().getOutput().getText();
```

**Important**: Do NOT use `PromptTemplate` with multimodal messages - use `String.format()` for variable substitution instead. The image must be included via `.media()` on the UserMessage builder.

**Correct imports**:

- `org.springframework.ai.content.Media` (not `.chat.messages.Media`)
- `org.springframework.ai.chat.messages.UserMessage`

#### Spring AI PromptTemplate Gotcha

When using Spring AI's `PromptTemplate` with JSON examples in prompts, **escape curly braces** to prevent them from being interpreted as template variables:

```java
// ❌ WRONG - Throws IllegalArgumentException: "The template string is not valid"
"""
Return JSON like this:
{
  "field": "value"
}
"""

// ✅ CORRECT - Escape braces in examples
"""
Return JSON like this:
{{
  "field": "value"
}}
"""
```

This applies to FinancialPeaceQuestionGenerator (trivia module). For image analysis, prefer `String.format()` with multimodal UserMessage (see above).

### Infrastructure Notes

#### S3 Vectors Integration (Skatetricks RAG Pipeline)

The `skatetricks` module uses **AWS S3 Vectors** as a vector store for Retrieval-Augmented Generation (RAG) to improve trick detection accuracy over time.

**Two-model architecture:**
- **Claude Sonnet 4.6** (`anthropic.claude-sonnet-4-6`) — visual trick analysis via Spring AI / Bedrock Converse API
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

#### Landscape Planning Module

The `landscape` module provides AI-powered landscape design with plant selection based on USDA hardiness zones. Users can upload images of their yard, receive personalized plant recommendations, and create annotated landscape plans.

**Architecture:**
- **Claude Sonnet 4.6** (`anthropic.claude-sonnet-4-6`) — analyzes landscape images to recommend suitable plants based on visible conditions, sunlight exposure, and hardiness zone compatibility
- **USDA Plants Database API** — authoritative plant data with hardiness zone, light/water requirements, and native status
- **AWS S3 + CloudFront** — stores uploaded landscape images with CDN delivery

**Key features:**
1. **Image Upload**: Users upload photos of their yard (JPEG/PNG, max 100MB)
2. **AI Analysis**: Claude analyzes the image considering sunlight exposure, existing vegetation, space constraints, and climate compatibility
3. **Plant Search**: Search USDA Plants Database with filters for hardiness zone, sun requirements, and water needs
4. **Plan Management**: Save, load, and share landscape plans with plant placements
5. **Caching**: Plant data and search results cached for 24 hours with Caffeine

**Database schema** (`landscape` schema):
- `landscape_plans` — plan metadata (user, name, description, image URLs, hardiness zone)
- `plant_placements` — user-placed plants on the image (coordinates, notes, quantity)
- `recommended_plants` — AI recommendations (plant info, reasoning, confidence score)

**Configuration** (`application.yml`):

```yaml
landscape:
  usda-api:
    base-url: https://plants.sc.egov.usda.gov/api
    timeout: 10000
  storage:
    bucket: cdn-page-stack-processedmediabucket446d3976-oonhpdwdpfzq
    cdn-domain: https://cdn.thonbecker.com
    folder-prefix: landscape-plans/
```

**Key classes:**
- `LandscapeFacade` — public API with 7 methods for plan creation, plant search, and placement management
- `LandscapeAiService` — uses Claude to analyze images and generate plant recommendations
- `PlantApiService` — integrates with USDA Plants Database (with caching and retry logic)
- `LandscapeImageStorageService` — uploads images to S3 with timestamped keys

**HTTP Client Pattern:**
Uses Spring's declarative HTTP client (`@GetExchange`) with WebClient backend for type-safe USDA API calls.

### Future Enhancements

The following features are planned for future development:

#### Landscape Module Enhancements

- **Canvas-based Plant Placement**: Implement Fabric.js for visual plant placement on images with drag-and-drop
- **PDF Export**: Generate printable landscape plans with plant lists, care instructions, and layout diagrams
- **Plant Compatibility Matrix**: Analyze companion planting and suggest compatible plant combinations
- **Seasonal Bloom Timeline**: Show when plants bloom throughout the year with a visual calendar
- **Cost Estimation**: Calculate estimated costs based on plant quantities and local nursery pricing
- **Maintenance Calendar**: Generate a yearly maintenance schedule for the selected plants
- **Community Sharing**: Allow users to share plans publicly and browse community-created designs
- **3D Visualization**: Integrate with 3D rendering libraries to show landscape maturity over time

#### General Features

- **Mobile App**: Native mobile applications for iOS and Android
- **Enhanced RAG Pipeline**: Expand S3 Vectors usage to other modules beyond skatetricks
- **Real-time Collaboration**: Add WebSocket support for collaborative landscape planning
- **Weather Integration**: Pull local weather data to inform plant recommendations
- **Garden Journal**: Track plant growth, add photos, and record observations over time

### Deployment

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/aws-deploy.yml`), which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to **AWS Lightsail** container service (`personal-service`).
