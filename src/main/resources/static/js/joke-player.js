// joke-player.js
document.addEventListener('DOMContentLoaded', function() {
    initJokePlayer();
});

function initJokePlayer() {
    // Handle successful joke retrieval
    document.addEventListener('htmx:afterRequest', function(evt) {
        if (evt.detail.successful && evt.detail.elt.id === 'navJokeButton') {
            handleJokeResponse(evt.detail.xhr.response);
        }
    });

    // Handle audio completion
    const audioElement = document.getElementById('navJokeAudio');
    if (audioElement) {
        audioElement.addEventListener('ended', handleAudioEnd);
    }

    // Handle errors
    document.addEventListener('htmx:error', function(evt) {
        if (evt.detail.elt.id === 'navJokeButton') {
            resetJokeButton();
        }
    });
}

function handleJokeResponse(response) {
    const audioElement = document.getElementById('navJokeAudio');
    const button = document.getElementById('navJokeButton');

    if (!audioElement || !button) return;

    // Update button state
    button.disabled = true;

    audioElement.src = response;
    audioElement.load();

    // Play the audio with slight delay to ensure loading
    setTimeout(() => {
        audioElement.play()
            .then(() => {
                updateButtonIcon(button, 'bi-volume-up', 'bi-emoji-laughing');
            })
            .catch(error => {
                console.error('Error playing audio:', error);
                resetJokeButton();
            });
    }, 100);
}

function handleAudioEnd() {
    resetJokeButton();
}

function resetJokeButton() {
    const button = document.getElementById('navJokeButton');
    if (button) {
        button.disabled = false;
        updateButtonIcon(button, 'bi-emoji-laughing', 'bi-volume-up');
    }
}

function updateButtonIcon(button, addClassName, removeClassName) {
    const icon = button.querySelector('i');
    if (icon) {
        icon.classList.remove(removeClassName);
        icon.classList.add(addClassName);
    }
}