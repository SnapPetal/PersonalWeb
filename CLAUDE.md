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

The application is a **modular monolith** enforced by Spring Modulith. Module boundaries are validated at startup and by `ModuleStructureTest`. Cross-module communication happens via:
1. **Public Facades** — for direct method calls (e.g., `BookingFacade`)
2. **Domain Events** — for reactive, decoupled communication (e.g., `BookingCreatedEvent`)

Each module follows this internal package convention:
- `api/` — Public facade interfaces + event listeners (how modules expose and consume functionality)
- `domain/` — Domain model objects (pure Java, no persistence annotations)
- `platform/` — All implementation details: persistence entities, repositories, services, web controllers

**Event-driven modules** (like `notification`) have event listeners in `api/` instead of facade interfaces, since they only consume events and don't expose methods to other modules.

### Modules

|     Module     |       Public Facade        |                                  Purpose                                   |
|----------------|----------------------------|----------------------------------------------------------------------------|
| `foosball`     | `FoosballFacade`           | Table soccer game tracking, stats, tournaments, ELO rating                 |
| `trivia`       | `TriviaFacade`             | AI-powered FPU trivia, WebSocket multiplayer                               |
| `skatetricks`  | `SkateTricksFacade`        | YOLO pose estimation + Bedrock AI trick detection                          |
| `landscape`    | `LandscapeFacade`          | AI-powered landscape planning with USDA plant database integration         |
| `booking`      | `BookingFacade`            | Appointment scheduling with auto-availability, event publishing            |
| `tankgame`     | `TankGameFacade`           | WebSocket tank game with player progression                                |
| `user`         | `UserFacade`               | User management                                                            |
| `notification` | _(event-driven only)_      | Email notifications via event subscribers (zero coupling to other modules) |
| `content`      | _(no external facade)_     | Bible verse, Dad jokes (AWS Polly TTS + S3), experience counter            |
| `shared`       | _(configuration + events)_ | Configuration classes + domain events for cross-module communication       |

### Key Technical Patterns

- **Database schema**: Managed exclusively by Liquibase (`src/main/resources/db/changelog/`). Hibernate DDL is set to `none`. Add new changesets; never alter existing ones.
- **Caching**: Caffeine (`CacheConfig`). Used for Bible verse, Dad joke responses, and plant data (24-hour TTL).
- **Retry**: Spring Retry (`RetryConfig`) for fault-tolerant external API calls.
- **Scheduled jobs**: ShedLock (`ShedlockConfig`) prevents duplicate execution in distributed environments.
- **AI**: Spring AI with AWS Bedrock Converse API (inference profile `us.anthropic.claude-sonnet-4-6`) for trivia question generation and landscape recommendations. DJL (Deep Java Library) with PyTorch for local YOLO pose estimation in skatetricks.
- **WebSockets**: STOMP over SockJS for trivia and tank game real-time communication. Skatetricks video conversion uses WebSocket for progress updates, but analysis uses HTTP polling to avoid timeout issues with long-running AI inference.
- **Async processing**: Skatetricks uses async endpoints with status polling for video conversion and analysis. Long-running operations (30+ seconds) are processed in background threads via `ExecutorService`. Client polls status endpoints (GET `/skatetricks/convert/{id}/status`, `/skatetricks/analyze/{id}/status`) every 2 seconds. YOLO models pre-load at startup (`@PostConstruct`) to prevent first-request timeouts.
- **Event-Driven Architecture**: Modules communicate via immutable event records in `shared.events` package. Events contain ALL data needed for processing (no callbacks). Example: `BookingCreatedEvent` contains all booking details; notification module sends emails directly from event data without calling back to booking module. This ensures zero coupling between modules while maintaining reactive communication.
- **Frontend**: Thymeleaf templates + HTMX for partial page updates + Bootstrap 5. Frontend libraries served as WebJars.
- **Theme Toggle**: Sun/moon icon toggle in navbar (home page only) for light/dark mode switching. Theme preference stored in localStorage (`darkMode: enabled/disabled`). Booking pages automatically apply the saved theme without showing the toggle. Other pages (trivia, foosball, etc.) remain light mode only.
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
          options:
            model: us.anthropic.claude-sonnet-4-6  # Inference profile ID (NOT direct model ID)
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
- **Claude Sonnet 4.6** (inference profile `us.anthropic.claude-sonnet-4-6`) — visual trick analysis via Spring AI / Bedrock Converse API
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

**Verifying vector store data:**

Check if verified attempts are stored in the vector store:

```bash
# List all vectors in the index (shows vector IDs and metadata)
aws-vault exec thonbecker -- aws s3-vectors list-vectors \
  --bucket thonbecker-vectors \
  --index-name skatetricks-tricks \
  --region us-east-1 \
  --output json | jq '.vectorList[] | {id: .id, metadata: .metadata}'

# Count total vectors
aws-vault exec thonbecker -- aws s3-vectors list-vectors \
  --bucket thonbecker-vectors \
  --index-name skatetricks-tricks \
  --region us-east-1 \
  --output json | jq '.vectorList | length'

# Get specific vector by ID (e.g., attempt-123)
aws-vault exec thonbecker -- aws s3-vectors get-vector \
  --bucket thonbecker-vectors \
  --index-name skatetricks-tricks \
  --vector-id "attempt-123" \
  --region us-east-1 \
  --output json | jq '.'
```

**Expected metadata fields per vector:**
- `trickName` — The verified trick name (e.g., "OLLIE")
- `confidence` — Original AI confidence score (0-100)
- `formScore` — Form quality score (0-100)
- `attemptId` — Database FK to `skatetricks.trick_attempts.id`

**Verification flow:**
1. Upload video → AI analyzes → saves to DB with `verified=false`
2. If confidence ≥ 80%, auto-verifies and writes to vector store immediately
3. If confidence < 80%, user clicks "Confirm" or "Correct" → writes to vector store
4. Vector ID format: `attempt-{attemptId}` (e.g., `attempt-42`)
5. Each verification/correction upserts the vector (same ID replaces existing)

**Confirming successful write:**

Look for this log message after verification:

```
✅ Successfully stored attempt 36 (OLLIE) in vector store 'skatetricks-tricks'
```

If the write fails, you'll see:

```
❌ Failed to write attempt X to vector store: <error details>
```

**Testing RAG retrieval:**

Upload a second video of the **same trick type** (e.g., another ollie). The system should:
1. Generate YOLO pose data from the new video
2. Query vector store for similar verified attempts (top-3 by cosine similarity)
3. Inject similar examples into Claude's prompt as few-shot learning
4. Potentially improve detection accuracy based on past verified attempts

Check logs for:

```
Fetching similar examples from vector store for pose data...
Found 2 similar verified attempts in vector store
```

#### Landscape Planning Module

The `landscape` module provides AI-powered landscape design with plant selection based on USDA hardiness zones. Users can upload images of their yard, receive personalized plant recommendations, and create annotated landscape plans.

**Architecture:**
- **Claude Sonnet 4.6** (inference profile `us.anthropic.claude-sonnet-4-6`) — analyzes landscape images to recommend suitable plants based on visible conditions, sunlight exposure, and hardiness zone compatibility
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

#### Booking Module

The `booking` module provides appointment scheduling functionality, replacing the external Calendly integration. Users can book meetings directly through the website with calendar integration and email notifications.

**Architecture:**
- **Event-Driven Notifications** — publishes `BookingCreatedEvent` and `BookingCancelledEvent` to `shared.events`; notification module handles all email sending
- **Automatic Availability Generation** — `AvailabilityScheduler` creates recurring weekly slots (Monday-Friday, 11 AM-12 PM and 6-9 PM) for 4 weeks ahead, runs on startup and daily at 2 AM
- **iCal4j Library** (v4.0.7) — generates RFC-compliant `.ics` calendar files with Central Time (America/Chicago) timezone
- **Calendar Integration** — `.ics` files compatible with Google Calendar, Outlook, Apple Calendar

**Key features:**
1. **Public Booking Page** (`/booking`): Browse meeting types, select date/time, provide attendee info, receive 8-character confirmation code
2. **Booking Types**: Multiple configurable meeting types (30 min, 1 hour, 45 min) with duration and buffer time
3. **Automatic Availability**: System generates recurring availability slots (weekdays only, 4-week rolling window)
4. **Booking Management**: View/cancel bookings via confirmation code
5. **Admin Dashboard** (`/booking/admin`): Manage bookings, availability slots, and booking types

**Database schema** (`booking` schema):
- `booking_types` — meeting type definitions (name, duration, buffer time, color)
- `availability_slots` — available time blocks (auto-generated by AvailabilityScheduler)
- `bookings` — user bookings with confirmation codes, status tracking

**Automatic Availability Configuration:**
- **Schedule**: Monday-Friday only (weekends excluded)
- **Morning slot**: 11:00 AM - 12:00 PM
- **Evening slot**: 6:00 PM - 9:00 PM
- **Generation frequency**: Daily at 2:00 AM (ShedLock prevents duplicates)
- **Cleanup**: Daily at 3:00 AM removes past slots
- **Rolling window**: Always maintains 4 weeks of future availability

**Key classes:**
- `BookingFacade` — public API with 17 methods for booking creation, availability checks, and admin operations
- `AvailabilityScheduler` — generates recurring weekly availability slots automatically
- `BookingController` — public endpoints for booking creation and management
- `BookingAdminController` — protected admin endpoints for system configuration

**Default booking types** (pre-loaded):
1. **30 Minute Consultation** — Quick project discussion
2. **1 Hour Technical Discussion** — Deep dive on architecture/design
3. **Project Discovery Call** (45 min) — Initial collaboration exploration

**Domain Events** (published to `shared.events`):
- `BookingCreatedEvent` — contains all booking details (attendee info, times, confirmation code, message)
- `BookingCancelledEvent` — contains cancellation details for notification

**Event-Driven Communication:**
The booking module publishes events with complete data; it never calls other modules. The notification module subscribes to these events and handles email sending independently.

#### Notification Module

The `notification` module is a **fully event-driven** module with no public facade API. It listens to events from other modules and sends appropriate notifications (email, SMS, push, etc.).

**Architecture:**
- **Zero Coupling** — notification module has NO dependencies on other business modules (booking, trivia, etc.)
- **Event Subscribers** — listens to events in `shared.events` package
- **No Public API** — other modules don't call notification; they publish events
- **Email Service** — currently logs to console; ready for AWS SES integration

**Module Structure:**
- `notification/api/` — event listeners (`NotificationEventListener`, `EventLoggingListener`)
- `notification/platform/` — email formatting services (`EmailNotificationService`, `CalendarService`)
- `notification/domain/` — internal notification models (not exposed externally)

**Email Configuration:**
- Sender email: `thon.becker@gmail.com`
- Admin notification email: `thon.becker@gmail.com`
- Timezone: Central Time (America/Chicago) for calendar attachments

**Event Subscriptions:**
- `BookingCreatedEvent` → sends confirmation email to attendee + notification to admin
- `BookingCancelledEvent` → sends cancellation notification to attendee
- `QuizCompletedEvent`, `QuizStartedEvent`, `PlayerJoinedQuizEvent` → logs quiz activity
- `GameRecordedEvent`, `PlayerCreatedEvent` → logs foosball activity
- `UserRegisteredEvent`, `UserLoginEvent`, `UserProfileUpdatedEvent` → logs user activity

**Key classes:**
- `NotificationEventListener` — handles booking-related events, sends emails
- `EventLoggingListener` — logs domain events from trivia, foosball, user modules
- `EmailNotificationService` — formats and sends booking confirmation/cancellation emails (works with event data directly)
- `CalendarService` — generates `.ics` calendar attachments from event data

**Best Practice Pattern:**

```java
// Event contains ALL data needed for notification
@EventListener
void onBookingCreated(BookingCreatedEvent event) {
    emailService.sendConfirmation(event);  // No callback to booking module!
}
```

This architecture ensures the notification module can be:
- Tested independently
- Scaled independently
- Extended with new channels (SMS, push) without touching other modules
- Replaced or disabled without breaking other modules

#### Shared Module

The `shared` module provides infrastructure configuration and cross-module contracts.

**Contents:**
- `shared/platform/configuration/` — Spring configuration classes (SecurityConfig, CacheConfig, AwsConfig, etc.)
- `shared/events/` — **Domain events** used for cross-module communication (e.g., `BookingCreatedEvent`, `BookingCancelledEvent`)

**Event-Driven Architecture Pattern:**
Domain events in `shared.events` are immutable records containing complete data for subscribers:

```java
// Events are self-contained - no callbacks needed
public record BookingCreatedEvent(
    Long bookingId, String confirmationCode,
    String attendeeEmail, String attendeeName, String attendeePhone,
    String bookingTypeName, LocalDateTime startTime, LocalDateTime endTime,
    String message
) {}
```

**Key principle:** Events are owned by neither publisher nor subscriber - they're neutral contracts in shared space.

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

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/aws-deploy.yml`), which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to **AWS Lightsail** container service (`personal-service`).
