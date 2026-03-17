# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Run the application (dev profile uses local Docker PostgreSQL)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run all tests
mvn test

# Run integration/verification tests
mvn verify

# Run a single test class
mvn test -Dtest=ModuleStructureTest

# Run module integration tests (requires Docker for Testcontainers)
mvn test -Dtest="BookingModuleTest,NotificationModuleTest"

# Apply code formatting (must pass before committing)
mvn spotless:apply

# Check formatting without applying
mvn spotless:check

# Build production jar
mvn clean package

# Build Docker image (used by CI)
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=personal
```

## Local Development Setup

1. Copy `.env.example` to `.env` and fill in AWS credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`).
2. Add Cognito OAuth2 credentials to `.env`:

   ```bash
   COGNITO_USER_POOL_ID=us-east-1_pHStskbGS
   COGNITO_CLIENT_ID=7vaut06m1c699il4qo6ri00724
   COGNITO_CLIENT_SECRET=llh0jo5r7v6tjsodufvc24lnfcouegmq5b6ktjsqtfhq4r3gpd6
   ```
3. Spring Boot Docker Compose integration auto-starts PostgreSQL from `docker-compose.yml` when running locally—no manual `docker-compose up` needed.
4. The `dev` profile (`application-dev.yml`) overrides the datasource to `localhost:5432/dbmaster` with user `dbmasteruser` and no password.
5. **Hot Reload**: Spring Boot DevTools is enabled in the `dev` profile for automatic restart when files change:
   - Thymeleaf templates (`src/main/resources/templates/`)
   - Static files (`src/main/resources/static/`)
   - Configuration files (`src/main/resources/application*.yml`)
   - LiveReload is enabled for automatic browser refresh (requires browser extension or IDE support)

## Authentication

### AWS Cognito OAuth2

The application uses **AWS Cognito** for user authentication via Spring Security OAuth2 Client.

**User Pool Configuration:**
- Pool ID: `us-east-1_pHStskbGS` (thonbecker-biz-users)
- Domain: `thonbecker-biz.auth.us-east-1.amazoncognito.com`
- App Client: `7vaut06m1c699il4qo6ri00724` (personal-web-oauth)
- Callback URLs: `http://localhost:8080/login/oauth2/code/cognito`, `https://thonbecker.com/login/oauth2/code/cognito`
- Logout URLs: `http://localhost:8080/`, `https://thonbecker.com/`
- OAuth Scopes: `openid`, `profile`, `email`

**Protected Endpoints:**
- `/landscape/plans/**` - Requires authentication (landscape plan CRUD operations)
- `/booking/admin/**` - Requires authentication (booking administration)
- All other endpoints are public by default

**Login Flow:**
1. User clicks "Login" button on landscape planner page
2. Redirects to Cognito hosted UI: `/oauth2/authorization/cognito`
3. User authenticates with Cognito
4. Redirects back to `/landscape` with OAuth code
5. Spring Security exchanges code for tokens
6. User session established, `principal.getName()` returns user email

**Create Test User:**

```bash
aws-vault exec thonbecker -- aws cognito-idp admin-create-user \
  --user-pool-id us-east-1_pHStskbGS \
  --username your-email@example.com \
  --user-attributes Name=email,Value=your-email@example.com Name=email_verified,Value=true \
  --temporary-password TempPass123! \
  --region us-east-1
```

**Thymeleaf Security Integration:**
- Dependency: `thymeleaf-extras-springsecurity6`
- Use `sec:authorize="isAuthenticated()"` to show/hide elements
- Use `sec:authentication="name"` to display user email

**GitHub Secrets (for production):**

```bash
COGNITO_USER_POOL_ID
COGNITO_CLIENT_ID
COGNITO_CLIENT_SECRET
```

## Architecture

### Spring Modulith Modular Monolith

The application is a **modular monolith** enforced by Spring Modulith. All modules are **closed** by default — sub-packages are internal unless explicitly exported via `@NamedInterface`. Module boundaries are validated by `ModuleStructureTest`.

Cross-module communication is **event-driven**:
- Each module publishes domain events from its `api/` package
- Consuming modules (like `notification`) declare dependencies on specific `api` named interfaces
- No module calls another module's service directly — events are the only cross-module contract

Each module follows this internal package convention:
- `api/` — `@NamedInterface("api")` exported package containing domain events, value objects, and event listeners
- `domain/` — Domain model objects (pure Java, no persistence annotations)
- `platform/` — All implementation details: services, persistence entities, repositories, web controllers

### Modules

|     Module     |       Service        |                                  Purpose                                   |
|----------------|----------------------|----------------------------------------------------------------------------|
| `foosball`     | `FoosballService`    | Table soccer game tracking, stats, tournaments, ELO rating                 |
| `trivia`       | `TriviaService`      | AI-powered FPU trivia, WebSocket multiplayer                               |
| `skatetricks`  | `SkateTricksService` | YOLO pose estimation + Bedrock AI trick detection                          |
| `landscape`    | `LandscapeService`   | AI-powered landscape planning with USDA plant database integration         |
| `booking`      | `BookingService`     | Appointment scheduling with auto-availability, event publishing            |
| `tankgame`     | _(self-contained)_   | WebSocket tank game with player progression                                |
| `user`         | `UserService`        | User management                                                            |
| `notification` | _(event-driven)_     | Email notifications via event subscribers (zero coupling to other modules) |
| `content`      | _(self-contained)_   | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter            |
| `shared`       | _(config only)_      | Infrastructure configuration classes (Security, Cache, AWS, WebSocket)     |

### Module Boundaries

All modules use `@ApplicationModule` with **closed** boundaries (no `Type.OPEN`). The `api/` sub-package is exported via `@NamedInterface("api")` for modules that publish events consumed by other modules.

**Dependency declarations** use `:: api` syntax to restrict access to only the exported interface:

```java
@ApplicationModule(
    displayName = "Notification Services",
    allowedDependencies = {"shared", "booking :: api", "trivia :: api", "foosball :: api", "user :: api"})
```

### Domain Events

Each module owns the events it publishes in its `api/` package. Events are immutable records containing ALL data needed for processing (no callbacks to the source module).

|           Event           | Published By |      Consumed By       |            Purpose             |
|---------------------------|--------------|------------------------|--------------------------------|
| `BookingCreatedEvent`     | booking      | notification           | Send confirmation email        |
| `BookingCancelledEvent`   | booking      | notification           | Send cancellation email        |
| `GameRecordedEvent`       | foosball     | notification (logging) | Log game activity              |
| `PlayerCreatedEvent`      | foosball     | notification (logging) | Log player creation            |
| `QuizStartedEvent`        | trivia       | notification (logging) | Log quiz start                 |
| `QuizCompletedEvent`      | trivia       | notification (logging) | Log quiz completion            |
| `PlayerJoinedQuizEvent`   | trivia       | notification (logging) | Log player join                |
| `UserRegisteredEvent`     | user         | notification (logging) | Log registration               |
| `UserLoginEvent`          | user         | notification (logging) | Log login                      |
| `UserProfileUpdatedEvent` | user         | notification (logging) | Log profile update             |
| `TrickAnalysisEvent`      | skatetricks  | _(none)_               | Published but no consumers yet |

### Event Publication Registry

Booking events use `@TransactionalEventListener` backed by Spring Modulith's **JPA event publication registry** (`event_publication` table). This provides guaranteed delivery — if a listener fails, the event persists with `completion_date = null` and can be replayed.

Other event listeners (logging in notification module) use `@EventListener` since missed log entries are non-critical.

**Dependencies:**
- `spring-modulith-starter-jpa` — enables the JPA event publication registry at runtime
- `spring-modulith-core` — module structure enforcement
- `spring-modulith-starter-test` — `@ApplicationModuleTest` and `Scenario` API for testing

### Key Technical Patterns

- **Database schema**: Managed exclusively by Liquibase (`src/main/resources/db/changelog/`). Hibernate DDL is set to `none`. **CRITICAL**: Never modify or delete existing changesets once they have been merged — Liquibase tracks applied changesets by ID and checksum. Altering a merged changeset will cause checksum validation failures on startup. Always create a new changeset file for schema changes.
- **Caching**: Caffeine (`CacheConfig`). Used for Bible verse, Dad joke responses, plant data, plant images (24-hour TTL).
- **Retry**: Spring Retry (`RetryConfig`) for fault-tolerant external API calls.
- **Scheduled jobs**: ShedLock (`ShedlockConfig`) prevents duplicate execution in distributed environments.
- **AI**: Spring AI with AWS Bedrock — Converse API (inference profile `us.anthropic.claude-sonnet-4-6`) for chat/vision, Titan Text Embeddings V2 (`amazon.titan-embed-text-v2:0`) via Spring AI `EmbeddingModel` for vector embeddings. Amazon Nova Canvas (`amazon.nova-canvas-v1:0`) via `BedrockRuntimeClient` for image generation. DJL (Deep Java Library) with PyTorch for local YOLO pose estimation in skatetricks.
- **Image Generation**: `LandscapeImageGenerationService` uses Nova Canvas `TEXT_IMAGE` with `CANNY_EDGE` conditioning to generate seasonal landscape variations from the user's photo. Images are resized to fit the 4,194,304 pixel limit before sending.
- **WebSockets**: STOMP over SockJS for trivia and tank game real-time communication. Skatetricks video conversion uses WebSocket for progress updates, but analysis uses HTTP polling to avoid timeout issues with long-running AI inference.
- **Async processing**: Skatetricks uses async endpoints with status polling for video conversion and analysis. Long-running operations (30+ seconds) are processed in background threads via `ExecutorService`. Client polls status endpoints (GET `/skatetricks/convert/{id}/status`, `/skatetricks/analyze/{id}/status`) every 2 seconds. YOLO models pre-load at startup (`@PostConstruct`) to prevent first-request timeouts.
- **Frontend**: Thymeleaf templates + HTMX + Alpine.js + Bootstrap 5. Frontend libraries served as WebJars. Fabric.js for interactive canvas (landscape plant placement). **IMPORTANT**: All static resource references (`/js/**`, `/css/**`) MUST use Thymeleaf expressions (`th:src="@{/js/...}"`, `th:href="@{/css/...}"`), never plain `src` or `href`. Spring Boot's content-based resource versioning (`spring.web.resources.chain.strategy.content`) appends MD5 hashes to URLs for cache busting across deployments.
- **Alpine.js**: Used for client-side reactivity across all modules. Components are registered via `Alpine.data()` inside an `alpine:init` event listener (e.g., `document.addEventListener("alpine:init", () => { Alpine.data("bibleVerse", () => ({...})); });`). This ensures components are registered with Alpine before DOM processing and avoids race conditions with `defer`-loaded Alpine. WebSocket/canvas/media code stays imperative within the component methods. Alpine.js is loaded via WebJar (`org.webjars.npm:alpinejs:3.14.9`) with `defer` attribute. Global CSS rule `[x-cloak] { display: none !important; }` in `main.css` prevents flash of unstyled content. Use `x-if` (not `x-show`) when multiple states should never coexist in the DOM simultaneously (e.g., loading spinner vs content).
- **HTMX + Alpine.js Coexistence**: HTMX handles server-rendered fragment swaps (foosball, plant search). Alpine.js handles client-side state (games, booking form, media). For pages using both (landscape planner), HTMX swaps raw HTML fragments and Alpine manages surrounding UI state. The `selectTimeSlot` function in booking is exposed to `window` via Alpine's `init()` for compatibility with server-rendered fragment `onclick` handlers.
- **Theme Toggle**: Sun/moon icon toggle in navbar (home page only) as Alpine `themeToggle()` component. Theme preference stored in localStorage (`darkMode: enabled/disabled`). Booking pages automatically apply the saved theme without showing the toggle.
- **CSRF**: Cookie-based CSRF tokens (`CookieCsrfTokenRepository`) with `CsrfTokenRequestAttributeHandler` for eager token generation (required in Spring Security 6+ where CSRF tokens are deferred by default). `csrf-utils.js` supports both meta tag and cookie-based token delivery with cookie as fallback. Alpine components on pages without meta tags (booking) read the token directly from the `XSRF-TOKEN` cookie.
- **Structured AI Output**: Skatetricks trick analyzer uses `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT` with Spring AI's `.entity()` for type-safe JSON responses from Claude via Bedrock Converse. `@JsonIgnoreProperties(ignoreUnknown = true)` on response schema records handles extra fields Claude may include. System prompts lead with "Your output MUST be ONLY a valid JSON object" to prevent text-before-JSON responses.

### Testing

#### Module Integration Tests

Use `@IntegrationTest` (a custom meta-annotation) for module-level integration tests with Spring Modulith's `Scenario` API:

```java
@IntegrationTest
class BookingModuleTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void publishesBookingCreatedEvent(Scenario scenario) {
        scenario.stimulate(() -> bookingService.createBooking(...))
            .andWaitForEventOfType(BookingCreatedEvent.class)
            .toArriveAndVerify(event -> {
                assertThat(event.attendeeEmail()).isNotBlank();
            });
    }
}
```

`@IntegrationTest` combines:
- `@ApplicationModuleTest(extraIncludes = "shared")` — boots only the module under test + shared config
- `@Import(TestcontainersConfig.class)` — Testcontainers PostgreSQL + stub OAuth2 `ClientRegistrationRepository`
- `@ActiveProfiles("test")` — disables Docker Compose, stubs AWS credentials

**Test infrastructure files:**
- `TestcontainersConfig` — PostgreSQL container (`postgres:18.1`) with `@ServiceConnection` + stub OAuth2
- `IntegrationTest` — meta-annotation combining all test setup
- `application-test.yml` — disables Docker Compose, provides dummy AWS credentials

#### Module Structure Tests

`ModuleStructureTest` verifies module boundaries, generates C4 diagrams, and prints module structure. Runs without a database (no Spring context).

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

#### Inference Profiles (Required)

**CRITICAL**: AWS Bedrock now requires using **inference profiles** instead of direct model IDs for on-demand throughput. Direct model invocation (e.g., `anthropic.claude-sonnet-4-6`) will fail with:

```
ValidationException: Invocation of model ID anthropic.claude-sonnet-4-6 with on-demand throughput isn't supported.
Retry your request with the ID or ARN of an inference profile that contains this model.
```

**Working inference profile IDs** (verified with `aws bedrock list-inference-profiles`):
- **Claude Sonnet 4.6**: `us.anthropic.claude-sonnet-4-6` (US multi-region) or `global.anthropic.claude-sonnet-4-6` (global)
- **Claude Opus 4.6**: `us.anthropic.claude-opus-4-6-v1` (US multi-region) or `global.anthropic.claude-opus-4-6-v1` (global)
- **Claude Sonnet 4.5**: `us.anthropic.claude-sonnet-4-5-20250929-v1:0` (US multi-region)
- **Claude Haiku 4.5**: `us.anthropic.claude-haiku-4-5-20251001-v1:0` (US multi-region)

**US inference profiles** route requests across us-east-1, us-east-2, and us-west-2 for automatic failover.
**Global inference profiles** route requests globally across all supported AWS regions.

**To list available inference profiles**:

```bash
aws-vault exec <profile> -- aws bedrock list-inference-profiles \
  --region us-east-1 --output json | jq '.inferenceProfileSummaries[] | select(.inferenceProfileName | contains("Claude"))'
```

**Current configuration** (`application.yml`):

```yaml
spring:
  ai:
    bedrock:
      converse:
        chat:
          enabled: true
          options:
            model: us.anthropic.claude-sonnet-4-6  # Inference profile ID (NOT direct model ID)
            temperature: 0.3
            max-tokens: 4096
      titan:
        embedding:
          model: amazon.titan-embed-text-v2:0
          input-type: TEXT  # CRITICAL: defaults to IMAGE if omitted
      aws:
        region: ${AWS_REGION:us-east-1}
        access-key: ${AWS_ACCESS_KEY_ID}
        secret-key: ${AWS_SECRET_ACCESS_KEY}
    model:
      embedding: bedrock-titan  # Activates Spring AI Titan embedding auto-config
```

#### Spring AI Bedrock Embedding (Titan V2)

Spring AI provides `EmbeddingModel` auto-configuration for Bedrock Titan via `spring-ai-starter-model-bedrock`. The `EmbeddingService` in `skatetricks` wraps this to produce `List<Float>` vectors for S3 Vectors storage.

**Dependencies** (both required):
- `spring-ai-starter-model-bedrock` — Titan embedding auto-config
- `spring-ai-starter-model-bedrock-converse` — Claude chat via Converse API

**Gotchas:**
- `input-type` defaults to `IMAGE` in `BedrockTitanEmbeddingProperties`, NOT `TEXT`. Always set `spring.ai.bedrock.titan.embedding.input-type=TEXT` for text embeddings, or Titan will try to base64-decode your text as an image.
- `model` and `input-type` are direct properties under `spring.ai.bedrock.titan.embedding`, NOT nested under `options:`.
- Spring Boot 4 uses Jackson 3 (`tools.jackson`), but Spring AI 2.0.0-M2 requires a Jackson 2.x `com.fasterxml.jackson.databind.ObjectMapper` bean. `AwsConfig` provides this explicitly.

#### Bedrock Image Generation (Nova Canvas)

The `LandscapeImageGenerationService` uses **Amazon Nova Canvas** (`amazon.nova-canvas-v1:0`) directly via the AWS SDK `BedrockRuntimeClient` (not Spring AI — Spring AI 2.0.0-M2 does not have a Bedrock image generation provider).

**Usage pattern:**

```java
// TEXT_IMAGE with CANNY_EDGE conditioning preserves the structure of the input image
Map.of(
    "taskType", "TEXT_IMAGE",
    "textToImageParams", Map.of(
        "text", prompt,
        "conditionImage", base64Image,
        "controlMode", "CANNY_EDGE",
        "controlStrength", 0.8),
    "imageGenerationConfig", Map.of(
        "width", 1024, "height", 1024,
        "quality", "standard", "numberOfImages", 1));
```

**Key constraints:**
- Input image must be ≤ 4,194,304 pixels (2048x2048). `LandscapeImageGenerationService.resizeIfNeeded()` handles this.
- `amazon.nova-canvas-v1:0` must be enabled in the Bedrock Model Access console in `us-east-1`.
- `BedrockRuntimeClient` bean is configured in `AwsConfig`.

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
// WRONG - Throws IllegalArgumentException: "The template string is not valid"
"""
Return JSON like this:
{
  "field": "value"
}
"""

// CORRECT - Escape braces in examples
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
- **Claude Sonnet 4.6** (inference profile `us.anthropic.claude-sonnet-4-6`) — visual trick analysis via Spring AI / Bedrock Converse API
- **Titan Text Embeddings V2** (`amazon.titan-embed-text-v2:0`) — 1024-dimension normalized float embeddings via Spring AI `EmbeddingModel` (auto-configured by `spring-ai-starter-model-bedrock`)

**Store flow** (verified attempt -> vector store):
1. Trick analyzed by Claude; YOLO pose data saved to `pose_data` column in DB via `SkateTricksService.saveResult()`
2. Auto-verified if `confidence >= 80`; otherwise surfaced to user for confirmation/correction
3. On verification: `writeToVectorStore()` embeds the **pose data text** (NOT trick name) via `EmbeddingService`, stores with `PutInputVector` keyed `attempt-{id}` and `Document` metadata (`trickName`, `confidence`, `formScore`, `attemptId`, `feedback`)
4. Attempts without pose data (e.g., direct video analysis path) are skipped for vector store writes

**IMPORTANT**: Both the stored vectors and query vectors must embed the **same type of data** (pose data). Previously, stored vectors used `"trick:{name} feedback:{feedback}"` while queries used pose data — this semantic mismatch made RAG retrieval meaningless. The fix embeds pose data for both storage and query.

**RAG query flow** (frame analysis path only):
1. `BedrockTrickAnalyzer.fetchSimilarExamples()` embeds the YOLO pose data text
2. Queries `queryVectors` top-3 by cosine similarity
3. Similar verified past attempts (with feedback from metadata) are injected as few-shot examples into Claude's system prompt

**Key classes:**
- `EmbeddingService` — wraps Spring AI `EmbeddingModel` (Titan Embed V2), returns `List<Float>` (1024 dims)
- `VectorStoreInitializer` — `@PostConstruct` creates the index (`DataType.FLOAT32`, `DistanceMetric.COSINE`, dim=1024); supports `recreate` flag for index reset
- `writeToVectorStore()` in `SkateTricksService` — embeds pose data, upserts (same key replaces on correction)
- `fetchSimilarExamples()` in `BedrockTrickAnalyzer` — RAG retrieval for frame analysis

**Configuration** (`application.yml`):

```yaml
skatetricks:
  vectorstore:
    bucket: thonbecker-vectors
    index: skatetricks-tricks
    dimension: 1024
    recreate: false  # Set to true to wipe and recreate the index on startup
```

**AWS prerequisite:** `amazon.titan-embed-text-v2:0` must be enabled in the Bedrock Model Access console in `us-east-1`.

**Known gap:** `analyzeVideo()` (direct video path) does not query the vector store and does not write to it — no pose data is available in that path.

#### Landscape Planning Module

The `landscape` module provides AI-powered landscape design with plant selection based on USDA hardiness zones.

**Architecture:**
- **Claude Sonnet 4.6** — analyzes landscape images to recommend suitable plants
- **Amazon Nova Canvas** — generates seasonal landscape variation images via `BedrockRuntimeClient`
- **USDA Plants Database API** — authoritative plant data with hardiness zone, light/water requirements
- **USDA Plants Gallery** — plant images fetched by USDA symbol (`PlantImageService`)
- **AWS S3 + CloudFront** — stores uploaded landscape images with CDN delivery
- **Fabric.js** — interactive canvas for visual plant placement with click-to-place markers

**Key features:**
1. **Image Upload**: Users upload photos of their yard (JPEG/PNG, max 100MB)
2. **AI Analysis**: Claude analyzes the image considering sunlight exposure, existing vegetation, space constraints
3. **Plant Search**: Search USDA Plants Database with filters for hardiness zone, sun/water requirements
4. **Interactive Plant Placement**: Fabric.js canvas — select a plant, click on the image to place it. Plant-type icons (tree, shrub, flower) with real plant photos from USDA gallery
5. **Seasonal AI Preview**: Claude generates text descriptions + Nova Canvas generates images showing the landscape in spring, summer, fall, winter
6. **Plan Management**: Save, load, and delete landscape plans with placements persisted to database
7. **Caching**: Plant data, search results, and plant images cached for 24 hours with Caffeine

**Key classes:**
- `LandscapeService` — coordinates image storage, AI analysis, plant search, placement, and seasonal preview
- `LandscapeAiService` — uses Claude for plant recommendations and seasonal text descriptions
- `LandscapeImageGenerationService` — uses Nova Canvas for seasonal image generation
- `PlantImageService` — fetches plant photos from USDA gallery by symbol, cached 24 hours
- `PlantApiService` — integrates with USDA Plants Database (with caching and retry logic)
- `LandscapeImageStorageService` — uploads images to S3 with timestamped keys

**Plant Image URL Pattern:**

```
https://plants.sc.egov.usda.gov/gallery/standard/{SYMBOL}_001_shp.jpg  (primary)
https://plants.sc.egov.usda.gov/gallery/pubs/{SYMBOL}_001_php.jpg     (fallback)
```

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

#### Booking Module

The `booking` module provides appointment scheduling with event-driven notifications.

**Architecture:**
- **Event-Driven Notifications** — publishes `BookingCreatedEvent` and `BookingCancelledEvent` from `booking.api`; notification module handles email sending via `@TransactionalEventListener`
- **Event Publication Registry** — `spring-modulith-starter-jpa` persists events to `event_publication` table for guaranteed delivery and replay
- **Automatic Availability Generation** — `AvailabilityScheduler` creates recurring weekly slots (Monday-Friday, 11 AM-12 PM and 6-9 PM) for 4 weeks ahead
- **iCal4j Library** — generates RFC-compliant `.ics` calendar files with Central Time (America/Chicago) timezone

**Key classes:**
- `BookingService` — booking creation, cancellation, availability management, event publishing
- `AvailabilityScheduler` — generates recurring weekly availability slots automatically
- `BookingController` — public endpoints for booking creation and management
- `BookingAdminController` — protected admin endpoints for system configuration

**Domain Events** (in `booking.api`):
- `BookingCreatedEvent` — contains all booking details (attendee info, times, confirmation code, message)
- `BookingCancelledEvent` — contains cancellation details for notification

#### Notification Module

The `notification` module is **fully event-driven** with no public service API.

**Architecture:**
- **Zero Coupling** — depends only on event types from other modules' `api` packages via `@NamedInterface`
- **`@TransactionalEventListener`** — for booking events (guaranteed delivery via event publication registry)
- **`@EventListener` + `@Async`** — for logging events (non-critical, fire-and-forget)
- **Email Service** — currently logs to console; ready for AWS SES integration

**Key classes:**
- `NotificationEventListener` — handles `BookingCreatedEvent` and `BookingCancelledEvent` with `@TransactionalEventListener`
- `EventLoggingListener` — logs events from trivia, foosball, user modules with `@Async`
- `EmailNotificationService` — formats booking confirmation/cancellation emails
- `CalendarService` — generates `.ics` calendar attachments from event data

#### Shared Module

The `shared` module provides **infrastructure configuration only** — no domain events, no business logic.

**Contents:**
- `shared/platform/configuration/` — SecurityConfig, CacheConfig, AwsConfig, WebSocketConfig, AsyncConfig, RetryConfig, JpaConfig, ShedlockConfig

### Future Enhancements

#### Landscape Module Enhancements

- **PDF Export**: Generate printable landscape plans with plant lists, care instructions, and layout diagrams
- **Plant Compatibility Matrix**: Analyze companion planting and suggest compatible plant combinations
- **Seasonal Bloom Timeline**: Show when plants bloom throughout the year with a visual calendar
- **Cost Estimation**: Calculate estimated costs based on plant quantities and local nursery pricing
- **Maintenance Calendar**: Generate a yearly maintenance schedule for the selected plants
- **Drag-and-Drop Repositioning**: Allow dragging placed plant markers to reposition them on the canvas

#### General Features

- **Mobile App**: Native mobile applications for iOS and Android
- **Enhanced RAG Pipeline**: Expand S3 Vectors usage to other modules beyond skatetricks
- **Real-time Collaboration**: Add WebSocket support for collaborative landscape planning
- **Weather Integration**: Pull local weather data to inform plant recommendations
- **Garden Journal**: Track plant growth, add photos, and record observations over time

### Git Workflow

**IMPORTANT**: Always push commits to remote immediately after creating them. Local commits that aren't pushed won't be deployed, which can cause confusion when production doesn't reflect local fixes.

```bash
# After creating a commit, always push:
git push origin main
```

If the push is rejected due to remote changes, rebase and push:

```bash
git fetch origin
git rebase origin/main
git push origin main
```

### Deployment

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/aws-deploy.yml`), which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to **AWS Lightsail** container service (`personal-service`). The workflow automatically cleans up old container images (keeps 5 most recent) before pushing to prevent Lightsail's image storage limit.
