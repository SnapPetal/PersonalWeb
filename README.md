# Thon Becker's Personal Website

A modern, feature-rich personal portfolio website built with Spring Boot, showcasing professional experience and interactive applications.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.8-blue)
![HTMX](https://img.shields.io/badge/HTMX-2.0.8-purple)

## Features

### Core Portfolio

- **Professional Experience Display** with dynamic year counter
- **Responsive Design** using Bootstrap 5
- **Dark/Light Mode** toggle for better user experience
- **Interactive Bible Verse** display with caching
- **Dad Jokes Player** with AI text-to-speech and audio storage

### Interactive Applications

- **Foosball Management System** - Complete table soccer game tracking with ELO ratings and tournaments
- **Dave Ramsey FPU Trivia** - AI-powered Financial Peace University trivia with real-time multiplayer
- **Skatetricks AI** - YOLO pose estimation + Bedrock AI trick detection with RAG-based learning
- **Landscape Planner** - AI-powered landscape design with USDA plant database integration
- **Booking System** - Appointment scheduling with auto-availability and calendar integration
- **Tank Game** - WebSocket-based multiplayer tank game with player progression

### Technical Features

- **Spring Modulith** - Modular monolith with enforced module boundaries
- **Spring AI** - AWS Bedrock Converse API (Claude) for chat/vision, Titan V2 for embeddings
- **Event-Driven Architecture** - Cross-module communication via domain events
- **S3 Vectors RAG Pipeline** - Retrieval-augmented generation for skatetrick detection
- **CSRF Protection** for secure form submissions
- **Caffeine Caching** for optimized performance
- **Spring Retry** for fault-tolerant external API calls

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.6+
- Docker (Spring Boot auto-starts PostgreSQL via Docker Compose)
- AWS credentials (Bedrock, Polly, S3, S3 Vectors, Cognito)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/SnapPetal/PersonalWeb.git
   cd PersonalWeb
   ```
2. **Set up environment variables**

   ```bash
   cp .env.example .env
   # Edit .env with your actual AWS and Cognito credentials
   ```
3. **Run the application**

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Architecture

### Backend Stack

- **Spring Boot 4.0.3** - Core framework (Java 25)
- **Spring Security + AWS Cognito** - OAuth2 authentication & CSRF protection
- **Spring HTTP Interfaces** - Declarative HTTP clients (`@GetExchange`, `@HttpExchange`) with WebClient backend
- **Spring WebSocket** - Real-time communication (STOMP over SockJS)
- **Spring AI** - AWS Bedrock Converse API (Claude Sonnet 4.6) + Titan Text Embeddings V2
- **PostgreSQL 18** - Relational database (auto-started via Docker Compose)
- **Liquibase** - Database migration and version control
- **Caffeine Cache** - In-memory caching solution
- **Thymeleaf** - Server-side template engine
- **DJL (Deep Java Library)** - PyTorch-backed YOLO pose estimation
- **Spring Modulith** - Modular monolith architecture with enforced boundaries

### Frontend Stack

- **Bootstrap 5.3.8** - Responsive UI framework
- **HTMX 2.0.8** - Dynamic HTML interactions
- **Bootstrap Icons** - Icon library
- **SockJS & STOMP** - WebSocket communication
- **Vanilla JavaScript** - Custom interactions

### Integration & Services

- **AWS Bedrock** - Claude Sonnet 4.6 (chat/vision) + Titan Text Embeddings V2
- **AWS S3 Vectors** - Vector store for RAG pipeline
- **AWS Polly** - Neural text-to-speech for dad jokes
- **AWS S3 + CloudFront** - File storage and CDN delivery
- **AWS Cognito** - OAuth2 user authentication
- **USDA Plants Database** - Plant data for landscape planning
- **iCal4j** - RFC-compliant calendar file generation

## Project Structure

This project follows a modular monolith architecture using Spring Modulith:

```
src/main/java/biz/thonbecker/personal/
├── foosball/          # Foosball game tracking, stats, tournaments, ELO
├── trivia/            # AI-powered FPU trivia, WebSocket multiplayer
├── skatetricks/       # YOLO pose estimation + Bedrock AI trick detection
├── landscape/         # AI-powered landscape planning with USDA plant data
├── booking/           # Appointment scheduling with auto-availability
├── tankgame/          # WebSocket tank game with player progression
├── user/              # User management
├── notification/      # Event-driven email notifications (zero coupling)
├── content/           # Bible verse, Dad jokes (Polly TTS + S3)
└── shared/            # Configuration classes + domain events
```

Each module follows this internal package convention:

- `api/` - Public facade interfaces + event listeners
- `domain/` - Domain model objects (pure Java)
- `platform/` - Implementation details: persistence, services, web controllers

See [Spring Modulith Documentation](docs/modulith/all-docs.adoc) for detailed module architecture.

## Modules

|     Module     |    Public Facade    |                          Purpose                           |
|----------------|---------------------|------------------------------------------------------------|
| `foosball`     | `FoosballFacade`    | Table soccer game tracking, stats, tournaments, ELO rating |
| `trivia`       | `TriviaFacade`      | AI-powered FPU trivia, WebSocket multiplayer               |
| `skatetricks`  | `SkateTricksFacade` | YOLO pose estimation + Bedrock AI trick detection          |
| `landscape`    | `LandscapeFacade`   | AI-powered landscape planning with USDA plant database     |
| `booking`      | `BookingFacade`     | Appointment scheduling with auto-availability              |
| `tankgame`     | `TankGameFacade`    | WebSocket tank game with player progression                |
| `user`         | `UserFacade`        | User management                                            |
| `notification` | _(event-driven)_    | Email notifications via event subscribers                  |
| `content`      | _(no facade)_       | Bible verse, Dad jokes (Polly TTS + S3)                    |
| `shared`       | _(config + events)_ | Configuration classes + domain events                      |

## Testing

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run a single test class
mvn test -Dtest=ModuleStructureTest
```

## Deployment

### Development

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production

Pushes to `main` trigger the GitHub Actions workflow, which builds a Docker image using Spring Boot Buildpacks (Paketo, Java 25) and deploys to AWS Lightsail.

```bash
# Build production jar
mvn clean package

# Build Docker image
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=personal
```

## Code Formatting

Spotless enforces formatting as part of the build:

```bash
# Apply formatting (must pass before committing)
mvn spotless:apply

# Check formatting without applying
mvn spotless:check
```

- **Java**: Palantir Java Format
- **JavaScript**: Prettier
- **Markdown**: Flexmark
- **POM**: SortPom

## License

This project is proprietary and confidential. All rights are reserved by Thon Becker. See the [LICENSE](LICENSE) file for details.

## Contact

- **GitHub**: [SnapPetal](https://github.com/SnapPetal)
- **LinkedIn**: [Thon Becker](https://www.linkedin.com/in/thon-becker/)

---

**Built with care by Thon Becker** | &copy; 2025 All Rights Reserved
