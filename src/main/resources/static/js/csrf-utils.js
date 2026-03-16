/**
 * CSRF Protection Utilities
 * Centralized CSRF token handling for the application.
 * Supports both meta tag and cookie-based CSRF token delivery.
 */

function getCsrfToken() {
  // Try meta tag first
  const metaToken = document
    .querySelector('meta[name="_csrf"]')
    ?.getAttribute("content");
  if (metaToken) return metaToken;

  // Fall back to XSRF-TOKEN cookie
  const cookies = document.cookie.split(";");
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split("=");
    if (name === "XSRF-TOKEN") {
      return decodeURIComponent(value);
    }
  }
  return "";
}

function getCsrfHeader() {
  const metaHeader = document
    .querySelector('meta[name="_csrf_header"]')
    ?.getAttribute("content");
  return metaHeader || "X-XSRF-TOKEN";
}

// Configure HTMX to include CSRF token in requests
document.addEventListener("htmx:configRequest", (evt) => {
  const token = getCsrfToken();
  const header = getCsrfHeader();
  if (token) {
    evt.detail.headers[header] = token;
  }
});

// Utility function for CSRF-protected POST requests
async function postWithCsrf(url, data, options = {}) {
  const token = getCsrfToken();
  const header = getCsrfHeader();

  if (!token) {
    throw new Error("CSRF token not available");
  }

  const defaultHeaders = {
    "Content-Type": "application/x-www-form-urlencoded",
    [header]: token,
  };

  const body = new URLSearchParams(data).toString();

  return fetch(url, {
    method: "POST",
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
    ...options,
    body: body,
  });
}
