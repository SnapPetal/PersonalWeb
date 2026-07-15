htmx.defineExtension("loading-states", {
  onEvent(name, event) {
    if (name === "htmx:beforeRequest") {
      const target = event.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.add("processing");
      }
    }
    if (name === "htmx:afterRequest") {
      const target = event.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.remove("processing");
        if (target.tagName === "FORM" && event.detail.successful) {
          const modal = target.closest(".modal");
          if (modal) {
            const modalInstance = bootstrap.Modal.getInstance(modal);
            if (modalInstance) {
              modalInstance.hide();
            }
          }
        }
      }
    }
  },
});

htmx.defineExtension("form-validation", {
  onEvent(name, event) {
    if (name === "htmx:beforeRequest" && event.detail.elt.tagName === "FORM") {
      const form = event.detail.elt;
      if (!form.checkValidity()) {
        event.preventDefault();
        Array.from(form.elements).forEach((input) => {
          if (!input.validity.valid) {
            input.classList.add("is-invalid");
          }
        });
        return false;
      }
    }
    if (name === "htmx:afterRequest" && event.detail.elt.tagName === "FORM") {
      const form = event.detail.elt;
      Array.from(form.elements).forEach((input) => {
        input.classList.remove("is-invalid");
      });
    }
  },
});

htmx.defineExtension("error-handling", {
  onEvent(name, event) {
    if (name === "htmx:sendError" || name === "htmx:responseError") {
      const target = event.detail.elt;
      target.classList.add("htmx-error");
      setTimeout(() => {
        target.classList.remove("htmx-error");
      }, 500);
    }
  },
});

document.addEventListener("DOMContentLoaded", () => {
  document.body.addEventListener("htmx:responseError", (event) => {
    const status = event.detail.xhr?.status ?? "unknown";
    console.error(
      `HTMX request failed with status ${status}`,
      event.detail.pathInfo?.requestPath
    );
  });

  document.body.addEventListener("htmx:sendError", (event) => {
    console.error("HTMX request could not be sent", event.detail.error);
  });
});
