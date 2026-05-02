# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

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

1. Copy `.env.example` to `.env` and fill in AWS credentials (`PERSONAL_AWS_ACCESS_KEY_ID`, `PERSONAL_AWS_SECRET_ACCESS_KEY`, `PERSONAL_AWS_REGION`), OpenAI credentials (`PERSONAL_OPENAI_API_KEY`), and Nextcloud credentials (`PERSONAL_NEXTCLOUD_USERNAME`, `PERSONAL_NEXTCLOUD_APP_PASSWORD`).
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
PERSONAL_COGNITO_USER_POOL_ID
PERSONAL_COGNITO_CLIENT_ID
PERSONAL_COGNITO_CLIENT_SECRET
PERSONAL_OPENAI_API_KEY
PERSONAL_NEXTCLOUD_USERNAME
PERSONAL_NEXTCLOUD_APP_PASSWORD
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

|     Module     |       Service        |                                Purpose                                 |
|----------------|----------------------|------------------------------------------------------------------------|
| `foosball`     | `FoosballService`    | Table soccer game tracking, stats, tournaments, ELO rating             |
| `trivia`       | `TriviaService`      | AI-powered FPU trivia, WebSocket multiplayer                           |
| `skatetricks`  | `SkateTricksService` | YOLO pose estimation + OpenAI vision trick detection                   |
| `landscape`    | `LandscapeService`   | AI-powered landscape planning with USDA plant database integration     |
| `booking`      | `BookingService`     | Appointment scheduling with auto-availability, event publishing        |
| `tankgame`     | _(self-contained)_   | WebSocket tank game with player progression                            |
| `user`         | `UserService`        | User management                                                        |
| `calendar`     | _(event-driven)_     | Nextcloud CalDAV integration, calendar sync for bookings               |
| `notification` | _(event-driven)_     | Email notifications via AWS SES (zero coupling to other modules)       |
| `content`      | _(self-contained)_   | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter        |
| `shared`       | _(config only)_      | Infrastructure configuration classes (Security, Cache, AWS, WebSocket) |

### Module Boundaries

All modules use `@ApplicationModule` with **closed** boundaries (no `Type.OPEN`). The `api/` sub-package is exported via `@NamedInterface("api")` for modules that publish events consumed by other modules.

**Dependency declarations** use `:: api` syntax to restrict access to only the exported interface:

```java
@ApplicationModule(
    displayName = "Notification Services",
    allowedDependencies = {"shared", "booking :: api", "trivia :: api", "foosball :: api", "user :: api"})
class NotificationModule {}
```

### Domain Events

Each module owns the events it publishes in its `api/` package. Events are immutable records containing ALL data needed for processing (no callbacks to the source module).

|                Event                | Published By |      Consumed By       |             Purpose              |
|-------------------------------------|--------------|------------------------|----------------------------------|
| `BookingCreatedEvent`               | booking      | notification, calendar | Send email + create CalDAV event |
| `BookingCancelledEvent`             | booking      | notification, calendar | Send email + delete CalDAV event |
| `BookingCancellationRequestedEvent` | calendar     | booking                | Cancel booking (calendar sync)   |
| `GameRecordedEvent`                 | foosball     | notification (logging) | Log game activity                |
| `PlayerCreatedEvent`                | foosball     | notification (logging) | Log player creation              |
| `QuizStartedEvent`                  | trivia       | notification (logging) | Log quiz start                   |
| `QuizCompletedEvent`                | trivia       | notification (logging) | Log quiz completion              |
| `PlayerJoinedQuizEvent`             | trivia       | notification (logging) | Log player join                  |
| `UserRegisteredEvent`               | user         | notification (logging) | Log registration                 |
| `UserLoginEvent`                    | user         | notification (logging) | Log login                        |
| `UserProfileUpdatedEvent`           | user         | notification (logging) | Log profile update               |
| `TrickAnalysisEvent`                | skatetricks  | _(none)_               | Published but no consumers yet   |

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
- **AI**: Spring AI 2.0.0-M5 with OpenAI for chat, vision, embeddings, structured output, and image generation. `EmbeddingService` uses Spring AI's `EmbeddingModel` with OpenAI embeddings for S3 Vectors storage. DJL (Deep Java Library) with PyTorch remains local for YOLO pose estimation in skatetricks.
- **Image Generation**: `LandscapeImageGenerationService` uses Spring AI's OpenAI `ImageModel` for seasonal landscape images. It returns base64 image data for the existing seasonal preview response contract.
- **WebSockets**: STOMP over SockJS for trivia and tank game real-time communication. Skatetricks video conversion uses WebSocket for progress updates, but analysis uses HTTP polling to avoid timeout issues with long-running AI inference.
- **Async processing**: Skatetricks uses async endpoints with status polling for video conversion and analysis. Long-running operations (30+ seconds) are processed in background threads via `ExecutorService`. Client polls status endpoints (GET `/skatetricks/convert/{id}/status`, `/skatetricks/analyze/{id}/status`) every 2 seconds. YOLO models pre-load at startup (`@PostConstruct`) to prevent first-request timeouts.
- **MediaConvert configuration**: Skatetricks transcoding requires `skatetricks.transcoding.mediaconvert-role-arn` (`SKATETRICKS_MEDIACONVERT_ROLE_ARN`). Do not set `SKATETRICKS_MEDIACONVERT_ENDPOINT`; `AwsMediaConvertVideoTranscoder` resolves the account-specific endpoint at runtime via `DescribeEndpoints`.
- **Video analysis frame sampling**: Uploaded/imported MP4 analysis extracts duration-aware frames server-side before OpenAI vision analysis. `skatetricks.analysis.max-frames` (`SKATETRICKS_ANALYSIS_MAX_FRAMES`) defaults to `24`.
- **Frontend**: Thymeleaf templates + HTMX + Alpine.js + Bootstrap 5. Frontend libraries served as WebJars. Fabric.js for interactive canvas (landscape plant placement). **IMPORTANT**: All static resource references (`/js/**`, `/css/**`) MUST use Thymeleaf expressions (`th:src="@{/js/...}"`, `th:href="@{/css/...}"`), never plain `src` or `href`. Spring Boot's content-based resource versioning (`spring.web.resources.chain.strategy.content`) appends MD5 hashes to URLs for cache busting across deployments.
- **Alpine.js**: Used for client-side reactivity across all modules. Components are registered via `Alpine.data()` inside an `alpine:init` event listener (e.g., `document.addEventListener("alpine:init", () => { Alpine.data("bibleVerse", () => ({...})); });`). This ensures components are registered with Alpine before DOM processing and avoids race conditions with `defer`-loaded Alpine. WebSocket/canvas/media code stays imperative within the component methods. Alpine.js is loaded via WebJar (`org.webjars.npm:alpinejs:3.14.9`) with `defer` attribute. Global CSS rule `[x-cloak] { display: none !important; }` in `main.css` prevents flash of unstyled content. Use `x-if` (not `x-show`) when multiple states should never coexist in the DOM simultaneously (e.g., loading spinner vs content).
- **HTMX + Alpine.js Coexistence**: HTMX handles server-rendered fragment swaps (foosball, plant search). Alpine.js handles client-side state (games, booking form, media). For pages using both (landscape planner), HTMX swaps raw HTML fragments and Alpine manages surrounding UI state. The `selectTimeSlot` function in booking is exposed to `window` via Alpine's `init()` for compatibility with server-rendered fragment `onclick` handlers.
- **Theme Toggle**: Sun/moon icon toggle in navbar (home page only) as Alpine `themeToggle()` component. Theme preference stored in localStorage (`darkMode: enabled/disabled`). Booking pages automatically apply the saved theme without showing the toggle.
- **CSRF**: Cookie-based CSRF tokens (`CookieCsrfTokenRepository`) with `CsrfTokenRequestAttributeHandler` for eager token generation (required in Spring Security 6+ where CSRF tokens are deferred by default). `csrf-utils.js` supports both meta tag and cookie-based token delivery with cookie as fallback. Alpine components on pages without meta tags (booking) read the token directly from the `XSRF-TOKEN` cookie.
- **Structured AI Output**: Skatetricks trick analyzer uses `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT` with Spring AI's `.entity()` for type-safe JSON responses from OpenAI. `@JsonIgnoreProperties(ignoreUnknown = true)` on response schema records handles extra fields the model may include. System prompts lead with "Your output MUST be ONLY a valid JSON object" to prevent text-before-JSON responses.

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
        final var requestStart = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        scenario.stimulate(() -> bookingService.createBooking(
                        1L,
                        "Test User",
                        "test@example.com",
                        "555-555-5555",
                        requestStart,
                        "Test booking",
                        "user-123"))
            .andWaitForEventOfType(BookingCreatedEvent.class)
            .toArriveAndVerify(event -> assertThat(event.attendeeEmail()).isNotBlank());
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
- Do not embed new helper, support, or utility classes inside other source files. Add new top-level classes in their own files, even when the class is small.

### OpenAI / Spring AI Configuration

The application has moved off AWS Bedrock. Do not add Bedrock starters, Bedrock model IDs, Titan embedding configuration, or `BedrockRuntimeClient` back into the app unless explicitly requested.

**Current Spring AI M5 configuration** (`application.yml`):

```yaml
spring:
  ai:
    openai:
      api-key: ${PERSONAL_OPENAI_API_KEY:${OPENAI_API_KEY:}}
      chat:
        options:
          model: ${PERSONAL_OPENAI_CHAT_MODEL:gpt-4o}
          max-tokens: 4096
      embedding:
        options:
          model: ${PERSONAL_OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
          dimensions: ${PERSONAL_OPENAI_EMBEDDING_DIMENSIONS:1024}
      image:
        options:
          model: ${PERSONAL_OPENAI_IMAGE_MODEL:dall-e-3}
          n: 1
          response-format: b64_json
          size: 1024x1024
    model:
      chat: openai
      embedding: openai
      image: openai
```

#### Spring AI Multimodal Messages (Images + Text)

When sending images via Spring AI chat/vision, include image data as `Media` on the user message:

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
final var badTemplate = """
        Return JSON like this:
        {
          "field": "value"
        }
        """;

// CORRECT - Escape braces in examples
final var goodTemplate = """
        Return JSON like this:
        {{
          "field": "value"
        }}
        """;
```

This applies to FinancialPeaceQuestionGenerator (trivia module). For image analysis, prefer `String.format()` with multimodal UserMessage (see above).

### Infrastructure Notes

#### S3 Vectors Integration (Skatetricks RAG Pipeline)

The `skatetricks` module uses **AWS S3 Vectors** as a vector store for Retrieval-Augmented Generation (RAG) to improve trick detection accuracy over time.

**Two-model architecture:**
- **OpenAI chat/vision model** — visual trick analysis via Spring AI `ChatClient`
- **OpenAI text embeddings** (`text-embedding-3-small`, configured to 1024 dimensions) — compact motion signatures via Spring AI `EmbeddingModel`

**Embedding strategy — two text outputs from PoseData:**
- `toPromptText()` — verbose per-frame skeletal data injected into the model prompt for visual analysis
- `toEmbeddingText()` — compact motion signature (5 key frames: setup/launch/peak/descent/landing, overall motion stats) optimized for text embedding. This is what gets embedded and stored in the vector store.

The `embedding_text` column on `trick_attempts` stores the compact text. If null (legacy rows), falls back to raw `pose_data` for embedding.

**Store flow** (verified attempt -> vector store):
1. Trick analyzed by OpenAI; YOLO pose data saved to `pose_data` column, embedding text to `embedding_text` column in DB
2. Auto-verified if `confidence >= 80`; otherwise surfaced to user for confirmation/correction
3. On verification: `writeToVectorStore()` embeds the **embedding text** (compact motion signature) via `EmbeddingService`, stores with `PutInputVector` keyed `attempt-{id}` and `Document` metadata (`trickName`, `confidence`, `formScore`, `attemptId`, `feedback`)
4. Attempts without pose/embedding data (e.g., direct video analysis path) are skipped for vector store writes

**IMPORTANT**: Both the stored vectors and query vectors must embed the **same type of data** (embedding text from `toEmbeddingText()`). The embedding text captures motion signatures (rotation, airborne ratio, knee bend, key frame angles) so that similar trick mechanics cluster together in vector space.

**RAG query flow** (frame analysis path only):
1. `OpenAiTrickAnalyzer.fetchSimilarExamples()` embeds the compact embedding text
2. Queries `queryVectors` top-3 by cosine similarity
3. Similar verified past attempts (with feedback from metadata) are injected as few-shot examples into the system prompt

**Key classes:**
- `EmbeddingService` — wraps Spring AI `EmbeddingModel`, returns `List<Float>` (1024 dims)
- `VectorStoreInitializer` — `@PostConstruct` creates the index (`DataType.FLOAT32`, `DistanceMetric.COSINE`, dim=1024); supports `recreate` flag for index reset. Conditional on `skatetricks.vectorstore.enabled`.
- `writeToVectorStore()` in `SkateTricksService` — embeds embedding text, upserts (same key replaces on correction)
- `fetchSimilarExamples()` in `OpenAiTrickAnalyzer` — RAG retrieval for frame analysis

**Configuration** (`application.yml`):

```yaml
skatetricks:
  vectorstore:
    bucket: thonbecker-vectors
    index: skatetricks-tricks
    dimension: 1024
    recreate: true   # Wipes and recreates the index on startup for the OpenAI embedding migration
    enabled: true    # Set to false to disable vector store (disabled in dev profile)
```

The `S3VectorsClient` bean, `VectorStoreInitializer`, and `EmbeddingService` are all conditional on `skatetricks.vectorstore.enabled` (defaults to `true`, set to `false` in `application-dev.yml`).

**Known gap:** `analyzeVideo()` (direct video path) does not query the vector store and does not write to it — no pose data is available in that path.

#### Landscape Planning Module

The `landscape` module provides AI-powered landscape design with plant selection based on USDA hardiness zones.

**Architecture:**
- **OpenAI chat/vision model** — analyzes landscape images to recommend suitable plants and describe seasonal views
- **OpenAI image model** — generates seasonal landscape variation images via Spring AI `ImageModel`
- **USDA Plants Database API** — authoritative plant data with hardiness zone, light/water requirements
- **USDA Plants Gallery** — plant images fetched by USDA symbol (`PlantImageService`)
- **AWS S3 + CloudFront** — stores uploaded landscape images with CDN delivery
- **Fabric.js** — interactive canvas for visual plant placement with click-to-place markers

**Key features:**
1. **Image Upload**: Users upload photos of their yard (JPEG/PNG, max 100MB)
2. **AI Analysis**: OpenAI analyzes the image considering sunlight exposure, existing vegetation, space constraints
3. **Plant Search**: Search USDA Plants Database with filters for hardiness zone, sun/water requirements
4. **Interactive Plant Placement**: Fabric.js canvas — select a plant, click on the image to place it. Plant-type icons (tree, shrub, flower) with real plant photos from USDA gallery
5. **Seasonal AI Preview**: OpenAI generates text descriptions and images showing the landscape in spring, summer, fall, winter
6. **Plan Management**: Save, load, and delete landscape plans with placements persisted to database
7. **Caching**: Plant data, search results, and plant images cached for 24 hours with Caffeine

**Key classes:**
- `LandscapeService` — coordinates image storage, AI analysis, plant search, placement, and seasonal preview
- `LandscapeAiService` — uses OpenAI for plant recommendations and seasonal text descriptions
- `LandscapeImageGenerationService` — uses Spring AI `ImageModel` for seasonal image generation
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

The `booking` module provides appointment scheduling with event-driven notifications and calendar integration.

**Architecture:**
- **Event-Driven** — publishes `BookingCreatedEvent` and `BookingCancelledEvent` from `booking.api`; notification module sends emails, calendar module creates CalDAV events
- **Cancellation via Events** — listens for `BookingCancellationRequestedEvent` (published by the calendar module when a Nextcloud calendar event is deleted) to cancel bookings. The `BookingCancellationListener` handles this in `booking.platform`.
- **Event Publication Registry** — `spring-modulith-starter-jpa` persists events to `event_publication` table for guaranteed delivery and replay
- **Automatic Availability Generation** — `AvailabilityScheduler` creates recurring weekly slots (Monday-Friday, 11 AM-12 PM and 6-9 PM) for 4 weeks ahead
- **iCal4j Library** — generates RFC-compliant `.ics` calendar files with Central Time (America/Chicago) timezone

**Key classes:**
- `BookingService` — booking creation, cancellation, availability management, event publishing
- `BookingCancellationListener` — handles `BookingCancellationRequestedEvent` from calendar module
- `AvailabilityScheduler` — generates recurring weekly availability slots automatically
- `BookingController` — public endpoints for booking creation and management
- `BookingAdminController` — protected admin endpoints for system configuration

**Domain Events** (in `booking.api`):
- `BookingCreatedEvent` — contains all booking details (attendee info, times, confirmation code, message)
- `BookingCancelledEvent` — contains cancellation details for notification and calendar
- `BookingCancellationRequestedEvent` — published by external modules (calendar) to request booking cancellation

#### Calendar Module

The `calendar` module provides **Nextcloud CalDAV integration** for automatic calendar management. It is fully event-driven.

**Architecture:**
- **Event-Driven** — listens for `BookingCreatedEvent` and `BookingCancelledEvent` to create/delete CalDAV events
- **Calendar Sync** — `CalendarSyncScheduler` polls Nextcloud every 5 minutes (ShedLock-protected) for deleted events; publishes `BookingCancellationRequestedEvent` when a deletion is detected
- **CalDAV via WebClient** — uses `WebClient` for CalDAV HTTP methods (PROPFIND, PUT, DELETE, MKCALENDAR). No dedicated CalDAV library.
- **Conditional** — all CalDAV beans are `@ConditionalOnProperty(name = "calendar.nextcloud.enabled")`, disabled in dev/test

**Key classes:**
- `CalendarEventListener` — handles `BookingCreatedEvent` (creates CalDAV event + stores mapping) and `BookingCancelledEvent` (deletes CalDAV event) with `@TransactionalEventListener`
- `NextcloudCalDavService` — CalDAV HTTP client (MKCALENDAR, PUT, DELETE, PROPFIND)
- `CalendarSyncScheduler` — polls for deleted events, publishes `BookingCancellationRequestedEvent`
- `IcsGenerator` — generates `.ics` content with deterministic UIDs (`booking-{code}@thonbecker.biz`)
- `CalendarEventMappingEntity` — persists booking-to-CalDAV-UID mapping in `booking.calendar_event_mappings` table

**Configuration** (`application.yml`):

```yaml
calendar:
  nextcloud:
    enabled: true
    base-url: https://cloud.thonbecker.biz
    username: ${PERSONAL_NEXTCLOUD_USERNAME}
    password: ${PERSONAL_NEXTCLOUD_APP_PASSWORD}
    calendar-name: bookings
    organizer-email: thon.becker@gmail.com
    sync-cron: "0 */5 * * * *"
```

**Environment variables:**
- `PERSONAL_NEXTCLOUD_USERNAME` — Nextcloud username
- `PERSONAL_NEXTCLOUD_APP_PASSWORD` — Nextcloud app password (generate in Settings > Security > App passwords)

**CalDAV URL pattern:** `https://cloud.thonbecker.biz/remote.php/dav/calendars/{username}/bookings/`

**Cancellation flow when calendar event deleted from Nextcloud:**
1. `CalendarSyncScheduler` detects missing UID via PROPFIND
2. Deletes mapping from `calendar_event_mappings`
3. Publishes `BookingCancellationRequestedEvent`
4. `BookingCancellationListener` (booking module) cancels the booking
5. `BookingCancelledEvent` fires → notification sends cancellation email
6. Calendar module's `CalendarEventListener` tries to delete CalDAV event → mapping already gone, skips

#### Notification Module

The `notification` module is **fully event-driven** with no public service API. It handles **email only** via AWS SES.

**Architecture:**
- **Zero Coupling** — depends only on event types from other modules' `api` packages via `@NamedInterface`
- **`@TransactionalEventListener`** — for booking events (guaranteed delivery via event publication registry)
- **`@EventListener` + `@Async`** — for logging events (non-critical, fire-and-forget)
- **AWS SES** — sends real emails with `.ics` attachments via `SendRawEmail` (MIME multipart)
- **Conditional** — email sending disabled in dev/test via `notification.email.enabled=false`

**Key classes:**
- `NotificationEventListener` — handles `BookingCreatedEvent` and `BookingCancelledEvent` with `@TransactionalEventListener`
- `EventLoggingListener` — logs events from trivia, foosball, user modules with `@Async`
- `EmailNotificationService` — sends booking confirmation (with `.ics` attachment) and cancellation emails via SES
- `CalendarService` — generates `.ics` calendar attachments from event data (for email attachments)

**Configuration** (`application.yml`):

```yaml
notification:
  email:
    enabled: true
    sender: thon.becker@gmail.com
    admin: thon.becker@gmail.com
```

**AWS SES prerequisites:**
- Sender email (`thon.becker@gmail.com`) must be verified in SES
- SES must be in production mode (not sandbox) to send to unverified recipients
- Uses existing AWS credentials (`PERSONAL_AWS_ACCESS_KEY_ID` / `PERSONAL_AWS_SECRET_ACCESS_KEY`)
- `SesClient` bean configured in `AwsConfig`

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
