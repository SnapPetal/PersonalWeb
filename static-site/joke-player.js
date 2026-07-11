(() => {
  const button = document.querySelector("[data-joke-toggle]");
  const audio = document.querySelector("[data-joke-audio]");
  if (!button || !audio) return;

  button.addEventListener("click", async () => {
    if (!audio.paused) {
      audio.pause();
      audio.currentTime = 0;
      button.textContent = "😂";
      return;
    }

    button.disabled = true;
    button.textContent = "…";
    try {
      const response = await fetch("/api/joke");
      if (!response.ok) throw new Error("Dad joke unavailable");
      audio.src = await response.text();
      await audio.play();
      button.textContent = "🔊";
    } catch (error) {
      button.textContent = "😂";
    } finally {
      button.disabled = false;
    }
  });

  audio.addEventListener("ended", () => {
    button.textContent = "😂";
  });
})();
