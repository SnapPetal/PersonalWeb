function initExperienceCounter() {
    const counter = document.querySelector('.exp-number');
    if (!counter) return;

    const target = parseInt(counter.dataset.count);
    let count = 0;
    const timer = setInterval(() => {
        count++;
        counter.textContent = count;
        if (count === target) {
            clearInterval(timer);
        }
    }, 100);
}

// Initialize when DOM content is loaded
document.addEventListener('DOMContentLoaded', initExperienceCounter);

// Initialize when HTMX triggers a content swap
document.addEventListener('htmx:afterSwap', initExperienceCounter);