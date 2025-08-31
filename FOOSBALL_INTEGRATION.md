# Foosball Integration

This document describes the foosball integration added to the PersonalWeb project, which provides a UI to interact with the [Foosball Backend Service](https://github.com/SnapPetal/foosball).

## Overview

The foosball integration adds a comprehensive management interface for tracking foosball games, players, and statistics. It connects to the foosball backend service via REST API calls and provides a modern, responsive web interface.

## Features

### 🏓 Player Management
- Add new players with name and email
- View all registered players
- Player creation timestamps

### 🎮 Game Recording
- Record 2v2 foosball games
- Team composition management
- Score tracking for both teams
- Optional game notes
- Automatic winner determination

### 📊 Statistics & Analytics
- Player performance statistics
- Position-based analysis (Goalie vs. Forward)
- Win percentages and game counts
- Team performance metrics

### 🔄 Real-time Updates
- Live data refresh capabilities
- Dynamic content updates
- Responsive UI with Bootstrap

## Architecture

### Backend Components

#### Java Classes
- **`FoosballPlayer`**: Player entity with id, name, email, and creation timestamp
- **`FoosballGame`**: Game entity with team composition, scores, and metadata
- **`FoosballStats`**: Statistics entity for player and position performance
- **`FoosballService`**: Service layer for API communication
- **`FoosballController`**: REST controller for web endpoints

#### Configuration
- **`WebClientConfig`**: RestTemplate configuration for HTTP requests
- **`application.yml`**: Foosball API base URL configuration

### Frontend Components

#### Templates
- **`foosball.html`**: Main foosball management interface
- **`index.html`**: Updated with foosball project card

#### JavaScript
- **`foosball.js`**: Client-side functionality and API interactions

#### CSS
- **`foosball.css`**: Custom styling for the foosball interface

## Setup Instructions

### 1. Prerequisites
- Java 21+
- Maven 3.6+
- Running foosball backend service

### 2. Foosball Backend Service
The foosball backend service must be running and accessible. By default, it's configured to connect to:
```
https://foosball.thonbecker.biz
```

To change this, update the configuration in `src/main/resources/application.yml`:
```yaml
foosball:
  api:
    base-url: https://foosball.thonbecker.biz
```

**Note**: All API endpoints are constructed by appending `/api/foosball/*` to the base URL. For example:
- Base URL: `https://foosball.thonbecker.biz`
- Players endpoint: `https://foosball.thonbecker.biz/api/foosball/players`
- Games endpoint: `https://foosball.thonbecker.biz/api/foosball/games`

### 3. Running the Application
```bash
# Start the PersonalWeb application
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Access the Foosball Interface
Navigate to: `http://localhost:8080/foosball`

## API Endpoints

### Internal Endpoints (PersonalWeb)
- `GET /foosball` - Main foosball management page
- `POST /foosball/players` - Create new player
- `POST /foosball/games` - Record new game
- `GET /foosball/api/players` - Get all players
- `GET /foosball/api/games` - Get all games
- `GET /foosball/api/stats/players` - Get player statistics
- `GET /foosball/api/stats/position` - Get position statistics

### External Endpoints (Foosball Service)
The application communicates with the foosball backend service using these endpoints:
- `GET /api/foosball/players` - Retrieve all players
- `POST /api/foosball/players` - Create new player
- `GET /api/foosball/games` - Retrieve all games
- `POST /api/foosball/games` - Create new game
- `GET /api/foosball/stats/players` - Get player statistics
- `GET /api/foosball/stats/position` - Get position statistics
- `GET /actuator/health` - Health check

## Usage Guide

### Adding Players
1. Click "Add New Player" button
2. Fill in player name and email
3. Click "Add Player" to save

### Recording Games
1. Click "Record New Game" button
2. Select 4 unique players (2 per team)
3. Enter scores for both teams
4. Add optional notes
5. Click "Record Game" to save

### Viewing Statistics
- **Player Statistics**: Shows games played, wins, and win percentage
- **Position Statistics**: Shows performance by position (Goalie/Forward)
- **Recent Games**: Displays latest game results

### Refreshing Data
- Use the refresh buttons on each section to reload data
- Data automatically refreshes when adding new players or games

## Error Handling

### Service Unavailable
If the foosball backend service is not accessible:
- Warning message is displayed
- Link to GitHub repository provided
- UI gracefully degrades

### API Errors
- Failed API calls are logged to console
- User-friendly error messages displayed
- Form validation prevents invalid submissions

## Customization

### Styling
The foosball interface uses custom CSS classes that can be modified in `src/main/resources/static/css/foosball.css`:
- `.foosball-stats-card` - Statistics card styling
- `.foosball-table` - Table styling
- `.foosball-team-section` - Team input section styling
- `.foosball-empty-state` - Empty state styling

### Configuration
Additional configuration options can be added to `application.yml`:
```yaml
foosball:
  api:
    base-url: http://localhost:8080
    timeout: 5000
    retry-attempts: 3
```

## Troubleshooting

### Common Issues

#### Service Not Available
- Ensure foosball backend service is running
- Check network connectivity
- Verify API base URL configuration

#### Data Not Loading
- Check browser console for JavaScript errors
- Verify API endpoints are accessible
- Check application logs for backend errors

#### Form Submission Issues
- Ensure all required fields are filled
- Check for duplicate player selections
- Verify email format is valid

### Debug Mode
Enable debug logging by adding to `application.yml`:
```yaml
logging:
  level:
    solutions.thonbecker.personal.service.FoosballService: DEBUG
    solutions.thonbecker.personal.controller.FoosballController: DEBUG
```

## Contributing

To extend the foosball integration:

1. **Add New Features**: Extend the service and controller classes
2. **UI Improvements**: Modify the HTML template and CSS
3. **API Integration**: Add new endpoints to the foosball service
4. **Testing**: Add unit tests for new functionality

## Related Projects

- [Foosball Backend Service](https://github.com/SnapPetal/foosball) - The backend API service
- [PersonalWeb](https://github.com/SnapPetal/PersonalWeb) - This project

## License

This integration is part of the PersonalWeb project and follows the same licensing terms.
