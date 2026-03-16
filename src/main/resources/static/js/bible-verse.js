// bible-verse.js — Alpine.js component
function bibleVerse() {
  return {
    verseText: "",
    verseTranslation: "",
    loading: true,
    error: false,

    init() {
      fetch("/api/bible/verse-of-day")
        .then((response) => {
          if (!response.ok) throw new Error("Failed to load verse");
          return response.json();
        })
        .then((data) => {
          this.verseText = data.text;
          this.verseTranslation = data.translation;
          this.loading = false;
        })
        .catch(() => {
          this.error = true;
          this.loading = false;
        });
    },
  };
}
