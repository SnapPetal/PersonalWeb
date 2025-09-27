// Foosball Management JavaScript

let games = [];
let playerStats = [];

// Initialize the page
document.addEventListener('DOMContentLoaded', function() {
    loadGames();
    loadPlayerStats();
    populatePlayerSelects();
});



// Load games from the API
async function loadGames() {
    try {
        const response = await fetch('/foosball/api/games');
        if (response.ok) {
            games = await response.json();
            updateGamesList();
        }
    } catch (error) {
        console.error('Error loading games:', error);
    }
}

// Load player statistics from the API
async function loadPlayerStats() {
    try {
        const response = await fetch('/foosball/api/stats/players');
        if (response.ok) {
            playerStats = await response.json();
            updatePlayerStatsList();
        }
    } catch (error) {
        console.error('Error loading player stats:', error);
    }
}



// Update the games list display
function updateGamesList() {
    const gamesList = document.getElementById('gamesList');
    if (!gamesList) return;

    if (games.length === 0) {
        gamesList.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="bi bi-trophy display-4"></i>
                <p class="mt-2">No games recorded yet. Record your first game!</p>
            </div>
        `;
        return;
    }

    const tableHTML = `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Team 1</th>
                        <th>Score</th>
                        <th>Team 2</th>
                        <th>Winner</th>
                    </tr>
                </thead>
                <tbody>
                    ${games.map(game => `
                        <tr>
                            <td>${formatGameDate(game.gameDate)}</td>
                            <td>${formatTeamPlayers(game.team1Players)}</td>
                            <td>
                                <span class="badge bg-primary">${game.team1Score || 0}</span>
                                <span class="text-muted">-</span>
                                <span class="badge bg-secondary">${game.team2Score || 0}</span>
                            </td>
                            <td>${formatTeamPlayers(game.team2Players)}</td>
                            <td>
                                <span class="badge bg-success">${game.winner || 'N/A'}</span>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    
    gamesList.innerHTML = tableHTML;
}

// Format team players for display
function formatTeamPlayers(teamPlayers) {
    if (!teamPlayers || !Array.isArray(teamPlayers)) return 'N/A';
    const playerNames = teamPlayers.map(player => player.name || 'Unknown').filter(name => name !== 'Unknown');
    if (playerNames.length === 0) {
        return 'Player names not available';
    }
    return playerNames.join(', ');
}

// Format game date for display
function formatGameDate(dateString) {
    if (!dateString) return 'N/A';
    
    try {
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        // If it's today, show time only
        if (date.toDateString() === now.toDateString()) {
            return `Today at ${date.toLocaleTimeString('en-US', { 
                hour: 'numeric', 
                minute: '2-digit',
                hour12: true 
            })}`;
        }
        
        // If it's yesterday
        const yesterday = new Date(now);
        yesterday.setDate(yesterday.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return `Yesterday at ${date.toLocaleTimeString('en-US', { 
                hour: 'numeric', 
                minute: '2-digit',
                hour12: true 
            })}`;
        }
        
        // If it's within the last 7 days, show day name
        if (diffDays <= 7) {
            return `${date.toLocaleDateString('en-US', { weekday: 'long' })} at ${date.toLocaleTimeString('en-US', { 
                hour: 'numeric', 
                minute: '2-digit',
                hour12: true 
            })}`;
        }
        
        // Otherwise show full date
        return date.toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            year: 'numeric',
            hour: 'numeric', 
            minute: '2-digit',
            hour12: true 
        });
    } catch (error) {
        console.warn('Error formatting date:', dateString, error);
        return dateString; // Fallback to original string
    }
}

// Format user creation time for display
function formatCreatedAt(dateString) {
    if (!dateString) return 'N/A';
    
    try {
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        // If it's today, show "Today"
        if (date.toDateString() === now.toDateString()) {
            return 'Today';
        }
        
        // If it's yesterday, show "Yesterday"
        const yesterday = new Date(now);
        yesterday.setDate(yesterday.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Yesterday';
        }
        
        // If it's within the last 7 days, show day name
        if (diffDays <= 7) {
            return date.toLocaleDateString('en-US', { weekday: 'long' });
        }
        
        // If it's within the last 30 days, show "X days ago"
        if (diffDays <= 30) {
            return `${diffDays} days ago`;
        }
        
        // Otherwise show date only (no time for user creation)
        return date.toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            year: 'numeric'
        });
    } catch (error) {
        console.warn('Error formatting created date:', dateString, error);
        return dateString; // Fallback to original string
    }
}

// Populate player select dropdowns
function populatePlayerSelects() {
    const selects = [
        'team1Player1', 'team1Player2', 
        'team2Player1', 'team2Player2'
    ];
    
    selects.forEach(selectId => {
        const select = document.getElementById(selectId);
        if (select) {
            // Clear existing options except the first one
            select.innerHTML = '<option value="">Select Player</option>';
            
            // Sort players alphabetically by name before adding options
            const sortedPlayers = [...players].sort((a, b) => {
                const nameA = (a.name || 'Unknown Player').toLowerCase();
                const nameB = (b.name || 'Unknown Player').toLowerCase();
                return nameA.localeCompare(nameB);
            });
            
            // Add player options
            sortedPlayers.forEach(player => {
                const option = document.createElement('option');
                option.value = player.id;
                option.textContent = player.name || 'Unknown Player';
                select.appendChild(option);
            });
        }
    });
}

// Add a new player
async function addPlayer() {
    const name = document.getElementById('playerName').value.trim();
    
    if (!name) {
        alert('Please fill in all required fields.');
        return;
    }
    
    try {
        const response = await postWithCsrfAndErrorHandling('/foosball/players', {
            name: name
        });
        
        if (response && response.ok) {
            const newPlayer = await response.json();
            showAlert('Player added successfully!', 'success');
            location.reload();
        } else if (response) {
            throw new Error('Failed to add player');
        }
        // If response is null, error was already handled by postWithCsrfAndErrorHandling
    } catch (error) {
        console.error('Error adding player:', error);
        showAlert(error.message || 'Failed to add player. Please try again.', 'danger');
    }
}

// Add a new game
async function addGame() {
    const team1Player1 = document.getElementById('team1Player1').value;
    const team1Player2 = document.getElementById('team1Player2').value;
    const team2Player1 = document.getElementById('team2Player1').value;
    const team2Player2 = document.getElementById('team2Player2').value;
    const team1Score = parseInt(document.getElementById('team1Score').value);
    const team2Score = parseInt(document.getElementById('team2Score').value);
    const notes = document.getElementById('gameNotes').value.trim();
    
    if (!team1Player1 || !team1Player2 || !team2Player1 || !team2Player2 || 
        isNaN(team1Score) || isNaN(team2Score)) {
        alert('Please fill in all required fields.');
        return;
    }
    
    // Check for duplicate players
    const team1Players = [team1Player1, team1Player2];
    const team2Players = [team2Player1, team2Player2];
    
    if (new Set([...team1Players, ...team2Players]).size !== 4) {
        alert('Each player can only be on one team and cannot play against themselves.');
        return;
    }
    
    try {
        // Get player names from the selected IDs
        const player1Name = players.find(p => p.id == team1Player1)?.name;
        const player2Name = players.find(p => p.id == team1Player2)?.name;
        const player3Name = players.find(p => p.id == team2Player1)?.name;
        const player4Name = players.find(p => p.id == team2Player2)?.name;
        
        const gameData = {
            whiteTeamPlayer1: player1Name,
            whiteTeamPlayer2: player2Name,
            blackTeamPlayer1: player3Name,
            blackTeamPlayer2: player4Name,
            whiteTeamScore: team1Score,
            blackTeamScore: team2Score,
            notes: notes
        };
        
        const response = await postWithCsrfAndErrorHandling('/foosball/games', gameData);
        
        if (response.ok) {
            const newGame = await response.json();
            games.unshift(newGame); // Add to beginning of array
            updateGamesList();
            
            // Refresh player statistics to reflect the new game
            await loadPlayerStats();
            
            // Clear form and close modal
            document.getElementById('addGameForm').reset();
            const modal = bootstrap.Modal.getInstance(document.getElementById('addGameModal'));
            modal.hide();
            
            // Show success message
            showAlert('Game recorded successfully!', 'success');
        } else {
            throw new Error('Failed to record game');
        }
    } catch (error) {
        console.error('Error recording game:', error);
        showAlert('Failed to record game. Please try again.', 'danger');
    }
}

// Update the player statistics list display
function updatePlayerStatsList() {
    const playerStatsList = document.getElementById('playerStatsList');
    if (!playerStatsList) return;

    if (!playerStats || playerStats.length === 0) {
        playerStatsList.innerHTML = `
            <div class="text-center text-muted py-4 foosball-empty-state">
                <i class="bi bi-graph-up display-4"></i>
                <p class="mt-2">No player statistics available yet.</p>
            </div>
        `;
        return;
    }

    const tableHTML = `
        <div class="table-responsive">
            <table class="table table-sm foosball-table">
                <thead>
                    <tr>
                        <th><i class="bi bi-person"></i> Player</th>
                        <th><i class="bi bi-controller"></i> Games</th>
                        <th><i class="bi bi-trophy"></i> Wins</th>
                        <th><i class="bi bi-x-circle"></i> Losses</th>
                        <th><i class="bi bi-percent"></i> Win Rate</th>
                    </tr>
                </thead>
                <tbody>
                    ${playerStats
                        .sort((a, b) => {
                            // Primary sort: by win percentage (descending - higher win rate first)
                            const winPctA = a.winPercentage || 0;
                            const winPctB = b.winPercentage || 0;
                            const winPctComparison = winPctB - winPctA;
                            
                            if (winPctComparison !== 0) {
                                return winPctComparison;
                            }
                            
                            // Secondary sort: if win rates are equal, sort by player name (ascending)
                            const nameA = (a.playerName || a.name || 'Unknown Player').toLowerCase();
                            const nameB = (b.playerName || b.name || 'Unknown Player').toLowerCase();
                            return nameA.localeCompare(nameB);
                        })
                        .map(stat => {
                        // Handle different possible property names from the API
                        const playerName = stat.playerName || stat.name || 'Unknown Player';
                        const gamesPlayed = stat.gamesPlayed || stat.totalGames || 0;
                        const wins = stat.wins || 0;
                        const losses = stat.losses || (gamesPlayed - wins) || 0;
                        const winPercentage = stat.winPercentage || (gamesPlayed > 0 ? (wins / gamesPlayed) * 100 : 0);
                        
                        // No color classes needed - just plain text
                        
                        return `
                            <tr>
                                <td><strong class="foosball-player-name">${playerName}</strong></td>
                                <td><span class="badge bg-info text-dark">${gamesPlayed}</span></td>
                                <td><span class="badge bg-success">${wins}</span></td>
                                <td><span class="badge bg-danger">${losses}</span></td>
                                <td>
                                    ${gamesPlayed > 0 ? 
                                        `<span>${Math.round(winPercentage)}%</span>` : 
                                        '<span class="text-secondary">-</span>'
                                    }
                                </td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        </div>
    `;

    playerStatsList.innerHTML = tableHTML;
}

// Refresh player statistics
async function refreshPlayerStats() {
    const refreshBtn = document.querySelector('.foosball-refresh-btn[onclick="refreshPlayerStats()"]');
    if (refreshBtn) {
        refreshBtn.classList.add('refreshing');
    }
    
    try {
        await loadPlayerStats();
        showAlert('Player statistics refreshed successfully!', 'success');
    } catch (error) {
        console.error('Error refreshing player stats:', error);
        showAlert('Failed to refresh player statistics.', 'danger');
    } finally {
        if (refreshBtn) {
            setTimeout(() => {
                refreshBtn.classList.remove('refreshing');
            }, 1000);
        }
    }
}



// Refresh games list
function refreshGames() {
    const refreshBtn = document.querySelector('.foosball-refresh-btn[onclick="refreshGames()"]');
    if (refreshBtn) {
        refreshBtn.classList.add('refreshing');
        refreshBtn.disabled = true;
    }
    
    loadGames().finally(() => {
        if (refreshBtn) {
            refreshBtn.classList.remove('refreshing');
            refreshBtn.disabled = false;
        }
    });
}

// Show alert message
function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    const container = document.querySelector('.container');
    container.insertBefore(alertDiv, container.firstChild);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

// Handle modal events
document.addEventListener('DOMContentLoaded', function() {
    // Reset forms when modals are hidden
    const addPlayerModal = document.getElementById('addPlayerModal');
    if (addPlayerModal) {
        addPlayerModal.addEventListener('hidden.bs.modal', function() {
            document.getElementById('addPlayerForm').reset();
        });
    }
    
    const addGameModal = document.getElementById('addGameModal');
    if (addGameModal) {
        addGameModal.addEventListener('hidden.bs.modal', function() {
            document.getElementById('addGameForm').reset();
        });
    }
});
