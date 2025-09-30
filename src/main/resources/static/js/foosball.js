// Foosball Management JavaScript
// Simplified version for HTMX-enabled page

// Date formatting utility functions
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// These functions can be used if needed for any remaining client-side logic
// Most functionality is now handled by HTMX and server-side rendering
