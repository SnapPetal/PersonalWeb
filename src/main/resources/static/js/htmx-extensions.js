// Utility extension for handling loading states and transitions
htmx.defineExtension("loading-states", {
  onEvent: function (name, evt) {
    // Handle loading states for forms and buttons
    if (name === "htmx:beforeRequest") {
      const target = evt.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.add("processing");
        // Find submit buttons - either in the form or the clicked button
        const buttons =
          target.tagName === "FORM"
            ? [
                ...target.querySelectorAll("button[type='submit']"),
                ...document.querySelectorAll(`button[onclick*="${target.id}"]`),
              ]
            : [target];

        buttons.forEach((button) => {
          button.disabled = true;
          if (!button.dataset.originalText) {
            button.dataset.originalText = button.innerHTML;
          }
          button.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...`;
        });
      }
    }
    if (name === "htmx:afterRequest") {
      const target = evt.detail.elt;
      if (target.tagName === "FORM" || target.tagName === "BUTTON") {
        target.classList.remove("processing");
        // Reset all buttons
        const buttons =
          target.tagName === "FORM"
            ? [
                ...target.querySelectorAll("button[type='submit']"),
                ...document.querySelectorAll(`button[onclick*="${target.id}"]`),
              ]
            : [target];

        buttons.forEach((button) => {
          button.disabled = false;
          if (button.dataset.originalText) {
            button.innerHTML = button.dataset.originalText;
          }
        });

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
