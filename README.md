# Thon Becker's Personal Website

A modular monolith personal portfolio and interactive applications platform built with Spring Boot 4, Spring Modulith, and Spring AI.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.8-blue)
![HTMX](https://img.shields.io/badge/HTMX-2.0.8-purple)
![Alpine.js](https://img.shields.io/badge/Alpine.js-3.14.9-teal)

## Features

### Portfolio

- Professional experience display with dynamic year counter
- Dark/light mode toggle
- Daily Bible verse with caching
- Dad jokes player with AI text-to-speech (AWS Polly)

### Interactive Applications

|      Application      |                                Description                                |
|-----------------------|---------------------------------------------------------------------------|
| **Foosball**          | Table soccer game tracking with ELO ratings and tournaments               |
| **FPU Trivia**        | AI-powered Financial Peace University trivia with real-time multiplayer   |
| **Skatetricks AI**    | YOLO pose estimation + Bedrock AI trick detection with RAG learning       |
| **Landscape Planner** | AI-powered landscape design with USDA plant database and Fabric.js canvas |
| **Booking System**    | Appointment scheduling with auto-availability and calendar integration    |
| **Tank Game**         | WebSocket-based multiplayer tank game with player progression             |

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.6+
- Docker (Spring Boot auto-starts PostgreSQL via Docker Compose)
- AWS credentials (Bedrock, Polly, S3, S3 Vectors, Cognito)

### Setup

```bash
git clone https://github.com/SnapPetal/PersonalWeb.git
cd PersonalWeb
cp .env.example .env
# Edit .env with your AWS and Cognito credentials
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For Skatetricks transcoding, set `SKATETRICKS_MEDIACONVERT_ROLE_ARN` from the HomeWeb CDK `MediaConvertRoleArn` output. Do not set `SKATETRICKS_MEDIACONVERT_ENDPOINT`; the app discovers the correct account-specific endpoint automatically via `DescribeEndpoints`.

Skatetricks remote import supports direct downloadable video URLs and now attempts provider-specific resolution for public Instagram, Facebook, and YouTube page URLs before transcoding. Private, auth-gated, or stream-protected videos can still fail.

## Architecture

### Stack

|     Layer     |                                    Technologies                                     |
|---------------|-------------------------------------------------------------------------------------|
| **Backend**   | Spring Boot 4, Spring Modulith, Spring AI, Spring Security + Cognito OAuth2         |
| **AI/ML**     | AWS Bedrock (Claude Sonnet 4.6, Titan Embeddings V2, Nova Canvas), DJL PyTorch YOLO |
| **Database**  | PostgreSQL 18, Liquibase migrations, Caffeine cache                                 |
| **Frontend**  | Thymeleaf, HTMX, Alpine.js, Bootstrap 5, Fabric.js, WebJars                         |
| **Real-time** | STOMP over SockJS (trivia, tank game)                                               |
| **AWS**       | Bedrock, S3, S3 Vectors, SES, CloudFront, Polly, Cognito, Lightsail                 |

### Modules

```
src/main/java/biz/thonbecker/personal/
├── foosball/       # Game tracking, stats, tournaments, ELO rating
├── trivia/         # AI-powered FPU trivia, WebSocket multiplayer
├── skatetricks/    # YOLO pose estimation + Bedrock AI trick detection
├── landscape/      # AI-powered landscape planning with USDA plant data
├── booking/        # Appointment scheduling with auto-availability
├── tankgame/       # WebSocket tank game with player progression
├── user/           # User management
├── calendar/       # Nextcloud CalDAV integration with calendar sync
├── notification/   # Event-driven email notifications via AWS SES
├── content/        # Bible verse, Dad jokes (Polly TTS + S3)
└── shared/         # Infrastructure configuration (Security, Cache, AWS, WebSocket)
```

Each module follows: `api/` (exported events/interfaces) | `domain/` (pure Java models) | `platform/` (services, persistence, controllers)

Cross-module communication is event-driven only — no module calls another module's service directly.

## Development

```bash
mvn test                    # Run all tests
mvn verify                  # Run integration/verification tests
mvn spotless:apply          # Apply code formatting (required before committing)
mvn clean package           # Build production jar
```

## Deployment

Pushes to `main` trigger GitHub Actions, which builds a Docker image via Spring Boot Buildpacks (Paketo, Java 25) and deploys to AWS Lightsail.

## License

This project is proprietary and confidential. All rights reserved by Thon Becker. See the [LICENSE](LICENSE) file for details.

## Contact

- **GitHub**: [SnapPetal](https://github.com/SnapPetal)
- **LinkedIn**: [Thon Becker](https://www.linkedin.com/in/thon-becker/)

---

**Built by Thon Becker** | &copy; 2025-2026 All Rights Reserved
