// joke-player.js — Alpine.js component
document.addEventListener("alpine:init", () => {
  Alpine.data("jokePlayer", () => ({
    playing: false,
    spinning: false,

    init() {
      const audio = this.$refs.jokeAudio;
      if (audio) {
        audio.addEventListener("ended", () => this.reset());
        audio.addEventListener("error", () => this.reset());
      }

      // Listen for HTMX response from joke button
      document.addEventListener("htmx:afterRequest", (evt) => {
        if (evt.detail.successful && evt.detail.elt === this.$refs.jokeBtn) {
          this.handleResponse(evt.detail.xhr.response);
        }
      });

      document.addEventListener("htmx:error", (evt) => {
        if (evt.detail.elt === this.$refs.jokeBtn) {
          this.reset();
        }
      });
    },

    handleResponse(response) {
      const audio = this.$refs.jokeAudio;
      if (!audio) return;

      this.playing = true;
      this.spinning = true;

      audio.src = response;
      audio.type = "audio/mp3";
      audio.load();

      setTimeout(() => {
        audio.play().catch(() => this.reset());
      }, 100);
    },

    reset() {
      this.playing = false;
      this.spinning = false;
    },
  }));
});
