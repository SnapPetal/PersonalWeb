/**
 * CSRF Protection Utilities
 * Centralized CSRF token handling for the application
 */

// Get CSRF token from meta tag
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (!token) {
        console.warn('CSRF token not found in page meta tags');
    }
    return token;
}

// Get CSRF header name from meta tag
function getCsrfHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    if (!header) {
        console.warn('CSRF header name not found in page meta tags');
    }
    return header;
}

// Configure HTMX to include CSRF token in all requests
document.addEventListener('htmx:configRequest', function(evt) {
    const token = getCsrfToken();
    const header = getCsrfHeader();
    if (token && header) {
        evt.detail.headers[header] = token;
    }
});

// Utility function for CSRF-protected POST requests
async function postWithCsrf(url, data, options = {}) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    if (!csrfToken || !csrfHeader) {
        throw new Error('CSRF token or header not available');
    }
    
    const defaultHeaders = {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken
    };
    
    return fetch(url, {
        method: 'POST',
        headers: {
            ...defaultHeaders,
            ...options.headers
        },
        body: JSON.stringify(data),
        ...options
    });
}

// Enhanced POST with error handling
async function postWithCsrfAndErrorHandling(url, data, options = {}) {
    try {
        const response = await postWithCsrf(url, data, options);
        
        if (response.status === 403) {
            console.error('CSRF token validation failed. Token may be expired.');
            throw new Error('Security token expired. Please refresh the page and try again.');
        }
        
        return response;
    } catch (error) {
        console.error('CSRF-protected request failed:', error);
        throw error;
    }
}

// Export functions for module use (if needed)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        getCsrfToken,
        getCsrfHeader,
        postWithCsrf,
        postWithCsrfAndErrorHandling
    };
}