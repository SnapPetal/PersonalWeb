// bible-verse.js
document.addEventListener("DOMContentLoaded", function () {
  const bibleVerseElement = document.getElementById("bible-verse");

  if (bibleVerseElement) {
    htmx.on("#bible-verse", "htmx:afterRequest", function (evt) {
      if (evt.detail.successful) {
        try {
          const response = JSON.parse(evt.detail.xhr.responseText);
          evt.detail.target.innerHTML = `
                        <div class="verse-content">
                            <p class="verse-text mb-2">${response.text}</p>
                            <p class="verse-translation mb-2">${response.translation}</p>
                        </div>
                    `;
        } catch (e) {
          console.error("Error parsing JSON:", e);
          evt.detail.target.innerHTML =
            '<p class="text-danger">Error loading verse</p>';
        }
      }
    });
  }
});
