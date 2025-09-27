// Global CSRF setup - include this in your main template
document.addEventListener('DOMContentLoaded', function() {
    // Set up global fetch interceptor for CSRF
    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
        // Only add CSRF to POST, PUT, DELETE requests
        if (options.method && ['POST', 'PUT', 'DELETE'].includes(options.method.toUpperCase())) {
            options.headers = options.headers || {};
            
            // Add CSRF token if not already present
            const csrfHeader = getCsrfHeader();
            if (csrfHeader && !options.headers[csrfHeader]) {
                options.headers[csrfHeader] = getCsrfToken();
            }
        }
        
        return originalFetch(url, options);
    };
});

// CSRF Token utility functions
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
}