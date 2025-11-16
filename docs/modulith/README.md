# Spring Modulith Documentation

This directory contains automatically generated documentation for the modular structure of the Personal Web application using Spring Modulith.

## What is Spring Modulith?

Spring Modulith helps developers implement modular monoliths in Spring Boot applications by:
- Enforcing module boundaries at compile/test time
- Generating documentation of module structure
- Providing observability for module interactions
- Supporting event-driven communication between modules

## Module Structure

The application is organized into the following modules:

### Core Modules

#### Trivia Module (`trivia/`)

**Purpose**: AI-powered Financial Peace University trivia quiz functionality with real-time multiplayer support

**Public API**:
- `biz.thonbecker.personal.trivia.api.TriviaFacade` - Main service interface for other modules
- `biz.thonbecker.personal.trivia.domain.*` - Domain models (Quiz, Question, Player, etc.)

**Internal Implementation**:
- `biz.thonbecker.personal.trivia.infrastructure.TriviaService` - Core business logic
- `biz.thonbecker.personal.trivia.infrastructure.QuestionGenerationService` - AI-powered question generation using AWS Bedrock
- `biz.thonbecker.personal.trivia.infrastructure.persistence.*` - JPA entities and repositories
- `biz.thonbecker.personal.trivia.infrastructure.web.*` - WebSocket and STOMP controllers

**Database Schema**: `trivia` schema with tables for quiz results and player statistics

**Dependencies**: None (self-contained module)

**Files**: See `module-trivia.puml`

#### Foosball Module (`foosball/`)

**Purpose**: Foosball game tracking, player management, tournament system, and comprehensive statistics

**Public API**:
- `biz.thonbecker.personal.foosball.api.FoosballFacade` - Main service interface for other modules
- `biz.thonbecker.personal.foosball.domain.*` - Domain models (Game, Player, Team, Stats)
- `biz.thonbecker.personal.foosball.api.GameRecordedEvent` - Event published when games are recorded
- `biz.thonbecker.personal.foosball.api.PlayerCreatedEvent` - Event published when players are created

**Internal Implementation**:
- `biz.thonbecker.personal.foosball.infrastructure.FoosballService` - Core business logic
- `biz.thonbecker.personal.foosball.infrastructure.TournamentService` - Tournament bracket management
- `biz.thonbecker.personal.foosball.infrastructure.persistence.*` - JPA entities and repositories
- `biz.thonbecker.personal.foosball.infrastructure.web.*` - REST and HTMX controllers
- `biz.thonbecker.personal.foosball.infrastructure.tournament.algorithm.*` - Tournament algorithms

**Database Schema**: `foosball` schema with tables for players, games, teams, statistics, and tournaments

**Dependencies**: `shared` (for common infrastructure)

**Files**: See `module-foosball.puml`

### Infrastructure Modules

#### Service Module (`service/`)

Contains various service implementations and legacy code. Being gradually refactored into proper modules.

#### Types Module (`types/`)

Contains shared type definitions. Being gradually refactored into domain-specific modules.

#### Controller Module (`controller/`)

Contains web controllers. Will be moved into module-specific infrastructure/web packages.

#### Configuration Module (`configuration/`)

Contains Spring configuration classes.

#### Client Module (`client/`)

Contains external API clients.

## PlantUML Diagrams

The `*.puml` files can be rendered using PlantUML to visualize:

### Module Component Diagram (`components.puml`)

Shows all modules and their relationships.

**To view**:
1. Install PlantUML: https://plantuml.com/
2. Or use online viewer: https://www.plantuml.com/plantuml/uml/
3. Or use IDE plugin (IntelliJ, VS Code)

### Individual Module Diagrams

Each `module-*.puml` file shows the internal structure of a specific module.

## Verification Tests

The module structure is verified by tests in `src/test/java/solutions/thonbecker/personal/modulith/`:

- `ModuleStructureTest.verifiesModularStructure()` - Ensures modules only depend on allowed dependencies
- `ModuleStructureTest.createsModuleDocumentation()` - Generates this documentation
- `TriviaModuleTest` - Tests the Trivia module in isolation
- `FoosballModuleTest` - Tests the Foosball module in isolation

Run tests with:

```bash
mvn test -Dtest=ModuleStructureTest
```

## Module Design Principles

### 1. Hexagonal Architecture

- **Domain Layer**: Pure business logic
- **Application Layer**: Public API (Facades)
- **Infrastructure Layer**: External integrations, persistence

### 2. Encapsulation

- Only `api` and `domain` packages are public
- `infrastructure` package is package-private
- Other modules cannot access implementation details

### 3. Dependency Rules

- Modules declare allowed dependencies explicitly
- Circular dependencies are prevented
- Dependencies are verified at test time

### 4. SOLID Principles

- **Single Responsibility**: Each module has one clear purpose
- **Open/Closed**: Extend via interfaces, not modification
- **Dependency Inversion**: Depend on abstractions (Facades)

## Migration Status

### ✅ Completed

- **Trivia Module**: Fully migrated with domain models, facade, and infrastructure
- **Foosball Module**: Fully migrated with domain models, facade, and infrastructure
- Module boundary verification tests
- Automated documentation generation

### 🔄 In Progress

- Moving legacy controllers into module infrastructure packages
- Refactoring service and types modules into domain-specific modules

### 📋 Planned

- Event-driven communication between modules
- Module-specific event publishing
- Observability and monitoring integration

## Regenerating Documentation

To regenerate this documentation after code changes:

```bash
mvn test -Dtest=ModuleStructureTest#createsModuleDocumentation
cp -r target/spring-modulith-docs/* docs/modulith/
```

## References

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- Module migration documentation is part of the codebase history and package-info.java files

