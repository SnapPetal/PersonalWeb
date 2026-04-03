// trivia-game.js — Alpine.js component for trivia game
function triviaGame() {
  return {
    // Connection state
    isConnected: false,
    connectionLabel: "Disconnected",
    connectionClass: "badge bg-secondary",
    serverUrl: "",
    playerName: "",
    playerId: null,

    // Quiz state
    currentQuizId: null,
    currentQuestion: null,
    quizStatus: null,
    isQuizCreator: false,
    selectedAnswer: null,
    answerSubmitted: false,

    // Form state
    quizTitle: "",
    questionCount: 10,
    difficulty: "MEDIUM",
    joinQuizId: "",
    creatingQuiz: false,
    quizCreated: false,

    // Lists
    players: [],
    scoreboard: [],
    finalResults: [],
    winner: null,
    logMessages: [],

    // Internal
    _stompClient: null,

    init() {
      this.serverUrl = window.location.origin + "/quiz-websocket";
      const savedName = localStorage.getItem("trivia-player-name");
      if (savedName) {
        this.playerName = savedName;
      }
      this.playerId = localStorage.getItem("trivia-player-id");
      if (!this.playerId) {
        this.playerId =
          "player_" +
          Date.now() +
          "_" +
          Math.random().toString(36).substr(2, 9);
        localStorage.setItem("trivia-player-id", this.playerId);
      }
    },

    log(message, type = "info") {
      const timestamp = new Date().toLocaleTimeString();
      this.logMessages.push({ text: `[${timestamp}] ${message}`, type });
      this.$nextTick(() => {
        const logEl = this.$refs.logContainer;
        if (logEl) {
          logEl.scrollTop = logEl.scrollHeight;
        }
      });
    },

    connect() {
      if (!this.playerName.trim()) {
        alert("Please enter your name");
        return;
      }

      localStorage.setItem("trivia-player-name", this.playerName.trim());

      this.connectionLabel = "Connecting...";
      this.connectionClass = "badge bg-warning";

      try {
        const url = this.serverUrl.trim();
        let socket;
        if (url.startsWith("ws://") || url.startsWith("wss://")) {
          socket = new WebSocket(url);
        } else {
          socket = new SockJS(url);
        }

        this._stompClient = Stomp.over(socket);
        this._stompClient.debug = null;

        socket.onclose = () => {
          this.log("WebSocket closed", "warning");
          this._setDisconnected();
        };

        socket.onerror = (error) => {
          this.log(`WebSocket error: ${error.type}`, "danger");
          this._setDisconnected();
        };

        this.log(`Connecting to ${url}...`, "info");
        this._stompClient.connect(
          {},
          () => this._onConnected(),
          (error) => this._onError(error)
        );
      } catch (error) {
        this.log(`Connection error: ${error.message}`, "danger");
        this._setDisconnected();
      }
    },

    _onConnected() {
      this.isConnected = true;
      this.connectionLabel = "Connected";
      this.connectionClass = "badge bg-success";
      this.log("STOMP connection established successfully", "success");

      this._stompClient.subscribe("/topic/quiz/created", (payload) =>
        this._onQuizCreated(JSON.parse(payload.body))
      );
      this._stompClient.subscribe("/topic/quiz/players", (payload) =>
        this._onPlayersUpdated(JSON.parse(payload.body))
      );
    },

    _onError(error) {
      this.log(
        "Could not connect to WebSocket server. Please refresh this page to try again!",
        "danger"
      );
      console.error("Connection error:", error);
      this._setDisconnected();
    },

    _setDisconnected() {
      this.isConnected = false;
      this.connectionLabel = "Disconnected";
      this.connectionClass = "badge bg-secondary";
    },

    disconnect() {
      if (this._stompClient) {
        this._stompClient.disconnect();
      }
      this._setDisconnected();
      this.log("Disconnected from WebSocket server", "warning");
    },

    createQuiz() {
      if (!this.isConnected) {
        this.log("Not connected to server", "warning");
        return;
      }
      if (!this.quizTitle.trim()) {
        alert("Please enter a quiz title");
        return;
      }

      this.creatingQuiz = true;

      this._stompClient.send(
        "/app/quiz/create/trivia",
        {},
        JSON.stringify({
          title: this.quizTitle.trim(),
          questionCount: parseInt(this.questionCount, 10),
          difficulty: this.difficulty,
          creatorId: this.playerId,
        })
      );
      this.log(
        `Creating trivia quiz: "${this.quizTitle}" with ${this.questionCount} questions`,
        "info"
      );
      this.isQuizCreator = true;
    },

    _onQuizCreated(quiz) {
      this.creatingQuiz = false;
      this.quizCreated = true;
      this.log(`Quiz created: ${quiz.title} with ID ${quiz.id}`, "success");
      this.currentQuizId = quiz.id;

      this._stompClient.subscribe("/topic/quiz/state/" + quiz.id, (payload) =>
        this._onQuizStateUpdated(JSON.parse(payload.body))
      );
      this.log("Subscribed to quiz state updates", "info");

      this._joinQuiz(quiz.id);
    },

    _joinQuiz(quizId) {
      if (!this.isConnected) {
        this.log("Not connected to server", "warning");
        return;
      }

      this._stompClient.send(
        "/app/quiz/join",
        {},
        JSON.stringify({
          player: { id: this.playerId, name: this.playerName.trim() },
          quizId: quizId,
        })
      );
      this.log(`Joining quiz ${quizId} as ${this.playerName.trim()}`, "info");

      this.currentQuizId = quizId;
      this._stompClient.subscribe("/topic/quiz/state/" + quizId, (payload) =>
        this._onQuizStateUpdated(JSON.parse(payload.body))
      );
      this.log("Subscribed to quiz state updates", "info");
    },

    joinQuizById() {
      const quizId = parseInt(this.joinQuizId, 10);
      if (!quizId || quizId < 1) {
        alert("Please enter a valid Quiz ID");
        return;
      }
      this._joinQuiz(quizId);
      this.log(`Attempting to join quiz with ID: ${quizId}`, "info");
    },

    _onPlayersUpdated(players) {
      this.players = players;
      this.log(
        `Player list updated: ${players.length} players in the quiz`,
        "info"
      );
    },

    startQuiz() {
      if (!this.currentQuizId) {
        this.log("No quiz selected", "warning");
        return;
      }
      if (!this.isQuizCreator) {
        this.log("Only the quiz creator can start the game", "warning");
        alert("Only the quiz creator can start the game");
        return;
      }

      this._stompClient.send(
        "/app/quiz/start",
        {},
        JSON.stringify({
          quizId: this.currentQuizId,
          playerId: this.playerId,
        })
      );
      this.log(`Starting quiz ${this.currentQuizId}`, "info");
    },

    _onQuizStateUpdated(state) {
      this.quizStatus = state.status;
      this.log(`Quiz state updated: ${state.status || "UNKNOWN"}`, "info");

      if (state.status === "IN_PROGRESS" || state.status === "STARTED") {
        if (state.currentQuestion) {
          const isNewQuestion =
            !this.currentQuestion ||
            this.currentQuestion.id !== state.currentQuestion.id;
          this.currentQuestion = state.currentQuestion;
          if (isNewQuestion) {
            this.selectedAnswer = null;
            this.answerSubmitted = false;
          }
        }
        if (state.players && state.players.length > 0) {
          this.scoreboard = [...state.players].sort(
            (a, b) => b.score - a.score
          );
        }
      } else if (state.status === "COMPLETED" || state.status === "FINISHED") {
        if (state.players && state.players.length > 0) {
          this.finalResults = [...state.players].sort(
            (a, b) => b.score - a.score
          );
          this.winner =
            this.finalResults.length > 0 ? this.finalResults[0] : null;
        }
      }
    },

    submitAnswer(index) {
      if (!this.currentQuizId || !this.currentQuestion) {
        this.log("Cannot submit answer: No active question", "warning");
        return;
      }

      this.selectedAnswer = index;
      this.answerSubmitted = true;

      this._stompClient.send(
        "/app/quiz/submit",
        {},
        JSON.stringify({
          quizId: this.currentQuizId,
          playerId: this.playerId,
          questionId: this.currentQuestion.id,
          selectedOption: index,
          timestamp: new Date().toISOString(),
        })
      );
      this.log(
        `Submitted answer ${index} for question ${this.currentQuestion.id}`,
        "info"
      );
    },

    nextQuestion() {
      if (!this.currentQuizId) {
        this.log("No quiz selected", "warning");
        return;
      }

      this._stompClient.send(
        "/app/quiz/next",
        {},
        JSON.stringify({ quizId: this.currentQuizId })
      );
      this.log("Moving to next question", "info");
    },

    testConnection() {
      const url = this.serverUrl.trim();
      if (!url) {
        alert("Please enter a server URL to test");
        return;
      }

      this.log(`Testing connection to ${url}...`, "info");
      fetch(url + "/info")
        .then((response) => {
          if (response.ok) {
            this.log(
              "Connection test successful! SockJS info endpoint is reachable.",
              "success"
            );
            return response.json();
          } else {
            this.log(
              `Connection test failed: Status ${response.status}`,
              "danger"
            );
          }
        })
        .then((info) => {
          if (info) {
            this.log(
              `Server info: WebSocket enabled: ${info.websocket}`,
              "info"
            );
          }
        })
        .catch((error) => {
          this.log(`Connection test error: ${error.message}`, "danger");
        });
    },

    // Computed helpers
    get isWaiting() {
      return this.quizStatus === "WAITING" || this.quizStatus === "CREATED";
    },
    get isInProgress() {
      return this.quizStatus === "IN_PROGRESS" || this.quizStatus === "STARTED";
    },
    get isCompleted() {
      return this.quizStatus === "COMPLETED" || this.quizStatus === "FINISHED";
    },
    get startButtonLabel() {
      if (this.quizStatus === "WAITING" || this.quizStatus === "CREATED") {
        return '<i class="bi bi-hourglass-split me-2"></i>Starting...';
      }
      if (this.currentQuizId && this.isQuizCreator) {
        return `<i class="bi bi-play-circle me-2"></i>Start "${this.quizTitle}"`;
      }
      return '<i class="bi bi-play-circle me-2"></i>Start Quiz';
    },
  };
}
