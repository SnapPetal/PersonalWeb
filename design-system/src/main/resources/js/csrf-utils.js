function getCsrfToken() {
  const metaToken = document
    .querySelector('meta[name="_csrf"]')
    ?.getAttribute("content");
  if (metaToken) return metaToken;

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

document.addEventListener("htmx:configRequest", (event) => {
  const token = getCsrfToken();
  const header = getCsrfHeader();
  if (token) {
    event.detail.headers[header] = token;
  }
});

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
    body,
  });
}
