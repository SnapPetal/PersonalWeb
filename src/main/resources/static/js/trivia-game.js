// trivia-game.js
document.addEventListener('DOMContentLoaded', function () {
  // DOM elements - add these ids to your HTML elements
  const connectBtn = document.getElementById('connect-btn');
  const disconnectBtn = document.getElementById('disconnect-btn');
  const createQuizBtn = document.getElementById('create-quiz-btn');
  const startQuizBtn = document.getElementById('start-quiz-btn');
  const playersList = document.getElementById('players-list');
  const connectionStatus = document.getElementById('connection-status');
  const questionElement = document.getElementById('current-question');
  const answersContainer = document.getElementById('answers-container');
  const scoreboardElement = document.getElementById('scoreboard');
  const logElement = document.getElementById('log');

  // Form elements
  const serverUrlInput = document.getElementById('server-url');
  const quizTitleInput = document.getElementById('quiz-title');
  const questionCountInput = document.getElementById('question-count');
  const difficultySelect = document.getElementById('difficulty');
  const playerNameInput = document.getElementById('player-name');

  // WebSocket and game state variables
  let stompClient = null;
  let isConnected = false;
  let currentQuizId = null;
  let currentQuestion = null;
  let playerId = null;
  let playerName = null;

  // Restore saved values from localStorage
  function restoreSavedValues() {
    const host = window.location.host;
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';

    let defaultServerUrl;
    if (host.includes('localhost') || host.includes('127.0.0.1')) {
      // Development environment
      defaultServerUrl = 'ws://localhost:8080/quiz-websocket';
    } else {
      // Production environment
      defaultServerUrl = `${wsProtocol}//endurance.thonbecker.biz/quiz-websocket`;
    }

    // Set the server URL input and disable it
    serverUrlInput.value = defaultServerUrl;
    serverUrlInput.parentElement.classList.add('d-none'); // Hide the form group

    // Restore player name
    const savedPlayerName = localStorage.getItem('trivia-player-name');
    if (savedPlayerName) {
      playerNameInput.value = savedPlayerName;
    }
  }

  // Log function for debugging
  function log(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const logLine = document.createElement('div');
    logLine.className = `text-${type}`;
    logLine.textContent = `[${timestamp}] ${message}`;
    logElement.appendChild(logLine);
    logElement.scrollTop = logElement.scrollHeight;
  }

  // Update UI based on connection state
  function updateConnectionUI(isConnected) {
    console.log('updateConnectionUI called with:', isConnected);

    if (isConnected) {
      connectionStatus.textContent = 'Connected';
      connectionStatus.className = 'badge bg-success';
      connectBtn.classList.add('d-none');
      disconnectBtn.classList.remove('d-none');
      createQuizBtn.disabled = false;
      serverUrlInput.disabled = true;
      playerNameInput.disabled = true;

      console.log('UI updated to Connected state');
    } else {
      connectionStatus.textContent = 'Disconnected';
      connectionStatus.className = 'badge bg-secondary';
      connectBtn.classList.remove('d-none');
      disconnectBtn.classList.add('d-none');
      createQuizBtn.disabled = true;
      startQuizBtn.disabled = true;
      serverUrlInput.disabled = false;
      playerNameInput.disabled = false;

      console.log('UI updated to Disconnected state');
    }

    // Log the final state
    console.log('Final connection status:', connectionStatus.textContent);
    console.log('Final connection class:', connectionStatus.className);
  }

  // Connect to WebSocket
  function connect() {
    playerName = playerNameInput.value.trim();
    if (!playerName) {
      alert('Please enter your name');
      return;
    }

    const serverUrl = serverUrlInput.value.trim();
    if (!serverUrl) {
      alert('Please enter the server URL');
      return;
    }

    // Save values in local storage
    localStorage.setItem('trivia-player-name', playerName);
    localStorage.setItem('trivia-server-url', serverUrl);

    // Generate a unique player ID if not already existing
    playerId = localStorage.getItem('trivia-player-id');
    if (!playerId) {
      playerId = 'player_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
      localStorage.setItem('trivia-player-id', playerId);
    }

    // Update UI
    connectionStatus.textContent = 'Connecting...';
    connectionStatus.className = 'badge bg-warning';

    try {
      // Create WebSocket connection
      let socket;

      // Use native WebSocket for ws:// or wss://, otherwise fall back to SockJS for http:// or https://
      if (serverUrl.startsWith('ws://') || serverUrl.startsWith('wss://')) {
        socket = new WebSocket(serverUrl);
      } else {
        socket = new SockJS(serverUrl);
      }

      stompClient = Stomp.over(socket);

      // Disable debug logs
      stompClient.debug = null;

      // Debug connection events
      socket.onopen = function () {
        log('WebSocket connected successfully', 'success');
        // Update UI to show connecting state
        connectionStatus.textContent = 'Establishing STOMP connection...';
        connectionStatus.className = 'badge bg-warning';
      };

      socket.onclose = function (event) {
        log(`WebSocket closed: ${event.code} - ${event.reason}`, 'warning');
        updateConnectionUI(false);
      };

      socket.onerror = function (error) {
        log(`WebSocket error: ${error.type}`, 'danger');
        console.error('WebSocket error:', error);
        updateConnectionUI(false);
      };

      log(`Connecting to ${serverUrl}...`, 'info');

      // Connect STOMP over WebSocket
      stompClient.connect({}, onConnected, onError);
    } catch (error) {
      log(`Connection error: ${error.message}`, 'danger');
      console.error('Connection error:', error);
      updateConnectionUI(false);
    }
  }

  // Callback when connected to the WebSocket
  function onConnected() {
    isConnected = true;
    log('STOMP connection established successfully', 'success');
    log('Connection status updated to: Connected', 'info');

    // Update the UI
    updateConnectionUI(true);

    // Log the current connection state
    console.log('Connection state updated:', {
      isConnected: isConnected,
      connectionStatus: connectionStatus.textContent,
      connectionClass: connectionStatus.className,
    });

    // Subscribe to quiz creation events
    stompClient.subscribe('/topic/quiz/created', onQuizCreated);

    // Subscribe to player updates
    stompClient.subscribe('/topic/quiz/players', onPlayersUpdated);

    // We'll subscribe to quiz state dynamically when joining a specific quiz
  }

  // Handle connection error
  function onError(error) {
    log('Could not connect to WebSocket server. Please refresh this page to try again!', 'danger');
    console.error('Connection error:', error);
    updateConnectionUI(false);
  }

  // Disconnect from WebSocket
  function disconnect() {
    if (stompClient !== null) {
      stompClient.disconnect();
    }
    isConnected = false;
    log('Disconnected from WebSocket server', 'warning');
    updateConnectionUI(false);
  }

  // Create a new trivia quiz
  function createTriviaQuiz() {
    if (!isConnected) {
      log('Not connected to server', 'warning');
      return;
    }

    const title = quizTitleInput.value.trim();
    if (!title) {
      alert('Please enter a quiz title');
      return;
    }

    const questionCount = parseInt(questionCountInput.value, 10);
    const difficulty = difficultySelect.value;

    const triviaRequest = {
      title: title,
      questionCount: questionCount,
      difficulty: difficulty,
    };

    stompClient.send('/app/quiz/create/trivia', {}, JSON.stringify(triviaRequest));
    log(`Creating trivia quiz: "${title}" with ${questionCount} questions`, 'info');
  }

  // Handle quiz creation event
  function onQuizCreated(payload) {
    const quiz = JSON.parse(payload.body);
    log(`Quiz created: ${quiz.title} with ID ${quiz.id}`, 'success');

    // Store the quiz ID for later use
    currentQuizId = quiz.id;

    // Subscribe to this specific quiz's state updates
    stompClient.subscribe('/topic/quiz/state/' + quiz.id, onQuizStateUpdated);

    // Join the quiz with our player info
    joinQuiz(quiz.id);

    // Show the start button for the quiz creator
    startQuizBtn.disabled = false;
  }

  // Join a quiz
  function joinQuiz(quizId) {
    if (!isConnected) {
      log('Not connected to server', 'warning');
      return;
    }

    const player = {
      id: playerId,
      name: playerName,
    };

    const joinRequest = {
      player: player,
      quizId: quizId,
    };

    stompClient.send('/app/quiz/join', {}, JSON.stringify(joinRequest));
    log(`Joining quiz ${quizId} as ${playerName}`, 'info');
  }

  // Handle player updates
  function onPlayersUpdated(payload) {
    const players = JSON.parse(payload.body);
    log(`Player list updated: ${players.length} players in the quiz`, 'info');

    // Update the UI with the player list
    playersList.innerHTML = '';
    players.forEach((player) => {
      const playerItem = document.createElement('li');
      playerItem.className = 'list-group-item';
      playerItem.textContent = player.name;
      if (player.id === playerId) {
        playerItem.className += ' bg-info';
      }
      playersList.appendChild(playerItem);
    });
  }

  // Start the quiz
  function startQuiz() {
    if (!currentQuizId) {
      log('No quiz selected', 'warning');
      return;
    }

    stompClient.send('/app/quiz/start', {}, currentQuizId.toString());
    log(`Starting quiz ${currentQuizId}`, 'info');
  }

  // Handle quiz state updates
  function onQuizStateUpdated(payload) {
    const state = JSON.parse(payload.body);
    log(`Quiz state updated: ${state.status}`, 'info');

    // Update UI based on quiz state
    updateQuizUI(state);
  }

  // Update the UI based on quiz state
  function updateQuizUI(state) {
    // Hide/show elements based on quiz state
    if (state.status === 'WAITING') {
      document.getElementById('waiting-room').classList.remove('d-none');
      document.getElementById('quiz-area').classList.add('d-none');
      document.getElementById('results-area').classList.add('d-none');
    } else if (state.status === 'IN_PROGRESS') {
      document.getElementById('waiting-room').classList.add('d-none');
      document.getElementById('quiz-area').classList.remove('d-none');
      document.getElementById('results-area').classList.add('d-none');

      // Display current question
      currentQuestion = state.currentQuestion;
      displayQuestion(currentQuestion);

      // Update scoreboard
      updateScoreboard(state.players);
    } else if (state.status === 'COMPLETED') {
      document.getElementById('waiting-room').classList.add('d-none');
      document.getElementById('quiz-area').classList.add('d-none');
      document.getElementById('results-area').classList.remove('d-none');

      // Show final results
      displayResults(state.players);
    }
  }

  // Display current question
  function displayQuestion(question) {
    if (!question) return;

    questionElement.textContent = question.questionText;

    // Clear previous answers
    answersContainer.innerHTML = '';

    // Add answer options
    question.options.forEach((answer, index) => {
      const answerBtn = document.createElement('button');
      answerBtn.className = 'btn btn-outline-primary m-2';
      answerBtn.textContent = answer;
      answerBtn.dataset.index = index;
      answerBtn.addEventListener('click', () => submitAnswer(index));
      answersContainer.appendChild(answerBtn);
    });
  }

  // Submit an answer
  function submitAnswer(answerIndex) {
    if (!currentQuizId || !currentQuestion) {
      log('Cannot submit answer: No active question', 'warning');
      return;
    }

    const submission = {
      quizId: currentQuizId,
      playerId: playerId,
      questionId: currentQuestion.id,
      selectedOption: answerIndex,
      timestamp: new Date().toISOString(),
    };

    stompClient.send('/app/quiz/submit', {}, JSON.stringify(submission));
    log(`Submitted answer ${answerIndex} for question ${currentQuestion.id}`, 'info');

    // Disable all answer buttons
    const answerButtons = answersContainer.querySelectorAll('button');
    answerButtons.forEach((btn) => {
      btn.disabled = true;
      if (parseInt(btn.dataset.index, 10) === answerIndex) {
        btn.classList.add('btn-primary');
        btn.classList.remove('btn-outline-primary');
      }
    });
  }

  // Update scoreboard
  function updateScoreboard(players) {
    scoreboardElement.innerHTML = '';

    // Sort players by score
    players.sort((a, b) => b.score - a.score);

    players.forEach((player) => {
      const playerItem = document.createElement('li');
      playerItem.className = 'list-group-item d-flex justify-content-between align-items-center';
      playerItem.textContent = player.name;

      const scoreSpan = document.createElement('span');
      scoreSpan.className = 'badge bg-primary rounded-pill';
      scoreSpan.textContent = player.score;

      playerItem.appendChild(scoreSpan);

      if (player.id === playerId) {
        playerItem.classList.add('bg-info');
      }

      scoreboardElement.appendChild(playerItem);
    });
  }

  // Display final results
  function displayResults(players) {
    const resultsElement = document.getElementById('final-results');
    resultsElement.innerHTML = '';

    // Sort players by score
    players.sort((a, b) => b.score - a.score);

    // Create a podium-style display
    const winner = players.length > 0 ? players[0] : null;

    if (winner) {
      const winnerElement = document.createElement('div');
      winnerElement.className = 'text-center mb-4';
      winnerElement.innerHTML = `
                <h3>🏆 Winner: ${winner.name} 🏆</h3>
                <p>Score: ${winner.score}</p>
            `;
      resultsElement.appendChild(winnerElement);
    }

    // Create a table for all players
    const table = document.createElement('table');
    table.className = 'table table-striped';

    const thead = document.createElement('thead');
    thead.innerHTML = `
            <tr>
                <th>Rank</th>
                <th>Player</th>
                <th>Score</th>
            </tr>
        `;

    const tbody = document.createElement('tbody');

    players.forEach((player, index) => {
      const row = document.createElement('tr');
      if (player.id === playerId) {
        row.className = 'table-info';
      }

      row.innerHTML = `
                <td>${index + 1}</td>
                <td>${player.name}</td>
                <td>${player.score}</td>
            `;

      tbody.appendChild(row);
    });

    table.appendChild(thead);
    table.appendChild(tbody);
    resultsElement.appendChild(table);
  }

  // Initialize the page
  restoreSavedValues();

  // Set up event listeners
  connectBtn.addEventListener('click', connect);
  disconnectBtn.addEventListener('click', disconnect);
  createQuizBtn.addEventListener('click', createTriviaQuiz);
  startQuizBtn.addEventListener('click', startQuiz);

  // Add connection test functionality
  const testConnectionBtn = document.getElementById('test-connection-btn');
  if (testConnectionBtn) {
    testConnectionBtn.addEventListener('click', testConnection);
  }

  // Test connection function
  function testConnection() {
    const serverUrl = serverUrlInput.value.trim();
    if (!serverUrl) {
      alert('Please enter a server URL to test');
      return;
    }

    log(`Testing connection to ${serverUrl}...`, 'info');

    // For SockJS, we test the info endpoint
    const infoUrl = serverUrl + '/info';

    fetch(infoUrl)
      .then((response) => {
        if (response.ok) {
          log('Connection test successful! SockJS info endpoint is reachable.', 'success');
          return response.json();
        } else {
          log(`Connection test failed: Unable to reach SockJS info endpoint. Status: ${response.status}`, 'danger');
        }
      })
      .then((info) => {
        if (info) {
          log(`Server info: WebSocket enabled: ${info.websocket}`, 'info');
        }
      })
      .catch((error) => {
        log(`Connection test error: ${error.message}`, 'danger');
      });
  }
});
