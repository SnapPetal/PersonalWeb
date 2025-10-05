(() => {
  const ELEMENTS = {
    JOKE_BUTTON: "navJokeButton",
    JOKE_AUDIO: "navJokeAudio",
  };

  const CLASSES = {
    SPINNING: "spinning",
    LAUGH_ICON: "bi-emoji-laughing",
    VOLUME_ICON: "bi-volume-up",
  };

  // DOM utility functions
  const getElement = (id) => document.getElementById(id);

  const toggleButtonState = (button, disabled) => {
    if (!button) return;
    button.disabled = disabled;
  };

  const updateButtonIcon = (button, addClass, removeClass) => {
    const icon = button?.querySelector("i");
    if (icon) {
      icon.classList.remove(removeClass);
      icon.classList.add(addClass);
    }
  };

  // Core functionality
  const resetJokeButton = () => {
    const button = getElement(ELEMENTS.JOKE_BUTTON);
    toggleButtonState(button, false);
    updateButtonIcon(button, CLASSES.LAUGH_ICON, CLASSES.VOLUME_ICON);
    button?.querySelector("i")?.classList.remove(CLASSES.SPINNING);
  };

  const handleJokeResponse = (response) => {
    const audioElement = getElement(ELEMENTS.JOKE_AUDIO);
    const button = getElement(ELEMENTS.JOKE_BUTTON);

    if (!audioElement || !button) return;

    toggleButtonState(button, true);
    button.querySelector("i")?.classList.add(CLASSES.SPINNING);

    audioElement.src = response;
    audioElement.load();

    // Play audio with slight delay to ensure loading
    setTimeout(() => {
      audioElement.play().catch((error) => {
        console.error("Error playing audio:", error);
        resetJokeButton();
      });
    }, 100);
  };

  // Event handlers
  const initJokePlayer = () => {
    // Handle successful joke retrieval
    document.addEventListener("htmx:afterRequest", (evt) => {
      if (evt.detail.successful && evt.detail.elt.id === ELEMENTS.JOKE_BUTTON) {
        handleJokeResponse(evt.detail.xhr.response);
      }
    });

    // Handle audio completion
    const audioElement = getElement(ELEMENTS.JOKE_AUDIO);
    audioElement?.addEventListener("ended", resetJokeButton);

    // Handle errors
    document.addEventListener("htmx:error", (evt) => {
      if (evt.detail.elt.id === ELEMENTS.JOKE_BUTTON) {
        resetJokeButton();
      }
    });
  };

  // Initialize on page load
  document.addEventListener("DOMContentLoaded", initJokePlayer);
})();
