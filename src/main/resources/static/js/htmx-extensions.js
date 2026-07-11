// Utility extension for handling loading states and modal cleanup.
htmx.defineExtension("loading-states", {
  onEvent: function (name, evt) {
    if (name === "htmx:beforeRequest") {
      const target = evt.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.add("processing");
      }
    }
    if (name === "htmx:afterRequest") {
      const target = evt.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.remove("processing");
        // If it's a successful form submission in a modal
        if (target.tagName === "FORM" && evt.detail.successful) {
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

// Extension for handling form validation
htmx.defineExtension("form-validation", {
  onEvent: function (name, evt) {
    if (name === "htmx:beforeRequest" && evt.detail.elt.tagName === "FORM") {
      const form = evt.detail.elt;
      if (!form.checkValidity()) {
        evt.preventDefault();
        Array.from(form.elements).forEach((input) => {
          if (!input.validity.valid) {
            input.classList.add("is-invalid");
          }
        });
        return false;
      }
    }
    if (name === "htmx:afterRequest" && evt.detail.elt.tagName === "FORM") {
      const form = evt.detail.elt;
      Array.from(form.elements).forEach((input) => {
        input.classList.remove("is-invalid");
      });
    }
  },
});

// Extension for handling error states with visual feedback
htmx.defineExtension("error-handling", {
  onEvent: function (name, evt) {
    if (name === "htmx:sendError" || name === "htmx:responseError") {
      const target = evt.detail.elt;
      target.classList.add("htmx-error");
      setTimeout(() => {
        target.classList.remove("htmx-error");
      }, 500);
    }
  },
});

// Keep server-side HTMX failures visible without duplicating page-specific code.
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
