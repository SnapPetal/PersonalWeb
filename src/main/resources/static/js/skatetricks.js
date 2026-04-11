"use strict";

var SUPPORTED_TRICKS = [
  { value: "OLLIE", label: "Ollie" },
  { value: "FRONTSIDE_180", label: "Frontside 180" },
  { value: "BACKSIDE_180", label: "Backside 180" },
  { value: "KICKFLIP", label: "Kickflip" },
  { value: "HEELFLIP", label: "Heelflip" },
  { value: "POP_SHUVIT", label: "Pop Shuvit" },
  { value: "TREFLIP", label: "Tre Flip" },
  { value: "BOARDSLIDE", label: "Boardslide" },
  { value: "FIFTY_FIFTY", label: "50-50 Grind" },
  { value: "FIVE_O", label: "5-0 Grind" },
  { value: "NOSEGRIND", label: "Nosegrind" },
  { value: "MANUAL", label: "Manual" },
  { value: "CRUISING", label: "Cruising" },
  { value: "DROP_IN", label: "Drop In" },
  { value: "UNKNOWN", label: "Unknown" },
];

var MAX_FRAME_WIDTH = 640;
var MAX_SEND_FRAMES = 10;

function skatetricksApp() {
  return {
    // ── Reactive UI state ──────────────────────────────────────────
    connectionText: "Disconnected",
    connectionClass: "bg-danger",
    frameCounterText: "Frames: 0",
    cameraStarted: false,
    capturing: false,
    captureBtnLabel: "Capture & Analyze",
    analyzeUploadDisabled: true,
    importUrlLoading: false,
    result: null,
    resultError: null,
    resultLoading: false,
    resultLoadingText: "",
    trickHistory: [],
    supportedTricks: SUPPORTED_TRICKS,

    // ── Internal imperative state (not rendered directly) ──────────
    sessionId:
      "session-" +
      Date.now() +
      "-" +
      Math.random().toString(36).substring(2, 9),
    stompClient: null,
    mediaStream: null,
    capturedFrames: [],
    frameCount: 0,
    captureInterval: null,
    currentVideoId: null,
    currentInputKey: null,
    currentOutputKey: null,
    currentVideoUrl: null,
    importVideoUrl: "",
    plyrPlayer: null,

    // ── Lifecycle ──────────────────────────────────────────────────
    init() {
      this.connectWebSocket();
    },

    // ── CSRF helpers ───────────────────────────────────────────────
    getCsrfToken() {
      return document.querySelector('meta[name="_csrf"]')?.content;
    },

    getCsrfHeader() {
      return document.querySelector('meta[name="_csrf_header"]')?.content;
    },

    getHeaders() {
      var headers = {};
      var header = this.getCsrfHeader();
      var token = this.getCsrfToken();
      if (header && token) {
        headers[header] = token;
      }
      return headers;
    },

    // ── WebSocket (imperative) ─────────────────────────────────────
    connectWebSocket() {
      var self = this;
      var socket = new SockJS("/skatetricks-websocket");
      self.stompClient = Stomp.over(socket);
      self.stompClient.debug = null;

      self.stompClient.connect(
        {},
        function () {
          self.connectionText = "Connected";
          self.connectionClass = "bg-success";

          self.stompClient.subscribe(
            "/topic/skatetricks/result/" + self.sessionId,
            function (message) {
              var result = JSON.parse(message.body);
              self.showResult(result);
              self.addToHistory(result);
            }
          );

          self.stompClient.subscribe(
            "/topic/skatetricks/error/" + self.sessionId,
            function (message) {
              self.result = null;
              self.resultLoading = false;
              self.resultError = message.body;
            }
          );

          self.stompClient.subscribe(
            "/topic/skatetricks/conversion/" + self.sessionId,
            function (message) {
              var status = JSON.parse(message.body);
              self.handleConversionStatus(status);
            }
          );
        },
        function () {
          self.connectionText = "Disconnected";
          self.connectionClass = "bg-danger";
          setTimeout(function () {
            self.connectWebSocket();
          }, 3000);
        }
      );
    },

    // ── Canvas frame capture (imperative) ──────────────────────────
    captureFrame(video) {
      var canvas = document.getElementById("frameCanvas");
      var ctx = canvas.getContext("2d");
      var srcW = video.videoWidth || 640;
      var srcH = video.videoHeight || 480;
      var scale = Math.min(1, MAX_FRAME_WIDTH / srcW);
      canvas.width = Math.round(srcW * scale);
      canvas.height = Math.round(srcH * scale);
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      return canvas.toDataURL("image/jpeg", 0.75).split(",")[1];
    },

    sampleFrames(allFrames, count) {
      if (allFrames.length <= count) return allFrames;
      var sampled = [];
      for (var i = 0; i < count; i++) {
        var idx = Math.round((i * (allFrames.length - 1)) / (count - 1));
        sampled.push(allFrames[idx]);
      }
      return sampled;
    },

    sendFramesForAnalysis(frames) {
      if (!this.stompClient || !this.stompClient.connected) {
        this.result = null;
        this.resultLoading = false;
        this.resultError = "Not connected to server";
        return;
      }
      if (frames.length === 0) {
        this.result = null;
        this.resultLoading = false;
        this.resultError = "No frames captured";
        return;
      }

      this.resultError = null;
      this.result = null;
      this.resultLoading = true;
      this.resultLoadingText = "Analyzing " + frames.length + " frames...";

      this.stompClient.send(
        "/app/skatetricks/analyze",
        {},
        JSON.stringify({
          sessionId: this.sessionId,
          frames: frames,
        })
      );
    },

    // ── Camera controls ────────────────────────────────────────────
    async startCamera() {
      try {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "environment" },
        });
        var liveVideo = document.getElementById("liveVideo");
        liveVideo.srcObject = this.mediaStream;
        this.cameraStarted = true;
      } catch (e) {
        alert("Could not access camera: " + e.message);
      }
    },

    stopCamera() {
      if (this.captureInterval) {
        clearInterval(this.captureInterval);
        this.captureInterval = null;
      }
      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach(function (t) {
          t.stop();
        });
        this.mediaStream = null;
      }
      var liveVideo = document.getElementById("liveVideo");
      liveVideo.srcObject = null;
      this.cameraStarted = false;
      this.capturing = false;
      this.captureBtnLabel = "Capture & Analyze";
    },

    toggleCapture() {
      var self = this;
      if (self.captureInterval) {
        clearInterval(self.captureInterval);
        self.captureInterval = null;
        self.capturing = false;
        self.captureBtnLabel = "Capture & Analyze";
        var toSend = self.sampleFrames(self.capturedFrames, MAX_SEND_FRAMES);
        self.frameCounterText =
          "Captured: " +
          self.capturedFrames.length +
          ", sending: " +
          toSend.length;
        self.sendFramesForAnalysis(toSend);
        self.capturedFrames = [];
        self.frameCount = 0;
      } else {
        self.capturedFrames = [];
        self.frameCount = 0;
        self.capturing = true;
        self.captureBtnLabel = "Stop & Analyze";
        var liveVideo = document.getElementById("liveVideo");
        self.captureInterval = setInterval(function () {
          if (liveVideo.readyState >= 2) {
            self.capturedFrames.push(self.captureFrame(liveVideo));
            self.frameCount++;
            self.frameCounterText = "Recording: " + self.frameCount + " frames";
          }
        }, 200);
      }
    },

    // ── Video upload and conversion ────────────────────────────────
    async handleVideoUpload(event) {
      var self = this;
      var file = event.target.files[0];
      if (!file) return;

      self.currentVideoId = null;
      self.currentInputKey = null;
      self.currentOutputKey = null;
      self.currentVideoUrl = null;
      self.analyzeUploadDisabled = true;

      var plyrContainer = document.getElementById("plyrContainer");
      plyrContainer.style.display = "none";

      if (self.plyrPlayer) {
        self.plyrPlayer.destroy();
        self.plyrPlayer = null;
      }

      plyrContainer.innerHTML = "";
      var uploadedVideo = document.createElement("video");
      uploadedVideo.id = "uploadedVideo";
      uploadedVideo.controls = true;
      uploadedVideo.playsInline = true;
      uploadedVideo.className = "w-100 rounded";
      uploadedVideo.style.maxHeight = "400px";
      plyrContainer.appendChild(uploadedVideo);

      self.frameCounterText = "Uploading video...";

      try {
        var uploadInitResponse = await fetch("/skatetricks/upload-url", {
          method: "POST",
          headers: Object.assign(
            { "Content-Type": "application/json" },
            self.getHeaders()
          ),
          body: JSON.stringify({
            filename: file.name,
            contentType: file.type || "application/octet-stream",
            size: file.size,
          }),
        });

        if (!uploadInitResponse.ok) {
          throw new Error(
            "Failed to create upload URL: " + uploadInitResponse.status
          );
        }

        var uploadInit = await uploadInitResponse.json();
        self.currentVideoId = uploadInit.videoId;
        self.currentInputKey = uploadInit.inputKey;

        self.frameCounterText = "Uploading directly to S3...";

        var uploadResponse = await fetch(uploadInit.uploadUrl, {
          method: "PUT",
          headers: {
            "Content-Type": uploadInit.contentType,
          },
          body: file,
        });

        if (!uploadResponse.ok) {
          throw new Error("S3 upload failed: " + uploadResponse.status);
        }

        self.frameCounterText = "Upload complete. Starting conversion...";

        var convertResponse = await fetch("/skatetricks/convert", {
          method: "POST",
          headers: Object.assign(
            { "Content-Type": "application/json" },
            self.getHeaders()
          ),
          body: JSON.stringify({
            videoId: uploadInit.videoId,
            sessionId: self.sessionId,
            inputKey: uploadInit.inputKey,
            filename: file.name,
          }),
        });

        if (!convertResponse.ok) {
          throw new Error(
            "Failed to start conversion: " + convertResponse.status
          );
        }

        setTimeout(function () {
          self.pollConversionStatus(self.currentVideoId);
        }, 3000);
      } catch (e) {
        self.frameCounterText = "Upload failed: " + e.message;
        self.analyzeUploadDisabled = true;
      }
    },

    async importVideoFromUrl() {
      var self = this;
      if (!self.importVideoUrl || !self.importVideoUrl.trim()) return;

      self.currentVideoId = null;
      self.currentInputKey = null;
      self.currentOutputKey = null;
      self.currentVideoUrl = null;
      self.analyzeUploadDisabled = true;
      self.importUrlLoading = true;
      self.frameCounterText = "Importing remote video...";

      var plyrContainer = document.getElementById("plyrContainer");
      plyrContainer.style.display = "none";

      if (self.plyrPlayer) {
        self.plyrPlayer.destroy();
        self.plyrPlayer = null;
      }

      try {
        var response = await fetch("/skatetricks/import-url", {
          method: "POST",
          headers: Object.assign(
            { "Content-Type": "application/json" },
            self.getHeaders()
          ),
          body: JSON.stringify({
            sessionId: self.sessionId,
            videoUrl: self.importVideoUrl.trim(),
          }),
        });

        if (!response.ok) {
          var errorText = "Failed to import URL: " + response.status;
          throw new Error(errorText);
        }

        var result = await response.json();
        self.currentVideoId = result.videoId;
        self.frameCounterText = "Remote import queued. Starting conversion...";
        setTimeout(function () {
          self.pollConversionStatus(self.currentVideoId);
        }, 2000);
      } catch (e) {
        self.frameCounterText = "URL import failed: " + e.message;
        self.analyzeUploadDisabled = true;
      } finally {
        self.importUrlLoading = false;
      }
    },

    handleConversionStatus(status) {
      if (status.videoId !== this.currentVideoId) return;

      if (status.status === "converting") {
        this.frameCounterText = "Converting video... " + status.progress + "%";
      } else if (status.status === "complete") {
        this.frameCounterText = "Conversion complete! Loading video...";
        this.currentOutputKey = status.outputKey || null;
        this.currentVideoUrl = status.videoUrl || null;
        this.loadConvertedVideo(status.videoUrl, status.size);
      } else if (status.status === "error") {
        this.frameCounterText = status.error || "Conversion failed";
        this.analyzeUploadDisabled = true;
      }
    },

    loadConvertedVideo(videoUrl, size) {
      var self = this;
      var uploadedVideo = document.getElementById("uploadedVideo");
      var plyrContainer = document.getElementById("plyrContainer");
      uploadedVideo.src = videoUrl + "?t=" + Date.now();
      uploadedVideo.load();

      uploadedVideo.addEventListener(
        "loadedmetadata",
        function () {
          plyrContainer.style.display = "block";
          self.plyrPlayer = new Plyr(uploadedVideo, {
            controls: [
              "play",
              "progress",
              "current-time",
              "mute",
              "volume",
              "fullscreen",
            ],
          });
          self.analyzeUploadDisabled = false;
          self.frameCounterText =
            "Converted to MP4 (" +
            Math.round(size / 1024) +
            " KB) — preview and click Analyze";
        },
        { once: true }
      );

      uploadedVideo.addEventListener(
        "error",
        function () {
          self.frameCounterText = "Error loading converted video";
          self.analyzeUploadDisabled = true;
        },
        { once: true }
      );
    },

    pollConversionStatus(videoId) {
      var self = this;
      fetch("/skatetricks/convert/" + videoId + "/status")
        .then(function (response) {
          if (!response.ok) return null;
          return response.json();
        })
        .then(function (status) {
          if (!status || status.videoId !== self.currentVideoId) return;

          if (status.status === "complete") {
            self.currentOutputKey = status.outputKey || null;
            self.currentVideoUrl = status.videoUrl || null;
            self.loadConvertedVideo(status.videoUrl, status.size);
          } else if (status.status === "error") {
            self.frameCounterText = status.error || "Conversion failed";
            self.analyzeUploadDisabled = true;
          } else if (
            status.status === "pending" ||
            status.status === "converting"
          ) {
            setTimeout(function () {
              self.pollConversionStatus(videoId);
            }, 2000);
          }
        })
        .catch(function (e) {
          console.error("Poll error:", e);
        });
    },

    // ── Video analysis ─────────────────────────────────────────────
    async analyzeUpload() {
      if (
        !this.currentVideoId &&
        !this.currentOutputKey &&
        !this.currentVideoUrl
      )
        return;

      this.analyzeUploadDisabled = true;
      this.frameCounterText = "Analyzing video...";
      this.result = null;
      this.resultError = null;
      this.resultLoading = true;
      this.resultLoadingText = "Analyzing video with AI...";

      try {
        var response = await fetch("/skatetricks/analyze", {
          method: "POST",
          headers: Object.assign(
            { "Content-Type": "application/json" },
            this.getHeaders()
          ),
          body: JSON.stringify({
            sessionId: this.sessionId,
            videoId: this.currentVideoId,
            outputKey: this.currentOutputKey,
            videoUrl: this.currentVideoUrl,
          }),
        });

        if (!response.ok) {
          throw new Error("Analysis failed: " + response.status);
        }

        var result = await response.json();
        this.pollAnalysisStatus(result.analysisId);
      } catch (e) {
        this.resultLoading = false;
        this.resultError = "Failed to start analysis: " + e.message;
        this.frameCounterText = "Analysis failed";
        this.analyzeUploadDisabled = false;
      }
    },

    pollAnalysisStatus(analysisId) {
      var self = this;
      fetch("/skatetricks/analyze/" + analysisId + "/status")
        .then(function (response) {
          if (!response.ok) {
            throw new Error(
              "Failed to get analysis status: " + response.status
            );
          }
          return response.json();
        })
        .then(function (status) {
          if (status.status === "complete" && status.result) {
            self.frameCounterText = "Analysis complete";
            self.showResult(status.result);
            self.addToHistory(status.result);
            self.analyzeUploadDisabled = false;
          } else if (status.status === "error") {
            self.resultLoading = false;
            self.resultError =
              "Analysis failed: " + (status.error || "Unknown error");
            self.frameCounterText = "Analysis failed";
            self.analyzeUploadDisabled = false;
          } else {
            setTimeout(function () {
              self.pollAnalysisStatus(analysisId);
            }, 2000);
          }
        })
        .catch(function (error) {
          self.resultLoading = false;
          self.resultError = "Analysis failed: " + error.message;
          self.frameCounterText = "Analysis failed";
          self.analyzeUploadDisabled = false;
        });
    },

    // ── Result display (reactive) ──────────────────────────────────
    showResult(data) {
      this.resultLoading = false;
      this.resultError = null;
      this.result = {
        trick: data.trick,
        trickLabel:
          data.trick === "UNKNOWN"
            ? "Unknown Trick"
            : data.trick.replace(/_/g, " "),
        confidence: data.confidence,
        confidenceColor: this.progressColor(data.confidence),
        formScore: data.formScore,
        formColor: this.progressColor(data.formScore),
        feedback: data.feedback || [],
        trickSequence: (data.trickSequence || []).map(function (entry) {
          return {
            name:
              entry.trick === "UNKNOWN"
                ? "Unknown"
                : entry.trick.replace(/_/g, " "),
            timeframe: entry.timeframe,
            confidence: entry.confidence,
          };
        }),
        attemptId: data.attemptId || null,
        verifyStatus: null,
        showCorrectionPicker: false,
        correctionValue: null,
        correctionOptions: SUPPORTED_TRICKS.filter(function (t) {
          return t.value !== data.trick;
        }),
      };
    },

    progressColor(value) {
      if (value >= 70) return "success";
      if (value >= 40) return "warning";
      return "danger";
    },

    formatTrickName(trick) {
      if (trick === "UNKNOWN") return "Unknown";
      return trick.replace(/_/g, " ");
    },

    // ── Verification ───────────────────────────────────────────────
    confirmTrick(correctedTrickName) {
      var self = this;
      if (!self.result || !self.result.attemptId) return;

      var attemptId = self.result.attemptId;
      var body = correctedTrickName
        ? { correctedTrickName: correctedTrickName }
        : {};

      self.result.verifyStatus = "verifying";

      fetch("/skatetricks/attempts/" + attemptId + "/verify", {
        method: "POST",
        headers: Object.assign(
          { "Content-Type": "application/json" },
          self.getHeaders()
        ),
        body: JSON.stringify(body),
      })
        .then(function (response) {
          if (!response.ok) {
            return response.json().then(function (errorData) {
              throw new Error(
                errorData.error || "Verify failed: " + response.status
              );
            });
          }
          if (correctedTrickName) {
            self.result.verifyStatus = "corrected";
            self.result.correctedLabel = correctedTrickName.replace(/_/g, " ");
          } else {
            self.result.verifyStatus = "confirmed";
          }
          self.updateHistoryVerified(attemptId, correctedTrickName);
        })
        .catch(function (e) {
          console.error("Verification error:", e);
          self.result.verifyStatus = "error";
          self.result.verifyError = e.message;
        });
    },

    showCorrection() {
      if (this.result) {
        this.result.showCorrectionPicker = true;
      }
    },

    submitCorrection() {
      if (!this.result || !this.result.correctionValue) return;
      this.confirmTrick(this.result.correctionValue);
    },

    // ── History ────────────────────────────────────────────────────
    addToHistory(data) {
      this.trickHistory.unshift({
        attemptId: data.attemptId || null,
        name:
          data.trick === "UNKNOWN" ? "Unknown" : data.trick.replace(/_/g, " "),
        confidence: data.confidence,
        verified: false,
      });
    },

    updateHistoryVerified(attemptId, correctedTrickName) {
      for (var i = 0; i < this.trickHistory.length; i++) {
        if (this.trickHistory[i].attemptId === attemptId) {
          this.trickHistory[i].verified = true;
          if (correctedTrickName) {
            this.trickHistory[i].name = correctedTrickName.replace(/_/g, " ");
          }
          break;
        }
      }
    },
  };
}
