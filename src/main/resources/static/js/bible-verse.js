// bible-verse.js — Alpine.js component
document.addEventListener("alpine:init", () => {
  Alpine.data("bibleVerse", () => ({
    verseText: "",
    greekText: "",
    verseTranslation: "",
    showGreek: false,
    loading: true,
    error: false,

    get hasGreek() {
      return this.greekText !== "";
    },

    get displayText() {
      return this.showGreek ? this.greekText : this.verseText;
    },

    get displayTranslation() {
      return this.showGreek ? "Greek" : this.verseTranslation;
    },

    toggle() {
      if (this.hasGreek) {
        this.showGreek = !this.showGreek;
      }
    },

    init() {
      fetch("/api/bible/verse-of-day")
        .then((response) => {
          if (!response.ok) throw new Error("Failed to load verse");
          return response.json();
        })
        .then((data) => {
          this.verseText = data.text;
          this.greekText = data.greekText || "";
          this.verseTranslation = data.translation;
          this.loading = false;
        })
        .catch(() => {
          this.error = true;
          this.loading = false;
        });
    },
  }));
});
