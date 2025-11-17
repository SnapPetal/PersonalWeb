// Tank Game Client
let stompClient = null;
let gameId = null;
let myTankId = null;
let gameState = null;
let canvas = null;
let ctx = null;

// Subscription tracking
let lobbySubscription = null;
let joinedSubscription = null;
let gameStateSubscription = null;
let progressionSubscription = null;
let inputInterval = null;

// Input state
const keys = {
  w: false,
  a: false,
  s: false,
  d: false,
  ArrowUp: false,
  ArrowLeft: false,
  ArrowDown: false,
  ArrowRight: false,
  " ": false,
};
let mouseX = 0;
let mouseY = 0;
let shooting = false;

// Initialize on page load
document.addEventListener("DOMContentLoaded", function () {
  canvas = document.getElementById("gameCanvas");
  ctx = canvas.getContext("2d");

  document.getElementById("joinGameBtn").addEventListener("click", joinGame);
  document
    .getElementById("playerNameInput")
    .addEventListener("keypress", function (e) {
      if (e.key === "Enter") joinGame();
    });
  document.getElementById("playAgainBtn").addEventListener("click", playAgain);

  setupInputHandlers();
  connectWebSocket();
});

function connectWebSocket() {
  const socket = new SockJS("/quiz-websocket");
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: (str) => console.log("STOMP:", str),
  });

  stompClient.onConnect = function (frame) {
    console.log("Connected to WebSocket");
    document.getElementById("lobbyStatus").innerHTML =
      '<span class="text-success"><i class="bi bi-check-circle"></i> Connected</span>';
  };

  stompClient.onStompError = function (frame) {
    console.error("STOMP error:", frame);
    document.getElementById("lobbyStatus").innerHTML =
      '<span class="text-danger"><i class="bi bi-x-circle"></i> Connection error</span>';
  };

  stompClient.activate();
}

function cleanupSubscriptions() {
  // Unsubscribe from all previous subscriptions
  if (lobbySubscription) {
    lobbySubscription.unsubscribe();
    lobbySubscription = null;
  }
  if (joinedSubscription) {
    joinedSubscription.unsubscribe();
    joinedSubscription = null;
  }
  if (gameStateSubscription) {
    gameStateSubscription.unsubscribe();
    gameStateSubscription = null;
  }
  if (progressionSubscription) {
    progressionSubscription.unsubscribe();
    progressionSubscription = null;
  }
  if (inputInterval) {
    clearInterval(inputInterval);
    inputInterval = null;
  }
}

function joinGame() {
  const playerName =
    document.getElementById("playerNameInput").value.trim() || "Player";

  // Clean up old subscriptions first!
  cleanupSubscriptions();

  // First, create or get a game
  stompClient.publish({
    destination: "/app/tankgame/create",
  });

  // Subscribe to lobby to get game ID
  lobbySubscription = stompClient.subscribe(
    "/topic/tankgame/lobby",
    function (message) {
      const game = JSON.parse(message.body);
      gameId = game.gameId;
      console.log("Game created/found:", gameId);

      // Subscribe to join confirmation first
      joinedSubscription = stompClient.subscribe(
        `/topic/tankgame/joined/${gameId}`,
        function (message) {
          const data = JSON.parse(message.body);

          // Only process if this is for us (matching player name)
          if (data.playerName === playerName) {
            myTankId = data.tankId;
            console.log("Joined as tank:", myTankId);

            // Subscribe to progression updates for this tank
            progressionSubscription = stompClient.subscribe(
              `/topic/tankgame/progression/${myTankId}`,
              function (message) {
                const data = JSON.parse(message.body);
                console.log("Progression update:", data);

                updateProgressionUI(data.progression);

                // Show match results if available
                if (data.matchResult) {
                  showMatchResults(data.matchResult, data.progression);
                }
              }
            );

            // Show game section
            document.getElementById("lobbySection").style.display = "none";
            document.getElementById("gameSection").style.display = "block";

            // Start sending input updates
            startInputLoop();
          }
        }
      );

      // Subscribe to game state updates
      gameStateSubscription = stompClient.subscribe(
        `/topic/tankgame/${gameId}`,
        function (message) {
          gameState = JSON.parse(message.body);
          updateGameUI();
          render();
        }
      );

      // Join the game
      stompClient.publish({
        destination: `/app/tankgame/join/${gameId}`,
        body: JSON.stringify({ playerName: playerName }),
      });
    }
  );
}

function setupInputHandlers() {
  // Keyboard
  document.addEventListener("keydown", (e) => {
    if (keys.hasOwnProperty(e.key)) {
      keys[e.key] = true;
      e.preventDefault();
    }
  });

  document.addEventListener("keyup", (e) => {
    if (keys.hasOwnProperty(e.key)) {
      keys[e.key] = false;
      e.preventDefault();
    }
  });

  // Mouse
  canvas.addEventListener("mousemove", (e) => {
    const rect = canvas.getBoundingClientRect();
    mouseX = e.clientX - rect.left;
    mouseY = e.clientY - rect.top;
  });

  canvas.addEventListener("mousedown", (e) => {
    if (e.button === 0) {
      // Left click
      shooting = true;
      e.preventDefault();
    }
  });

  canvas.addEventListener("mouseup", (e) => {
    if (e.button === 0) {
      shooting = false;
    }
  });

  // Prevent context menu on right click
  canvas.addEventListener("contextmenu", (e) => e.preventDefault());
}

function startInputLoop() {
  // Clear any existing interval first
  if (inputInterval) {
    clearInterval(inputInterval);
  }

  inputInterval = setInterval(() => {
    if (!gameId || !myTankId || !stompClient) return;

    const input = {
      up: keys.w || keys.ArrowUp,
      down: keys.s || keys.ArrowDown,
      left: keys.a || keys.ArrowLeft,
      right: keys.d || keys.ArrowRight,
      shoot: shooting || keys[" "],
      mouseX: mouseX,
      mouseY: mouseY,
    };

    stompClient.publish({
      destination: `/app/tankgame/input/${gameId}/${myTankId}`,
      body: JSON.stringify(input),
    });
  }, 50); // Send input 20 times per second
}

function updateGameUI() {
  if (!gameState) return;

  // Update game status
  const statusElement = document.getElementById("gameStatus");
  statusElement.className = "game-status";

  const playAgainBtn = document.getElementById("playAgainBtn");

  switch (gameState.status) {
    case "WAITING":
      statusElement.classList.add("status-waiting");
      statusElement.innerHTML =
        '<i class="bi bi-hourglass-split"></i> Waiting for players...';
      playAgainBtn.style.display = "none";
      break;
    case "PLAYING":
      statusElement.classList.add("status-playing");
      const aliveCount = Object.values(gameState.tanks).filter(
        (t) => t.alive
      ).length;
      statusElement.innerHTML = `<i class="bi bi-controller"></i> Battle in Progress (${aliveCount} tanks alive)`;
      playAgainBtn.style.display = "none";
      break;
    case "FINISHED":
      statusElement.classList.add("status-finished");
      statusElement.innerHTML = `<i class="bi bi-trophy"></i> Winner: ${
        gameState.winnerName || "Draw"
      }!`;
      playAgainBtn.style.display = "inline-block";
      break;
  }

  // Update player list
  const playerListElement = document.getElementById("playerList");
  playerListElement.innerHTML = "";

  Object.values(gameState.tanks).forEach((tank) => {
    const playerCard = document.createElement("div");
    playerCard.className = `player-card ${tank.alive ? "alive" : "dead"}`;

    const isMe = tank.id === myTankId;
    playerCard.innerHTML = `
            <div>
                <span class="color-indicator" style="background-color: ${
                  tank.color
                }"></span>
                <strong>${tank.playerName}${isMe ? " (You)" : ""}</strong>
            </div>
            <small class="text-muted">Kills: ${tank.kills}</small>
            <div class="health-bar">
                <div class="health-fill" style="width: ${tank.health}%"></div>
            </div>
            <small>${tank.health}% HP</small>
        `;

    playerListElement.appendChild(playerCard);
  });
}

function updateProgressionUI(progression) {
  // Update level
  document.getElementById("playerLevel").textContent = progression.level;

  // Update coins
  document.getElementById("playerCoins").textContent = progression.coins;

  // Calculate XP progress
  const currentXp = progression.currentXp;
  const xpForNextLevel = progression.xpForNextLevel;
  const xpProgress = (currentXp / xpForNextLevel) * 100;

  // Update XP bar
  document.getElementById("xpBarFill").style.width = xpProgress + "%";
  document.getElementById(
    "xpBarText"
  ).textContent = `${currentXp} / ${xpForNextLevel} XP`;

  // Update stats
  document.getElementById("statKills").textContent = progression.totalKills;
  document.getElementById("statDeaths").textContent = progression.totalDeaths;
  document.getElementById("statWins").textContent = progression.totalWins;

  // Calculate K/D ratio
  const kd =
    progression.totalDeaths > 0
      ? (progression.totalKills / progression.totalDeaths).toFixed(2)
      : progression.totalKills.toFixed(2);
  document.getElementById("statKD").textContent = kd;
}

function showMatchResults(matchResult, progression) {
  // Get the old level to detect level-ups
  const oldLevel = parseInt(document.getElementById("playerLevel").textContent);
  const newLevel = progression.level;
  const leveledUp = newLevel > oldLevel;

  // Update match results
  const placementText =
    matchResult.placement === 1
      ? "1st Place! 🏆"
      : matchResult.placement === 2
      ? "2nd Place! 🥈"
      : matchResult.placement === 3
      ? "3rd Place! 🥉"
      : `${matchResult.placement}th Place`;

  document.getElementById("matchPlacement").textContent = placementText;
  document.getElementById("matchXP").textContent = matchResult.xpEarned;
  document.getElementById("matchCoins").textContent = matchResult.coinsEarned;

  // Show level-up notification if applicable
  const levelUpNotification = document.getElementById("levelUpNotification");
  if (leveledUp) {
    document.getElementById("newLevel").textContent = newLevel;
    levelUpNotification.style.display = "block";
  } else {
    levelUpNotification.style.display = "none";
  }

  // Show the match results panel
  document.getElementById("matchResults").classList.add("show");

  // Hide match results after 8 seconds or when play again is clicked
  setTimeout(() => {
    document.getElementById("matchResults").classList.remove("show");
  }, 8000);
}

function render() {
  if (!gameState || !ctx) return;

  // Clear canvas
  ctx.fillStyle = "#2d2d2d";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Draw grid (optional, for visual reference)
  ctx.strokeStyle = "#3a3a3a";
  ctx.lineWidth = 1;
  for (let x = 0; x < canvas.width; x += 50) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, canvas.height);
    ctx.stroke();
  }
  for (let y = 0; y < canvas.height; y += 50) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(canvas.width, y);
    ctx.stroke();
  }

  // Draw walls
  ctx.fillStyle = "#666";
  gameState.walls.forEach((wall) => {
    ctx.fillRect(wall.x, wall.y, wall.width, wall.height);
  });

  // Draw projectiles
  gameState.projectiles.forEach((projectile) => {
    ctx.fillStyle = "#ffeb3b";
    ctx.beginPath();
    ctx.arc(projectile.x, projectile.y, projectile.radius, 0, Math.PI * 2);
    ctx.fill();

    // Glow effect
    ctx.shadowBlur = 10;
    ctx.shadowColor = "#ffeb3b";
    ctx.fill();
    ctx.shadowBlur = 0;
  });

  // Draw tanks
  Object.values(gameState.tanks).forEach((tank) => {
    if (!tank.alive) return;

    ctx.save();

    // Tank body
    ctx.fillStyle = tank.color;
    ctx.fillRect(tank.x, tank.y, tank.width, tank.height);

    // Tank outline
    ctx.strokeStyle = tank.id === myTankId ? "#fff" : "#000";
    ctx.lineWidth = tank.id === myTankId ? 3 : 2;
    ctx.strokeRect(tank.x, tank.y, tank.width, tank.height);

    // Tank turret (barrel)
    const centerX = tank.x + tank.width / 2;
    const centerY = tank.y + tank.height / 2;
    const barrelLength = 25;

    ctx.strokeStyle = tank.color;
    ctx.lineWidth = 6;
    ctx.lineCap = "round";
    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.lineTo(
      centerX + Math.cos(tank.rotation) * barrelLength,
      centerY + Math.sin(tank.rotation) * barrelLength
    );
    ctx.stroke();

    // Player name
    ctx.fillStyle = "#fff";
    ctx.font = "12px Arial";
    ctx.textAlign = "center";
    ctx.fillText(tank.playerName, centerX, tank.y - 20);

    // Health bar above tank
    const healthBarWidth = tank.width;
    const healthBarHeight = 4;
    const healthPercent = tank.health / tank.maxHealth;

    ctx.fillStyle = "#333";
    ctx.fillRect(tank.x, tank.y - 10, healthBarWidth, healthBarHeight);

    ctx.fillStyle =
      healthPercent > 0.5
        ? "#28a745"
        : healthPercent > 0.25
        ? "#ffc107"
        : "#dc3545";
    ctx.fillRect(
      tank.x,
      tank.y - 10,
      healthBarWidth * healthPercent,
      healthBarHeight
    );

    ctx.restore();
  });

  // Draw crosshair for player's tank
  if (
    myTankId &&
    gameState.tanks[myTankId] &&
    gameState.tanks[myTankId].alive
  ) {
    ctx.strokeStyle = "#00ff00";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(mouseX, mouseY, 10, 0, Math.PI * 2);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(mouseX - 15, mouseY);
    ctx.lineTo(mouseX - 5, mouseY);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(mouseX + 5, mouseY);
    ctx.lineTo(mouseX + 15, mouseY);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(mouseX, mouseY - 15);
    ctx.lineTo(mouseX, mouseY - 5);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(mouseX, mouseY + 5);
    ctx.lineTo(mouseX, mouseY + 15);
    ctx.stroke();
  }
}

function playAgain() {
  // Hide match results immediately
  document.getElementById("matchResults").classList.remove("show");

  // Leave current game
  if (gameId && myTankId && stompClient) {
    stompClient.publish({
      destination: `/app/tankgame/leave/${gameId}/${myTankId}`,
    });
  }

  // Clean up all subscriptions and intervals
  cleanupSubscriptions();

  // Reset state
  gameId = null;
  myTankId = null;
  gameState = null;

  // Rejoin with same name (preserves progression!)
  setTimeout(() => {
    joinGame();
  }, 500);
}

// Handle page visibility - pause when tab is hidden
document.addEventListener("visibilitychange", function () {
  if (document.hidden) {
    // Reset input when tab is hidden
    Object.keys(keys).forEach((key) => (keys[key] = false));
    shooting = false;
  }
});

// Cleanup on page unload
window.addEventListener("beforeunload", function () {
  if (gameId && myTankId && stompClient) {
    stompClient.publish({
      destination: `/app/tankgame/leave/${gameId}/${myTankId}`,
    });
  }
});
