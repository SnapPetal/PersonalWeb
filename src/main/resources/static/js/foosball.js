// Foosball Management JavaScript

let players = [];
let games = [];

// Initialize the page
document.addEventListener('DOMContentLoaded', function() {
    loadPlayers();
    loadGames();
    populatePlayerSelects();
});

// Load players from the API
async function loadPlayers() {
    try {
        const response = await fetch('/foosball/api/players');
        if (response.ok) {
            players = await response.json();
            updatePlayersList();
            populatePlayerSelects();
        }
    } catch (error) {
        console.error('Error loading players:', error);
    }
}

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

// Update the players list display
function updatePlayersList() {
    const playersList = document.getElementById('playersList');
    if (!playersList) return;

    if (players.length === 0) {
        playersList.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="bi bi-people display-4"></i>
                <p class="mt-2">No players found. Add your first player to get started!</p>
            </div>
        `;
        return;
    }

    const tableHTML = `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Created</th>
                    </tr>
                </thead>
                <tbody>
                    ${players.map(player => `
                        <tr>
                            <td>${player.name || 'N/A'}</td>
                            <td>${player.email || 'N/A'}</td>
                            <td>${player.createdAt || 'N/A'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    
    playersList.innerHTML = tableHTML;
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
                            <td>${game.gameDate || 'N/A'}</td>
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
    return teamPlayers.map(player => player.name || 'Unknown').join(', ');
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
            
            // Add player options
            players.forEach(player => {
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
    const email = document.getElementById('playerEmail').value.trim();
    
    if (!name || !email) {
        alert('Please fill in all required fields.');
        return;
    }
    
    try {
        const response = await fetch('/foosball/players', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: name,
                email: email
            })
        });
        
        if (response.ok) {
            const newPlayer = await response.json();
            players.push(newPlayer);
            updatePlayersList();
            populatePlayerSelects();
            
            // Clear form and close modal
            document.getElementById('addPlayerForm').reset();
            const modal = bootstrap.Modal.getInstance(document.getElementById('addPlayerModal'));
            modal.hide();
            
            // Show success message
            showAlert('Player added successfully!', 'success');
        } else {
            throw new Error('Failed to add player');
        }
    } catch (error) {
        console.error('Error adding player:', error);
        showAlert('Failed to add player. Please try again.', 'danger');
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
        const gameData = {
            team1Players: [
                { id: parseInt(team1Player1) },
                { id: parseInt(team1Player2) }
            ],
            team2Players: [
                { id: parseInt(team2Player1) },
                { id: parseInt(team2Player2) }
            ],
            team1Score: team1Score,
            team2Score: team2Score,
            notes: notes
        };
        
        const response = await fetch('/foosball/games', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(gameData)
        });
        
        if (response.ok) {
            const newGame = await response.json();
            games.unshift(newGame); // Add to beginning of array
            updateGamesList();
            
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

// Refresh players list
function refreshPlayers() {
    loadPlayers();
}

// Refresh games list
function refreshGames() {
    loadGames();
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
