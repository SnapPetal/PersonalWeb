/**
 * CSRF Protection Utilities
 * Centralized CSRF token handling for the application
 */

// Configure HTMX default headers including CSRF token
document.addEventListener('DOMContentLoaded', () => {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

  if (csrfToken && csrfHeader) {
    htmx.config.defaultHeaders = {
      [csrfHeader]: csrfToken,
    };
  }
});

// Configure HTMX to include CSRF token in requests
document.addEventListener('htmx:configRequest', (evt) => {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

  if (csrfToken && csrfHeader) {
    evt.detail.headers[csrfHeader] = csrfToken;
  }
});

// Utility function for CSRF-protected POST requests
async function postWithCsrf(url, data, options = {}) {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

  if (!csrfToken || !csrfHeader) {
    throw new Error('CSRF token or header not available');
  }

  const defaultHeaders = {
    'Content-Type': 'application/x-www-form-urlencoded',
    [csrfHeader]: csrfToken,
  };

  const body = new URLSearchParams(data).toString();

  return fetch(url, {
    method: 'POST',
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
    ...options,
    body: body,
  });
}
