(function () {
  "use strict";

  const sessionId =
    "session-" + Date.now() + "-" + Math.random().toString(36).substring(2, 9);
  let stompClient = null;
  let mediaStream = null;
  let capturedFrames = [];
  let frameCount = 0;
  let captureInterval = null;

  const liveVideo = document.getElementById("liveVideo");
  const frameCanvas = document.getElementById("frameCanvas");
  const ctx = frameCanvas.getContext("2d");

  const startCameraBtn = document.getElementById("startCameraBtn");
  const stopCameraBtn = document.getElementById("stopCameraBtn");
  const captureBtn = document.getElementById("captureBtn");
  const videoUpload = document.getElementById("videoUpload");
  const analyzeUploadBtn = document.getElementById("analyzeUploadBtn");
  const connectionStatus = document.getElementById("connectionStatus");
  const frameCounter = document.getElementById("frameCounter");
  const analysisResult = document.getElementById("analysisResult");
  const trickHistory = document.getElementById("trickHistory");

  const plyrContainer = document.getElementById("plyrContainer");
  let uploadedVideo = document.getElementById("uploadedVideo");
  let plyrPlayer = null;

  // Current converted video ID (for analysis)
  let currentVideoId = null;

  // Get CSRF token for POST requests
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector(
    'meta[name="_csrf_header"]'
  )?.content;

  function getHeaders() {
    const headers = {};
    if (csrfHeader && csrfToken) {
      headers[csrfHeader] = csrfToken;
    }
    return headers;
  }

  function connectWebSocket() {
    const socket = new SockJS("/skatetricks-websocket");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect(
      {},
      function () {
        connectionStatus.textContent = "Connected";
        connectionStatus.className = "badge bg-success";

        stompClient.subscribe(
          "/topic/skatetricks/result/" + sessionId,
          function (message) {
            const result = JSON.parse(message.body);
            displayResult(result);
            addToHistory(result);
          }
        );

        stompClient.subscribe(
          "/topic/skatetricks/error/" + sessionId,
          function (message) {
            analysisResult.innerHTML =
              '<div class="alert alert-danger">' + message.body + "</div>";
          }
        );

        stompClient.subscribe(
          "/topic/skatetricks/conversion/" + sessionId,
          function (message) {
            const status = JSON.parse(message.body);
            handleConversionStatus(status);
          }
        );
      },
      function () {
        connectionStatus.textContent = "Disconnected";
        connectionStatus.className = "badge bg-danger";
        setTimeout(connectWebSocket, 3000);
      }
    );
  }

  var MAX_FRAME_WIDTH = 640;

  function captureFrame(video) {
    var srcW = video.videoWidth || 640;
    var srcH = video.videoHeight || 480;
    var scale = Math.min(1, MAX_FRAME_WIDTH / srcW);
    frameCanvas.width = Math.round(srcW * scale);
    frameCanvas.height = Math.round(srcH * scale);
    ctx.drawImage(video, 0, 0, frameCanvas.width, frameCanvas.height);
    return frameCanvas.toDataURL("image/jpeg", 0.75).split(",")[1];
  }

  function sendFramesForAnalysis(frames) {
    if (!stompClient || !stompClient.connected) {
      analysisResult.innerHTML =
        '<div class="alert alert-warning">Not connected to server</div>';
      return;
    }
    if (frames.length === 0) {
      analysisResult.innerHTML =
        '<div class="alert alert-warning">No frames captured</div>';
      return;
    }

    analysisResult.innerHTML =
      '<div class="text-center"><div class="spinner-border text-primary"></div><p class="mt-2">Analyzing ' +
      frames.length +
      " frames...</p></div>";

    stompClient.send(
      "/app/skatetricks/analyze",
      {},
      JSON.stringify({
        sessionId: sessionId,
        frames: frames,
      })
    );
  }

  function displayResult(result) {
    const confidenceColor =
      result.confidence >= 70
        ? "success"
        : result.confidence >= 40
        ? "warning"
        : "danger";
    const formColor =
      result.formScore >= 70
        ? "success"
        : result.formScore >= 40
        ? "warning"
        : "danger";

    let html =
      '<h4 class="text-center mb-3">' +
      (result.trick === "UNKNOWN"
        ? "Unknown Trick"
        : result.trick.replace(/_/g, " ")) +
      "</h4>";
    html +=
      '<div class="mb-3"><label class="form-label small">Confidence</label>';
    html +=
      '<div class="progress"><div class="progress-bar bg-' +
      confidenceColor +
      '" style="width:' +
      result.confidence +
      '%">' +
      result.confidence +
      "%</div></div></div>";
    html +=
      '<div class="mb-3"><label class="form-label small">Form Score</label>';
    html +=
      '<div class="progress"><div class="progress-bar bg-' +
      formColor +
      '" style="width:' +
      result.formScore +
      '%">' +
      result.formScore +
      "%</div></div></div>";

    if (result.feedback && result.feedback.length > 0) {
      html += '<h6>Feedback</h6><ul class="list-group list-group-flush">';
      result.feedback.forEach(function (item) {
        html +=
          '<li class="list-group-item small"><i class="bi bi-chat-dots me-2"></i>' +
          item +
          "</li>";
      });
      html += "</ul>";
    }
    analysisResult.innerHTML = html;
  }

  function addToHistory(result) {
    const placeholder = trickHistory.querySelector(".text-muted");
    if (placeholder) placeholder.remove();

    const item = document.createElement("div");
    item.className =
      "list-group-item d-flex justify-content-between align-items-center";
    const name =
      result.trick === "UNKNOWN" ? "Unknown" : result.trick.replace(/_/g, " ");
    item.innerHTML =
      "<span>" +
      name +
      '</span><span class="badge bg-primary">' +
      result.confidence +
      "%</span>";
    trickHistory.prepend(item);
  }

  // Live camera
  startCameraBtn.addEventListener("click", async function () {
    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });
      liveVideo.srcObject = mediaStream;
      startCameraBtn.disabled = true;
      stopCameraBtn.disabled = false;
      captureBtn.disabled = false;
    } catch (e) {
      alert("Could not access camera: " + e.message);
    }
  });

  stopCameraBtn.addEventListener("click", function () {
    if (captureInterval) {
      clearInterval(captureInterval);
      captureInterval = null;
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(function (t) {
        t.stop();
      });
      mediaStream = null;
    }
    liveVideo.srcObject = null;
    startCameraBtn.disabled = false;
    stopCameraBtn.disabled = true;
    captureBtn.disabled = true;
    captureBtn.innerHTML =
      '<i class="bi bi-record-circle"></i> Capture & Analyze';
  });

  var MAX_SEND_FRAMES = 10;

  function sampleFrames(allFrames, count) {
    if (allFrames.length <= count) return allFrames;
    var sampled = [];
    for (var i = 0; i < count; i++) {
      var idx = Math.round((i * (allFrames.length - 1)) / (count - 1));
      sampled.push(allFrames[idx]);
    }
    return sampled;
  }

  captureBtn.addEventListener("click", function () {
    if (captureInterval) {
      clearInterval(captureInterval);
      captureInterval = null;
      captureBtn.innerHTML =
        '<i class="bi bi-record-circle"></i> Capture & Analyze';
      var toSend = sampleFrames(capturedFrames, MAX_SEND_FRAMES);
      frameCounter.textContent =
        "Captured: " + capturedFrames.length + ", sending: " + toSend.length;
      sendFramesForAnalysis(toSend);
      capturedFrames = [];
      frameCount = 0;
    } else {
      capturedFrames = [];
      frameCount = 0;
      captureBtn.innerHTML = '<i class="bi bi-stop-fill"></i> Stop & Analyze';
      captureInterval = setInterval(function () {
        if (liveVideo.readyState >= 2) {
          capturedFrames.push(captureFrame(liveVideo));
          frameCount++;
          frameCounter.textContent = "Recording: " + frameCount + " frames";
        }
      }, 200);
    }
  });

  // Handle conversion status updates from WebSocket
  function handleConversionStatus(status) {
    console.log("Conversion status update:", status);

    if (status.videoId !== currentVideoId) {
      console.log("Ignoring status for different video");
      return;
    }

    if (status.status === "converting") {
      frameCounter.textContent = "Converting video... " + status.progress + "%";
    } else if (status.status === "complete") {
      frameCounter.textContent = "Conversion complete! Loading video...";
      loadConvertedVideo(status.videoId, status.size);
    } else if (status.status === "error") {
      frameCounter.textContent = "Conversion failed";
      analyzeUploadBtn.disabled = true;
    }
  }

  function loadConvertedVideo(videoId, size) {
    // Load converted video from server (with cache-busting)
    const videoUrl = "/skatetricks/video/" + videoId + "?t=" + Date.now();
    console.log("Loading video from:", videoUrl);
    uploadedVideo.src = videoUrl;
    uploadedVideo.load();

    uploadedVideo.addEventListener(
      "loadedmetadata",
      function () {
        console.log("Video loaded, duration:", uploadedVideo.duration);
        plyrContainer.style.display = "block";
        plyrPlayer = new Plyr(uploadedVideo, {
          controls: [
            "play",
            "progress",
            "current-time",
            "mute",
            "volume",
            "fullscreen",
          ],
        });
        analyzeUploadBtn.disabled = false;
        frameCounter.textContent =
          "Converted to MP4 (" +
          Math.round(size / 1024) +
          " KB) — preview and click Analyze";
      },
      { once: true }
    );

    uploadedVideo.addEventListener(
      "error",
      function () {
        frameCounter.textContent = "Error loading converted video";
        analyzeUploadBtn.disabled = true;
      },
      { once: true }
    );
  }

  // Poll for conversion status (fallback if WebSocket misses it)
  function pollConversionStatus(videoId) {
    fetch("/skatetricks/convert/" + videoId + "/status")
      .then(function (response) {
        if (!response.ok) return null;
        return response.json();
      })
      .then(function (status) {
        if (!status || status.videoId !== currentVideoId) return;

        if (status.status === "complete") {
          loadConvertedVideo(status.videoId, status.size);
        } else if (status.status === "error") {
          frameCounter.textContent = "Conversion failed";
        } else if (
          status.status === "pending" ||
          status.status === "converting"
        ) {
          // Keep polling
          setTimeout(function () {
            pollConversionStatus(videoId);
          }, 2000);
        }
      })
      .catch(function (e) {
        console.error("Poll error:", e);
      });
  }

  // Upload - converts on server asynchronously
  videoUpload.addEventListener("change", async function (e) {
    const file = e.target.files[0];
    if (!file) return;

    console.log("New file selected:", file.name);

    // Reset UI and state
    currentVideoId = null;
    plyrContainer.style.display = "none";
    analyzeUploadBtn.disabled = true;

    // Destroy existing Plyr instance and recreate video element
    if (plyrPlayer) {
      plyrPlayer.destroy();
      plyrPlayer = null;
    }

    // Clear the entire container and recreate video element
    plyrContainer.innerHTML = "";
    uploadedVideo = document.createElement("video");
    uploadedVideo.id = "uploadedVideo";
    uploadedVideo.controls = true;
    uploadedVideo.playsInline = true;
    uploadedVideo.className = "w-100 rounded";
    uploadedVideo.style.maxHeight = "400px";
    plyrContainer.appendChild(uploadedVideo);

    frameCounter.textContent = "Uploading video...";

    // Upload video - server returns immediately with videoId
    const formData = new FormData();
    formData.append("video", file);
    formData.append("sessionId", sessionId);

    try {
      const response = await fetch("/skatetricks/convert", {
        method: "POST",
        headers: getHeaders(),
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Upload failed: " + response.status);
      }

      const result = await response.json();
      currentVideoId = result.videoId;
      console.log("Got videoId:", currentVideoId, "status:", result.status);

      frameCounter.textContent = "Converting video...";

      // Start polling as fallback (WebSocket should be faster)
      setTimeout(function () {
        pollConversionStatus(currentVideoId);
      }, 3000);
    } catch (e) {
      console.error("Upload error:", e);
      frameCounter.textContent = "Conversion failed: " + e.message;
      analyzeUploadBtn.disabled = true;
    }
  });

  analyzeUploadBtn.addEventListener("click", async function () {
    if (!currentVideoId) return;

    console.log("Analyzing videoId:", currentVideoId);

    analyzeUploadBtn.disabled = true;
    frameCounter.textContent = "Analyzing video...";
    analysisResult.innerHTML =
      '<div class="text-center"><div class="spinner-border text-primary"></div><p class="mt-2">Analyzing video with AI...</p></div>';

    try {
      const analyzeUrl =
        "/skatetricks/analyze/" +
        currentVideoId +
        "?sessionId=" +
        encodeURIComponent(sessionId);
      console.log("Calling:", analyzeUrl);

      const response = await fetch(analyzeUrl, {
        method: "POST",
        headers: getHeaders(),
      });

      if (!response.ok) {
        throw new Error("Analysis failed: " + response.status);
      }

      const result = await response.json();
      frameCounter.textContent = "Analysis complete";
      displayResult(result);
      addToHistory(result);
    } catch (e) {
      console.error("Analysis error:", e);
      analysisResult.innerHTML =
        '<div class="alert alert-danger">Failed to analyze video: ' +
        e.message +
        "</div>";
      frameCounter.textContent = "Analysis failed";
    } finally {
      analyzeUploadBtn.disabled = false;
    }
  });

  connectWebSocket();
})();
