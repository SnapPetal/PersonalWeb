# Thon Becker's Personal Website

A modern, feature-rich personal portfolio website built with Spring Boot, showcasing professional experience and interactive applications.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.8-blue)
![HTMX](https://img.shields.io/badge/HTMX-2.0.7-purple)

## 🌟 Features

### **Core Portfolio**

- **Professional Experience Display** with dynamic year counter
- **Responsive Design** using Bootstrap 5
- **Dark/Light Mode** toggle for better user experience
- **Interactive Bible Verse** display with caching
- **Dad Jokes Player** with audio integration

### **Interactive Applications**

- **🏓 Foosball Management System** - Complete table soccer game tracking
- **🎯 Dave Ramsey FPU Trivia** - AI-powered Financial Peace University trivia with real-time multiplayer
- **📊 Statistics Dashboard** - Player and team performance analytics

### **Technical Features**

- **🔒 CSRF Protection** for secure form submissions
- **🌍 Environment Configuration** with AWS Cognito integration
- **⚡ Caching** with Caffeine for optimized performance
- **🔄 Retry Logic** for external API calls
- **📱 Progressive Web App** features

## 🚀 Quick Start

### Prerequisites

- Java 25+
- Maven 3.6+
- PostgreSQL 16+ (for local development)
- Docker (for running PostgreSQL locally)
- AWS Cognito setup (for authentication features)
- AWS Bedrock access (for AI-powered trivia questions)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/SnapPetal/PersonalWeb.git
   cd PersonalWeb
   ```
2. **Set up environment variables**

   ```bash
   cp .env.example .env
   # Edit .env with your actual Cognito credentials
   ```
3. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

## 🏗️ Architecture

### **Backend Stack**

- **Spring Boot 3.5.6** - Core framework
- **Spring Security** - Authentication & CSRF protection
- **Spring Cloud OpenFeign** - HTTP client for microservices
- **Spring WebSocket** - Real-time communication for trivia
- **Spring AI with AWS Bedrock** - AI-powered question generation
- **PostgreSQL 16** - Relational database
- **Liquibase** - Database migration and version control
- **Caffeine Cache** - In-memory caching solution
- **Thymeleaf** - Server-side template engine

### **Frontend Stack**

- **Bootstrap 5.3.8** - Responsive UI framework
- **HTMX 2.0.7** - Dynamic HTML interactions
- **Bootstrap Icons** - Icon library
- **SockJS & STOMP** - WebSocket communication
- **Vanilla JavaScript** - Custom interactions

### **Integration & Services**

- **AWS Cognito** - OAuth2/OpenID authentication
- **External APIs** - Bible verse and dad jokes integration
- **Spring Modulith** - Modular monolith architecture with enforced boundaries

## 📁 Project Structure

This project follows a modular monolith architecture using Spring Modulith:

```
src/
├── main/
│   ├── java/biz/thonbecker/personal/
│   │   ├── foosball/                  # Foosball Module
│   │   │   ├── api/                   # Public facade interfaces
│   │   │   ├── domain/                # Domain models (Game, Player, Stats)
│   │   │   └── infrastructure/        # Implementation, persistence, web
│   │   ├── trivia/                    # Trivia Module
│   │   │   ├── api/                   # Public facade interfaces
│   │   │   ├── domain/                # Domain models (Quiz, Question)
│   │   │   └── infrastructure/        # Implementation, persistence, web
│   │   ├── shared/                    # Shared infrastructure
│   │   ├── configuration/             # Spring configuration
│   │   └── PersonalWebApplication.java
│   ├── resources/
│   │   ├── templates/                 # Thymeleaf HTML templates
│   │   ├── static/                    # CSS, JS, images
│   │   ├── db/changelog/              # Liquibase migrations
│   │   └── application.yml            # Configuration
└── test/
    ├── java/                          # Unit tests
    └── modulith/                      # Module structure tests
```

See [Spring Modulith Documentation](docs/modulith/README.md) for detailed module architecture.

## 🔧 Configuration

### **🔐 CSRF Protection**

The application implements Cross-Site Request Forgery protection using Spring Security with token-based validation for all POST requests.

### **🌍 Environment Configuration**

Configure your development environment with AWS Cognito integration using environment variables. See `.env.example` for required settings.

## 🎮 Applications

### **Foosball Management System**

A complete table soccer game tracking system integrated as a Spring Modulith module:

- **Player Management** - Create and track players with email validation
- **Game Recording** - Record match results with team positions and scores
- **Statistics & Analytics** - Comprehensive player and team performance metrics
  - Win/loss records and percentages
  - Head-to-head team statistics
  - Player rankings by various criteria
  - Game history and trends
- **Tournament System** - Single-elimination tournament bracket generation
- **Database Persistence** - PostgreSQL with Liquibase migrations
- **HTMX Integration** - Dynamic UI updates without page reloads
- **Module Architecture** - Hexagonal architecture with public facades and encapsulated implementation

**Module:** `biz.thonbecker.personal.foosball`
**Public API:** `FoosballFacade` interface

### **Dave Ramsey FPU Trivia Game**

- **AI-Powered Questions** - Spring AI with AWS Bedrock generates Dave Ramsey Financial Peace University questions
- **WebSocket Integration** - Real-time multiplayer gameplay using STOMP protocol
- **Difficulty Levels** - Easy, Medium, and Hard question difficulty
- **Fallback Questions** - 20 pre-configured FPU questions when AI is unavailable
- **Scoring System** - Live leaderboard updates and winner determination
- **Database Persistence** - PostgreSQL stores quiz results and player statistics
- **Event Logging** - Comprehensive game session tracking

### **Bible Verse of the Day**

- **Daily Verses** - Cached daily verse retrieval
- **KJV Translation** - King James Version integration
- **Retry Logic** - Fault-tolerant API calls
- **Responsive Display** - Mobile-optimized presentation

## 🛡️ Security Features

- **CSRF Token Protection** - All POST requests secured
- **OAuth2 Integration** - AWS Cognito authentication
- **Secure Headers** - Spring Security configuration
- **Environment Isolation** - Separate dev/prod configs

## 🔄 API Endpoints

### **Core APIs**

- `GET /api/experience/count` - Professional experience counter
- `GET /api/bible/verse-of-day` - Daily bible verse
- `GET /api/joke` - Dad joke with audio

### **Foosball APIs**

**REST Endpoints:**
- `GET /api/foosball/players` - Retrieve all players
- `GET /api/foosball/stats/players` - Player statistics and rankings
- `GET /api/foosball/stats/teams` - Team performance statistics
- `GET /api/foosball/games` - Retrieve recent games
- `POST /api/foosball/players` - Create new player
- `POST /api/foosball/games` - Record new game

**HTMX Endpoints:**
- `GET /foosball/htmx/games` - Game list partial
- `POST /foosball/htmx/games` - Record game with HTMX response
- `GET /foosball/htmx/stats/players` - Player stats partial
- `GET /foosball/htmx/stats/teams` - Team stats partial

**Tournament Endpoints:**
- `GET /api/tournaments` - List all tournaments
- `POST /api/tournaments` - Create new tournament
- `POST /api/tournaments/{id}/register` - Register player for tournament
- `POST /api/tournaments/{id}/start` - Start tournament and generate bracket
- `POST /api/tournaments/matches/{matchId}/result` - Record match result

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Run integration tests
./mvnw verify
```

## 📦 Deployment

### **Development**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### **Production**

```bash
# Build
./mvnw clean package -Pproduction

# Run
java -jar target/personal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### **Docker** (Optional)

```dockerfile
FROM openjdk:21-jre-slim
COPY target/personal-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is proprietary and confidential. All rights are reserved by Thon Becker. See the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **GitHub**: [SnapPetal](https://github.com/SnapPetal)
- **LinkedIn**: [Thon Becker](https://www.linkedin.com/in/thon-becker/)

## 🎯 Roadmap

- [ ] **Mobile App** - React Native companion app
- [ ] **Analytics Dashboard** - Usage statistics and insights
- [ ] **Blog System** - Technical writing platform
- [ ] **Portfolio Expansion** - Additional interactive demos
- [ ] **Performance Monitoring** - APM integration
- [ ] **CI/CD Pipeline** - Automated deployment workflow

---

**Built with ❤️ by Thon Becker** | © 2025 All Rights Reserved
