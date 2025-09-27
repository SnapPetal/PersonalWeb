// Alternative CSRF utility using cookies instead of meta tags

// Get CSRF token from cookie
function getCsrfTokenFromCookie() {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

// Utility function for CSRF-protected POST requests using cookie
async function postWithCsrfFromCookie(url, data) {
    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCsrfTokenFromCookie()
        },
        body: JSON.stringify(data)
    });
}