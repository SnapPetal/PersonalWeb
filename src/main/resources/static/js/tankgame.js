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

// Alpine.js component
function tankGameApp() {
  return {
    // Lobby state
    inLobby: true,
    playerName: "",
    lobbyStatusText: "",
    lobbyStatusClass: "text-muted",
    lobbyStatusIcon: "",

    // Game status
    gameStatusText: "Waiting for players...",
    gameStatusClass: "status-waiting",
    gameStatusIcon: "bi-hourglass-split",
    showPlayAgain: false,

    // Player list
    tanks: [],

    // Progression
    level: 1,
    coins: 0,
    xpProgress: 0,
    xpText: "0 / 100 XP",
    statKills: 0,
    statDeaths: 0,
    statWins: 0,
    statKD: "0.00",

    // Match results
    showMatchResults: false,
    matchPlacement: "",
    matchXP: 0,
    matchCoins: 0,
    leveledUp: false,
    newLevel: 2,

    // Match results auto-hide timer
    _matchResultsTimer: null,

    init() {
      this.$nextTick(() => {
        canvas = document.getElementById("gameCanvas");
        ctx = canvas.getContext("2d");
        setupInputHandlers();
        connectWebSocket(this);
      });
    },

    joinGame() {
      const name = this.playerName.trim() || "Player";
      joinGame(this, name);
    },

    playAgain() {
      playAgain(this);
    },
  };
}

function connectWebSocket(app) {
  const socket = new SockJS("/quiz-websocket");
  stompClient = Stomp.over(socket);

  stompClient.connect(
    {},
    function () {
      console.log("Connected to WebSocket");
      app.lobbyStatusText = "Connected";
      app.lobbyStatusClass = "text-success";
      app.lobbyStatusIcon = "bi-check-circle";
    },
    function (error) {
      console.error("STOMP error:", error);
      app.lobbyStatusText = "Connection error";
      app.lobbyStatusClass = "text-danger";
      app.lobbyStatusIcon = "bi-x-circle";
      setTimeout(() => connectWebSocket(app), 5000);
    }
  );
}

function cleanupSubscriptions() {
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

function joinGame(app, playerName) {
  cleanupSubscriptions();

  stompClient.send("/app/tankgame/create", {}, "");

  lobbySubscription = stompClient.subscribe(
    "/topic/tankgame/lobby",
    function (message) {
      const game = JSON.parse(message.body);
      gameId = game.gameId;
      console.log("Game created/found:", gameId);

      joinedSubscription = stompClient.subscribe(
        `/topic/tankgame/joined/${gameId}`,
        function (message) {
          const data = JSON.parse(message.body);

          if (data.playerName === playerName) {
            myTankId = data.tankId;
            console.log("Joined as tank:", myTankId);

            progressionSubscription = stompClient.subscribe(
              `/topic/tankgame/progression/${myTankId}`,
              function (message) {
                const data = JSON.parse(message.body);
                console.log("Progression update:", data);

                updateProgressionUI(app, data.progression);

                if (data.matchResult) {
                  showMatchResults(app, data.matchResult, data.progression);
                }
              }
            );

            app.inLobby = false;
            startInputLoop();
          }
        }
      );

      gameStateSubscription = stompClient.subscribe(
        `/topic/tankgame/${gameId}`,
        function (message) {
          gameState = JSON.parse(message.body);
          updateGameUI(app);
          render();
        }
      );

      stompClient.send(
        `/app/tankgame/join/${gameId}`,
        {},
        JSON.stringify({ playerName: playerName })
      );
    }
  );
}

function setupInputHandlers() {
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

  canvas.addEventListener("mousemove", (e) => {
    const rect = canvas.getBoundingClientRect();
    mouseX = e.clientX - rect.left;
    mouseY = e.clientY - rect.top;
  });

  canvas.addEventListener("mousedown", (e) => {
    if (e.button === 0) {
      shooting = true;
      e.preventDefault();
    }
  });

  canvas.addEventListener("mouseup", (e) => {
    if (e.button === 0) {
      shooting = false;
    }
  });

  canvas.addEventListener("contextmenu", (e) => e.preventDefault());
}

function startInputLoop() {
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

    stompClient.send(
      `/app/tankgame/input/${gameId}/${myTankId}`,
      {},
      JSON.stringify(input)
    );
  }, 50);
}

function updateGameUI(app) {
  if (!gameState) return;

  switch (gameState.status) {
    case "WAITING":
      app.gameStatusClass = "status-waiting";
      app.gameStatusIcon = "bi-hourglass-split";
      app.gameStatusText = "Waiting for players...";
      app.showPlayAgain = false;
      break;
    case "PLAYING":
      app.gameStatusClass = "status-playing";
      app.gameStatusIcon = "bi-controller";
      const aliveCount = Object.values(gameState.tanks).filter(
        (t) => t.alive
      ).length;
      app.gameStatusText = `Battle in Progress (${aliveCount} tanks alive)`;
      app.showPlayAgain = false;
      break;
    case "FINISHED":
      app.gameStatusClass = "status-finished";
      app.gameStatusIcon = "bi-trophy";
      app.gameStatusText = `Winner: ${gameState.winnerName || "Draw"}!`;
      app.showPlayAgain = true;
      break;
  }

  // Update player list for Alpine
  app.tanks = Object.values(gameState.tanks).map((tank) => ({
    id: tank.id,
    playerName: tank.playerName,
    color: tank.color,
    alive: tank.alive,
    kills: tank.kills,
    health: tank.health,
    isMe: tank.id === myTankId,
  }));
}

function updateProgressionUI(app, progression) {
  app.level = progression.level;
  app.coins = progression.coins;

  const currentXp = progression.currentXp;
  const xpForNextLevel = progression.xpForNextLevel;
  app.xpProgress = (currentXp / xpForNextLevel) * 100;
  app.xpText = `${currentXp} / ${xpForNextLevel} XP`;

  app.statKills = progression.totalKills;
  app.statDeaths = progression.totalDeaths;
  app.statWins = progression.totalWins;

  app.statKD =
    progression.totalDeaths > 0
      ? (progression.totalKills / progression.totalDeaths).toFixed(2)
      : progression.totalKills.toFixed(2);
}

function showMatchResults(app, matchResult, progression) {
  const oldLevel = app.level;
  const newLevel = progression.level;

  app.matchPlacement =
    matchResult.placement === 1
      ? "1st Place! \u{1F3C6}"
      : matchResult.placement === 2
      ? "2nd Place! \u{1F948}"
      : matchResult.placement === 3
      ? "3rd Place! \u{1F949}"
      : `${matchResult.placement}th Place`;

  app.matchXP = matchResult.xpEarned;
  app.matchCoins = matchResult.coinsEarned;

  app.leveledUp = newLevel > oldLevel;
  app.newLevel = newLevel;

  app.showMatchResults = true;

  if (app._matchResultsTimer) {
    clearTimeout(app._matchResultsTimer);
  }
  app._matchResultsTimer = setTimeout(() => {
    app.showMatchResults = false;
  }, 8000);
}

function render() {
  if (!gameState || !ctx) return;

  ctx.fillStyle = "#2d2d2d";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Draw grid
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

    ctx.shadowBlur = 10;
    ctx.shadowColor = "#ffeb3b";
    ctx.fill();
    ctx.shadowBlur = 0;
  });

  // Draw tanks
  Object.values(gameState.tanks).forEach((tank) => {
    if (!tank.alive) return;

    ctx.save();

    ctx.fillStyle = tank.color;
    ctx.fillRect(tank.x, tank.y, tank.width, tank.height);

    ctx.strokeStyle = tank.id === myTankId ? "#fff" : "#000";
    ctx.lineWidth = tank.id === myTankId ? 3 : 2;
    ctx.strokeRect(tank.x, tank.y, tank.width, tank.height);

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

    ctx.fillStyle = "#fff";
    ctx.font = "12px Arial";
    ctx.textAlign = "center";
    ctx.fillText(tank.playerName, centerX, tank.y - 20);

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

  // Draw crosshair
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

function playAgain(app) {
  app.showMatchResults = false;
  if (app._matchResultsTimer) {
    clearTimeout(app._matchResultsTimer);
    app._matchResultsTimer = null;
  }

  if (gameId && myTankId && stompClient) {
    stompClient.send(`/app/tankgame/leave/${gameId}/${myTankId}`, {}, "");
  }

  cleanupSubscriptions();

  gameId = null;
  myTankId = null;
  gameState = null;

  const name = app.playerName.trim() || "Player";
  setTimeout(() => {
    joinGame(app, name);
  }, 500);
}

// Handle page visibility - pause when tab is hidden
document.addEventListener("visibilitychange", function () {
  if (document.hidden) {
    Object.keys(keys).forEach((key) => (keys[key] = false));
    shooting = false;
  }
});

// Cleanup on page unload
window.addEventListener("beforeunload", function () {
  if (gameId && myTankId && stompClient) {
    stompClient.send(`/app/tankgame/leave/${gameId}/${myTankId}`, {}, "");
  }
});
