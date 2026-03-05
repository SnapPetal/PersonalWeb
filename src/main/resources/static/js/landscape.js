// Landscape Planner JavaScript

(function () {
  "use strict";

  // State
  let currentPlanId = null;
  let currentZone = null;
  let canvas = null;
  let selectedPlant = null;

  // CSRF Token
  const csrfToken = document
    .querySelector('meta[name="_csrf"]')
    ?.getAttribute("content");
  const csrfHeader = document
    .querySelector('meta[name="_csrf_header"]')
    ?.getAttribute("content");

  // Initialize on page load
  document.addEventListener("DOMContentLoaded", function () {
    initializeCreatePlanForm();
    initializePlantSearch();
    initializeLoadPlanLinks();
  });

  /**
   * Initializes the create plan form submission
   */
  function initializeCreatePlanForm() {
    const form = document.getElementById("createPlanForm");
    if (!form) return;

    form.addEventListener("submit", async function (e) {
      e.preventDefault();

      const formData = new FormData(form);
      const button = document.getElementById("createPlanBtn");
      const spinner = document.getElementById("uploadSpinner");

      // Disable button and show spinner
      button.disabled = true;
      spinner.classList.remove("d-none");

      try {
        const response = await fetch("/landscape/plans", {
          method: "POST",
          headers: {
            [csrfHeader]: csrfToken,
          },
          body: formData,
        });

        if (response.ok) {
          const plan = await response.json();
          console.log("Plan created:", plan);

          // Store plan info
          currentPlanId = plan.id;
          currentZone = plan.hardinessZone;

          // Display the plan
          displayPlan(plan);

          // Load recommendations
          loadRecommendations(plan.id);

          // Show plant search
          document.getElementById("plantSearchCard").style.display = "block";
          document.getElementById("searchZone").value = currentZone;

          // Reset form
          form.reset();

          // Show success message
          showNotification("Plan created successfully!", "success");
        } else {
          throw new Error("Failed to create plan");
        }
      } catch (error) {
        console.error("Error creating plan:", error);
        showNotification("Failed to create plan. Please try again.", "danger");
      } finally {
        button.disabled = false;
        spinner.classList.add("d-none");
      }
    });
  }

  /**
   * Displays a landscape plan in the workspace
   */
  function displayPlan(plan) {
    const workspace = document.getElementById("planWorkspace");

    const html = `
            <div class="text-center">
                <h5>${plan.name}</h5>
                <p class="text-muted small">${plan.description || ""}</p>
                <img src="${plan.imageCdnUrl}" class="img-fluid rounded" alt="${
      plan.name
    }" style="max-height: 500px;">
                <div class="mt-3">
                    <span class="badge bg-primary">${plan.hardinessZone}</span>
                    <span class="badge bg-success">${
                      plan.placements.length
                    } plants placed</span>
                </div>
            </div>
        `;

    workspace.innerHTML = html;

    // Initialize canvas for annotations (simplified - production would use Fabric.js)
    // For now, just display the image
  }

  /**
   * Loads AI recommendations for a plan
   */
  async function loadRecommendations(planId) {
    const card = document.getElementById("recommendationsCard");
    const container = document.getElementById("recommendations");

    card.style.display = "block";

    try {
      const response = await fetch(
        `/landscape/plans/${planId}/recommendations`
      );
      if (response.ok) {
        const html = await response.text();
        container.innerHTML = html;

        // Initialize add-to-plan buttons
        initializeAddToPlantButtons();
      } else {
        throw new Error("Failed to load recommendations");
      }
    } catch (error) {
      console.error("Error loading recommendations:", error);
      container.innerHTML =
        '<p class="text-danger">Failed to load recommendations.</p>';
    }
  }

  /**
   * Initializes plant search result click handlers
   */
  function initializePlantSearch() {
    document.addEventListener("click", function (e) {
      const plantResult = e.target.closest(".plant-result");
      if (plantResult) {
        e.preventDefault();
        selectPlant(plantResult);
      }
    });
  }

  /**
   * Selects a plant from search results
   */
  function selectPlant(element) {
    // Remove previous selection
    document.querySelectorAll(".plant-result").forEach((el) => {
      el.classList.remove("selected");
    });

    // Mark as selected
    element.classList.add("selected");

    selectedPlant = {
      usdaSymbol: element.dataset.usdaSymbol,
      scientificName: element.dataset.scientificName,
      commonName: element.dataset.commonName,
    };

    showNotification("Plant selected. Click on the image to place it.", "info");
  }

  /**
   * Initializes add-to-plan buttons on recommendation cards
   */
  function initializeAddToPlantButtons() {
    document.querySelectorAll(".add-to-plan-btn").forEach((button) => {
      button.addEventListener("click", function () {
        const card = this.closest(".recommendation-card");
        selectedPlant = {
          usdaSymbol: card.dataset.usdaSymbol,
          scientificName: card.dataset.scientificName,
          commonName: card.dataset.commonName,
        };

        showNotification(
          `${
            selectedPlant.commonName || selectedPlant.scientificName
          } selected. Click on the image to place it.`,
          "success"
        );
      });
    });
  }

  /**
   * Initializes load plan links
   */
  function initializeLoadPlanLinks() {
    document.querySelectorAll(".load-plan-link").forEach((link) => {
      link.addEventListener("click", async function (e) {
        e.preventDefault();
        const planId = this.dataset.planId;
        await loadPlan(planId);
      });
    });
  }

  /**
   * Loads an existing plan
   */
  async function loadPlan(planId) {
    try {
      const response = await fetch(`/landscape/plans/${planId}`);
      if (response.ok) {
        const plan = await response.json();
        currentPlanId = plan.id;
        currentZone = plan.hardinessZone;

        displayPlan(plan);
        loadRecommendations(plan.id);

        // Show plant search
        document.getElementById("plantSearchCard").style.display = "block";
        document.getElementById("searchZone").value = currentZone;

        showNotification("Plan loaded successfully!", "success");
      } else {
        throw new Error("Failed to load plan");
      }
    } catch (error) {
      console.error("Error loading plan:", error);
      showNotification("Failed to load plan.", "danger");
    }
  }

  /**
   * Shows a notification toast
   */
  function showNotification(message, type = "info") {
    // Simple alert for now - could be upgraded to Bootstrap toast
    const alertClass = `alert-${type}`;
    const alert = document.createElement("div");
    alert.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alert.style.zIndex = "9999";
    alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
    document.body.appendChild(alert);

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
      alert.remove();
    }, 5000);
  }

  // Expose global functions if needed
  window.LandscapePlanner = {
    loadPlan,
    showNotification,
  };
})();
