htmx.on('#bible-verse', 'htmx:afterRequest', function(evt) {
    if (evt.detail.successful) {
        const verse = JSON.parse(evt.detail.xhr.response);
        evt.detail.target.innerHTML = `
            <p class="verse-text mb-2">${verse.text}</p>
            <p class="verse-translation text-muted small">${verse.translation}</p>
        `;
    }
});
