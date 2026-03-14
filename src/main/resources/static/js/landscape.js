// Landscape Planner JavaScript

(function () {
  "use strict";

  // State
  let currentPlanId = null;
  let currentZone = null;
  let canvas = null;
  let selectedPlant = null;
  let placedMarkers = [];
  let keydownHandler = null;

  // Plant type keywords for icon selection
  const PLANT_TYPES = {
    tree: [
      "oak",
      "maple",
      "elm",
      "birch",
      "pine",
      "cedar",
      "willow",
      "ash",
      "poplar",
      "cherry",
      "magnolia",
      "dogwood",
      "spruce",
      "fir",
      "hickory",
      "walnut",
      "beech",
      "sycamore",
      "cypress",
      "redwood",
      "hemlock",
      "linden",
      "locust",
      "chestnut",
      "tree",
    ],
    shrub: [
      "hydrangea",
      "boxwood",
      "holly",
      "azalea",
      "forsythia",
      "lilac",
      "viburnum",
      "juniper",
      "barberry",
      "privet",
      "rhododendron",
      "spirea",
      "weigela",
      "yew",
      "arborvitae",
      "shrub",
      "bush",
    ],
    flower: [
      "rose",
      "lily",
      "daisy",
      "tulip",
      "peony",
      "iris",
      "aster",
      "zinnia",
      "marigold",
      "lavender",
      "petunia",
      "sunflower",
      "coneflower",
      "echinacea",
      "daylily",
      "daffodil",
      "chrysanthemum",
      "geranium",
      "begonia",
      "impatiens",
      "flower",
      "bloom",
    ],
    grass: [
      "grass",
      "sedge",
      "fescue",
      "switchgrass",
      "bluestem",
      "rush",
      "bamboo",
      "pampas",
      "fountain grass",
      "ornamental grass",
    ],
  };

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

  function initializeCreatePlanForm() {
    const form = document.getElementById("createPlanForm");
    if (!form) return;

    form.addEventListener("submit", async function (e) {
      e.preventDefault();

      const formData = new FormData(form);
      const button = document.getElementById("createPlanBtn");
      const spinner = document.getElementById("uploadSpinner");

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
          currentPlanId = plan.id;
          currentZone = plan.hardinessZone;
          displayPlan(plan);
          loadRecommendations(plan.id);

          document.getElementById("plantSearchCard").style.display = "block";
          document.getElementById("searchZone").value = currentZone;
          form.reset();
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

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.appendChild(document.createTextNode(str || ""));
    return div.innerHTML;
  }

  /**
   * Detects plant type from common name for icon selection
   */
  function detectPlantType(name) {
    if (!name) return "default";
    const lower = name.toLowerCase();
    for (const [type, keywords] of Object.entries(PLANT_TYPES)) {
      for (const keyword of keywords) {
        if (lower.includes(keyword)) return type;
      }
    }
    return "default";
  }

  /**
   * Creates a plant silhouette shape group for the Fabric.js canvas
   */
  function createPlantIcon(x, y, name, color, plantType) {
    const objects = [];

    if (plantType === "tree") {
      // Tree: triangle crown + rectangle trunk
      objects.push(
        new fabric.Triangle({
          width: 30,
          height: 28,
          fill: color,
          left: -15,
          top: -30,
          opacity: 0.85,
        })
      );
      objects.push(
        new fabric.Rect({
          width: 6,
          height: 10,
          fill: "#8B4513",
          left: -3,
          top: -2,
          opacity: 0.85,
        })
      );
    } else if (plantType === "shrub") {
      // Shrub: overlapping ellipses
      objects.push(
        new fabric.Ellipse({
          rx: 14,
          ry: 10,
          fill: color,
          left: -14,
          top: -14,
          opacity: 0.8,
        })
      );
      objects.push(
        new fabric.Ellipse({
          rx: 10,
          ry: 8,
          fill: adjustColor(color, 20),
          left: -2,
          top: -18,
          opacity: 0.75,
        })
      );
    } else if (plantType === "flower") {
      // Flower: center circle + petal circles
      const petalCount = 5;
      const petalRadius = 6;
      const distance = 8;
      for (let i = 0; i < petalCount; i++) {
        const angle = (i / petalCount) * Math.PI * 2 - Math.PI / 2;
        objects.push(
          new fabric.Circle({
            radius: petalRadius,
            fill: color,
            left: Math.cos(angle) * distance - petalRadius,
            top: Math.sin(angle) * distance - petalRadius,
            opacity: 0.8,
          })
        );
      }
      objects.push(
        new fabric.Circle({
          radius: 4,
          fill: "#FFD700",
          left: -4,
          top: -4,
          opacity: 0.9,
        })
      );
    } else if (plantType === "grass") {
      // Grass: vertical lines radiating from base
      for (let i = -2; i <= 2; i++) {
        objects.push(
          new fabric.Line([0, 0, i * 4, -18 + Math.abs(i) * 3], {
            stroke: color,
            strokeWidth: 2,
            left: i * 2,
            top: -16 + Math.abs(i) * 2,
            opacity: 0.85,
          })
        );
      }
    } else {
      // Default: leaf shape (two arcs)
      objects.push(
        new fabric.Ellipse({
          rx: 10,
          ry: 14,
          fill: color,
          left: -10,
          top: -18,
          opacity: 0.8,
        })
      );
    }

    const group = new fabric.Group(objects, {
      left: x - 15,
      top: y - 20,
      selectable: true,
      hasControls: false,
      hasBorders: false,
      lockMovementX: true,
      lockMovementY: true,
      hoverCursor: "pointer",
      shadow: new fabric.Shadow({
        color: "rgba(0,0,0,0.3)",
        blur: 4,
        offsetX: 1,
        offsetY: 2,
      }),
    });

    group._isPlantMarker = true;
    group._plantName = name;

    return group;
  }

  /**
   * Adjusts a hex color by a brightness offset
   */
  function adjustColor(hex, amount) {
    const num = parseInt(hex.slice(1), 16);
    const r = Math.min(255, ((num >> 16) & 0xff) + amount);
    const g = Math.min(255, ((num >> 8) & 0xff) + amount);
    const b = Math.min(255, (num & 0xff) + amount);
    return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
  }

  function displayPlan(plan) {
    if (canvas) {
      canvas.dispose();
      canvas = null;
    }
    if (keydownHandler) {
      document.removeEventListener("keydown", keydownHandler);
      keydownHandler = null;
    }

    const workspace = document.getElementById("planWorkspace");
    placedMarkers = [];

    const html = `
            <div class="text-center mb-2">
                <h5>${escapeHtml(plan.name)}</h5>
                <p class="text-muted small mb-1">${escapeHtml(
                  plan.description
                )}</p>
                <div class="mb-2">
                    <span class="badge bg-primary">${escapeHtml(
                      plan.hardinessZone
                    )}</span>
                    <span class="badge bg-success" id="plantCountBadge">${
                      plan.placements.length
                    } plants placed</span>
                </div>
            </div>
            <div id="canvasHelp" class="alert alert-info py-2 small d-none">
                <i class="bi bi-info-circle"></i>
                Select a plant from recommendations or search, then click on the image to place it.
                Click a placed marker to select it, then press <kbd>Delete</kbd> to remove.
            </div>
            <div class="canvas-wrapper" id="canvasWrapper">
                <canvas id="planCanvas"></canvas>
            </div>
            <div id="plantLegend" class="mt-2"></div>
            <div class="text-center mt-3">
                <button class="btn btn-outline-info" id="seasonalPreviewBtn" disabled>
                    <i class="bi bi-calendar4-range"></i> Seasonal Preview
                </button>
            </div>
            <div id="seasonalPreview" class="mt-3" style="display:none;"></div>
        `;

    workspace.innerHTML = html;

    document
      .getElementById("seasonalPreviewBtn")
      .addEventListener("click", function () {
        loadSeasonalPreview(currentPlanId);
      });

    initializeCanvas(plan.imageCdnUrl, plan.placements);
  }

  function initializeCanvas(imageUrl, existingPlacements) {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onerror = function () {
      console.warn("Image failed to load with CORS, retrying without...");
      const retryImg = new Image();
      retryImg.onload = function () {
        setupCanvas(retryImg, existingPlacements);
      };
      retryImg.onerror = function () {
        console.error("Failed to load image:", imageUrl);
        showNotification("Failed to load landscape image.", "danger");
      };
      retryImg.src = imageUrl;
    };
    img.onload = function () {
      setupCanvas(img, existingPlacements);
    };
    img.src = imageUrl;
  }

  function setupCanvas(img, existingPlacements) {
    const wrapper = document.getElementById("canvasWrapper");
    const maxWidth = wrapper.parentElement.clientWidth;
    const scale = Math.min(1, maxWidth / img.width);
    const canvasWidth = Math.floor(img.width * scale);
    const canvasHeight = Math.floor(img.height * scale);

    canvas = new fabric.Canvas("planCanvas", {
      width: canvasWidth,
      height: canvasHeight,
      selection: false,
    });

    canvas.setBackgroundImage(
      new fabric.Image(img, {
        scaleX: scale,
        scaleY: scale,
      }),
      canvas.renderAll.bind(canvas)
    );

    // Place existing markers
    if (existingPlacements && existingPlacements.length > 0) {
      existingPlacements.forEach(function (placement) {
        addMarkerToCanvas(
          (placement.xCoord / 100) * canvasWidth,
          (placement.yCoord / 100) * canvasHeight,
          placement.commonName || placement.plantName,
          placement.usdaSymbol,
          true
        );
      });
    }

    // Click to place a plant
    canvas.on("mouse:down", function (opt) {
      if (opt.target) return;
      if (!selectedPlant) {
        showNotification(
          "Select a plant first from recommendations or search results.",
          "warning"
        );
        return;
      }
      if (!currentPlanId) {
        showNotification("No plan loaded.", "warning");
        return;
      }

      const pointer = canvas.getPointer(opt.e);
      const xPercent = (pointer.x / canvasWidth) * 100;
      const yPercent = (pointer.y / canvasHeight) * 100;

      savePlacement(currentPlanId, selectedPlant, xPercent, yPercent);

      addMarkerToCanvas(
        pointer.x,
        pointer.y,
        selectedPlant.commonName || selectedPlant.scientificName,
        selectedPlant.usdaSymbol,
        false
      );

      showNotification(
        `Placed ${selectedPlant.commonName || selectedPlant.scientificName}`,
        "success"
      );

      updatePlantCount();
      updateLegend();
    });

    keydownHandler = function (e) {
      if (e.key === "Delete" || e.key === "Backspace") {
        const active = canvas.getActiveObject();
        if (active && active._isPlantMarker) {
          canvas.remove(active);
          if (active._labelObj) canvas.remove(active._labelObj);
          placedMarkers = placedMarkers.filter(function (m) {
            return m.marker !== active;
          });
          updatePlantCount();
          updateLegend();
          canvas.discardActiveObject();
          canvas.renderAll();
        }
      }
    };
    document.addEventListener("keydown", keydownHandler);

    document.getElementById("canvasHelp")?.classList.remove("d-none");

    // Enable seasonal preview if plants exist
    updateSeasonalButton();
    updateLegend();
  }

  function addMarkerToCanvas(x, y, name, usdaSymbol, isExisting) {
    const markerColor = getPlantColor(usdaSymbol);
    const plantType = detectPlantType(name);

    // Start with silhouette icon, then try to load real plant image
    const icon = createPlantIcon(x, y, name, markerColor, plantType);
    icon._usdaSymbol = usdaSymbol;

    // Label below the icon
    const label = new fabric.Text((name || usdaSymbol).substring(0, 8), {
      fontSize: 9,
      fontWeight: "bold",
      fill: "#fff",
      fontFamily: "sans-serif",
      textAlign: "center",
      left: x - 20,
      top: y + 8,
      selectable: false,
      evented: false,
      backgroundColor: "rgba(0,0,0,0.5)",
      padding: 2,
    });

    icon._labelObj = label;

    canvas.add(icon);
    canvas.add(label);

    const markerEntry = {
      marker: icon,
      label: label,
      name: name,
      usdaSymbol: usdaSymbol,
      isExisting: isExisting,
    };
    placedMarkers.push(markerEntry);

    setupMarkerEvents(icon, name);

    // Try to load real plant image from USDA
    loadPlantImage(usdaSymbol, name, x, y, markerColor, markerEntry);

    updateSeasonalButton();
  }

  /**
   * Fetches a plant image from USDA and replaces the silhouette icon on the canvas
   */
  function loadPlantImage(symbol, name, x, y, borderColor, markerEntry) {
    if (!symbol) return;

    fetch(`/landscape/plants/image?symbol=${encodeURIComponent(symbol)}`)
      .then(function (response) {
        if (!response.ok) return null;
        return response.json();
      })
      .then(function (data) {
        if (!data || !data.imageUrl || !canvas) return;

        const img = new Image();
        img.crossOrigin = "anonymous";
        img.onload = function () {
          // Create circular clipped plant image
          const size = 40;
          const fabricImg = new fabric.Image(img, {
            left: x - size / 2,
            top: y - size / 2,
            scaleX: size / img.width,
            scaleY: size / img.height,
            selectable: true,
            hasControls: false,
            hasBorders: false,
            lockMovementX: true,
            lockMovementY: true,
            hoverCursor: "pointer",
            clipPath: new fabric.Circle({
              radius: size / 2,
              originX: "center",
              originY: "center",
            }),
            stroke: borderColor,
            strokeWidth: 3,
            shadow: new fabric.Shadow({
              color: "rgba(0,0,0,0.4)",
              blur: 5,
              offsetX: 1,
              offsetY: 2,
            }),
          });

          fabricImg._isPlantMarker = true;
          fabricImg._plantName = name;
          fabricImg._usdaSymbol = markerEntry.usdaSymbol;
          fabricImg._labelObj = markerEntry.label;

          // Replace silhouette with real image
          canvas.remove(markerEntry.marker);
          canvas.add(fabricImg);
          markerEntry.marker = fabricImg;

          setupMarkerEvents(fabricImg, name);

          canvas.renderAll();
        };
        img.onerror = function () {
          // Keep the silhouette icon
        };
        img.src = data.imageUrl;
      })
      .catch(function () {
        // Keep the silhouette icon on error
      });
  }

  function setupMarkerEvents(obj, name) {
    obj.on("mouseover", function () {
      obj.set({ opacity: 1 });
      obj.setShadow(
        new fabric.Shadow({
          color: "rgba(255,193,7,0.6)",
          blur: 8,
          offsetX: 0,
          offsetY: 0,
        })
      );
      canvas.renderAll();
      showTooltip(obj, name);
    });

    obj.on("mouseout", function () {
      obj.set({ opacity: 1 });
      obj.setShadow(
        new fabric.Shadow({
          color: "rgba(0,0,0,0.3)",
          blur: 4,
          offsetX: 1,
          offsetY: 2,
        })
      );
      canvas.renderAll();
      hideTooltip();
    });
  }

  function showTooltip(obj, text) {
    let tooltip = document.getElementById("plantTooltip");
    if (!tooltip) {
      tooltip = document.createElement("div");
      tooltip.id = "plantTooltip";
      tooltip.className = "plant-tooltip";
      document.getElementById("canvasWrapper").appendChild(tooltip);
    }
    tooltip.textContent = text;
    tooltip.style.left = obj.left + 35 + "px";
    tooltip.style.top = obj.top + "px";
    tooltip.style.display = "block";
  }

  function hideTooltip() {
    const tooltip = document.getElementById("plantTooltip");
    if (tooltip) tooltip.style.display = "none";
  }

  function getPlantColor(usdaSymbol) {
    const colors = [
      "#198754",
      "#0d6efd",
      "#dc3545",
      "#fd7e14",
      "#6f42c1",
      "#20c997",
      "#d63384",
      "#0dcaf0",
      "#6610f2",
      "#ffc107",
    ];
    let hash = 0;
    for (let i = 0; i < usdaSymbol.length; i++) {
      hash = usdaSymbol.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  async function savePlacement(planId, plant, xCoord, yCoord) {
    try {
      const response = await fetch(`/landscape/plans/${planId}/placements`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          [csrfHeader]: csrfToken,
        },
        body: JSON.stringify({
          usdaSymbol: plant.usdaSymbol,
          plantName: plant.scientificName || "",
          commonName: plant.commonName || "",
          xCoord: xCoord,
          yCoord: yCoord,
          notes: "",
        }),
      });

      if (!response.ok) {
        throw new Error("Failed to save placement");
      }
    } catch (error) {
      console.error("Error saving placement:", error);
      showNotification("Failed to save plant placement.", "danger");
    }
  }

  function updatePlantCount() {
    const badge = document.getElementById("plantCountBadge");
    if (badge) {
      badge.textContent = placedMarkers.length + " plants placed";
    }
  }

  function updateSeasonalButton() {
    const btn = document.getElementById("seasonalPreviewBtn");
    if (btn) {
      btn.disabled = placedMarkers.length === 0;
    }
  }

  function updateLegend() {
    const legend = document.getElementById("plantLegend");
    if (!legend) return;

    const groups = {};
    placedMarkers.forEach(function (m) {
      const key = m.usdaSymbol;
      if (!groups[key]) {
        groups[key] = {
          name: m.name,
          usdaSymbol: m.usdaSymbol,
          count: 0,
          type: detectPlantType(m.name),
        };
      }
      groups[key].count++;
    });

    const entries = Object.values(groups);
    if (entries.length === 0) {
      legend.innerHTML = "";
      return;
    }

    const typeIcons = {
      tree: "bi-tree",
      shrub: "bi-flower2",
      flower: "bi-flower1",
      grass: "bi-moisture",
      default: "bi-flower3",
    };

    let html = '<div class="d-flex flex-wrap gap-2">';
    entries.forEach(function (entry) {
      const color = getPlantColor(entry.usdaSymbol);
      const icon = typeIcons[entry.type] || typeIcons["default"];
      html += `<span class="badge" style="background-color: ${color};">
                <i class="bi ${icon}"></i> ${escapeHtml(entry.name)} (${
        entry.count
      })
            </span>`;
    });
    html += "</div>";
    legend.innerHTML = html;
  }

  /**
   * Loads seasonal preview from AI
   */
  async function loadSeasonalPreview(planId) {
    const container = document.getElementById("seasonalPreview");
    const btn = document.getElementById("seasonalPreviewBtn");

    if (!container || !planId) return;

    btn.disabled = true;
    btn.innerHTML =
      '<span class="spinner-border spinner-border-sm"></span> Generating seasonal images...';
    container.style.display = "none";

    try {
      const response = await fetch(`/landscape/plans/${planId}/seasons`);
      if (!response.ok) throw new Error("Failed to load seasonal analysis");

      const analysis = await response.json();
      container.style.display = "block";
      renderSeasonalPreview(analysis);
    } catch (error) {
      console.error("Error loading seasonal preview:", error);
      container.style.display = "block";
      container.innerHTML =
        '<div class="alert alert-danger">Failed to generate seasonal preview. Make sure you have plants placed on your plan.</div>';
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-calendar4-range"></i> Seasonal Preview';
    }
  }

  function renderSeasonalPreview(analysis) {
    const container = document.getElementById("seasonalPreview");
    if (!container) return;

    const seasons = [
      {
        key: "spring",
        label: "Spring",
        icon: "bi-flower1",
        color: "success",
        data: analysis.spring,
      },
      {
        key: "summer",
        label: "Summer",
        icon: "bi-sun",
        color: "warning",
        data: analysis.summer,
      },
      {
        key: "fall",
        label: "Fall",
        icon: "bi-leaf",
        color: "danger",
        data: analysis.fall,
      },
      {
        key: "winter",
        label: "Winter",
        icon: "bi-snow2",
        color: "info",
        data: analysis.winter,
      },
    ];

    let html = '<h6 class="text-center mb-3">Seasonal Landscape Preview</h6>';
    html += '<div class="row row-cols-1 row-cols-md-2 g-3">';

    seasons.forEach(function (season) {
      if (!season.data) return;

      let tipsHtml = "";
      if (season.data.careTips && season.data.careTips.length > 0) {
        tipsHtml = '<ul class="list-unstyled mt-2 mb-0 small">';
        season.data.careTips.forEach(function (tip) {
          tipsHtml += `<li><i class="bi bi-check-circle text-${
            season.color
          }"></i> ${escapeHtml(tip)}</li>`;
        });
        tipsHtml += "</ul>";
      }

      let imageHtml = "";
      if (season.data.imageBase64) {
        imageHtml = `<img src="data:image/png;base64,${season.data.imageBase64}"
                          class="card-img-top seasonal-image" alt="${season.label} landscape">`;
      }

      html += `
                <div class="col">
                    <div class="card h-100 border-${season.color}">
                        <div class="card-header bg-${season.color} ${
        season.color === "warning" ? "text-dark" : "text-white"
      }">
                            <h6 class="mb-0"><i class="bi ${
                              season.icon
                            }"></i> ${season.label}</h6>
                        </div>
                        ${imageHtml}
                        <div class="card-body">
                            <p class="card-text small">${escapeHtml(
                              season.data.description
                            )}</p>
                            ${tipsHtml}
                        </div>
                    </div>
                </div>
            `;
    });

    html += "</div>";
    container.innerHTML = html;
  }

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
        initializeAddToPlantButtons();
        loadRecommendationImages();
      } else {
        throw new Error("Failed to load recommendations");
      }
    } catch (error) {
      console.error("Error loading recommendations:", error);
      container.innerHTML =
        '<p class="text-danger">Failed to load recommendations.</p>';
    }
  }

  function initializePlantSearch() {
    document.addEventListener("click", function (e) {
      const plantResult = e.target.closest(".plant-result");
      if (plantResult) {
        e.preventDefault();
        selectPlant(plantResult);
      }
    });
  }

  function selectPlant(element) {
    document.querySelectorAll(".plant-result").forEach(function (el) {
      el.classList.remove("selected");
    });
    document.querySelectorAll(".recommendation-card").forEach(function (el) {
      el.classList.remove("selected");
    });

    element.classList.add("selected");

    selectedPlant = {
      usdaSymbol: element.dataset.usdaSymbol,
      scientificName: element.dataset.scientificName,
      commonName: element.dataset.commonName,
    };

    showNotification(
      `${
        selectedPlant.commonName || selectedPlant.scientificName
      } selected. Click on the image to place it.`,
      "info"
    );
  }

  function initializeAddToPlantButtons() {
    document.querySelectorAll(".add-to-plan-btn").forEach(function (button) {
      button.addEventListener("click", function () {
        const card = this.closest(".recommendation-card");

        document
          .querySelectorAll(".recommendation-card")
          .forEach(function (el) {
            el.classList.remove("selected");
          });
        document.querySelectorAll(".plant-result").forEach(function (el) {
          el.classList.remove("selected");
        });

        card.classList.add("selected");

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
   * Fetches plant images from Wikipedia for each recommendation card
   */
  function loadRecommendationImages() {
    document.querySelectorAll(".recommendation-card").forEach(function (card) {
      const symbol = card.dataset.usdaSymbol;
      const wrapper = card.querySelector(".plant-image-wrapper");
      if (!wrapper || !symbol) return;

      fetchPlantImage(symbol, wrapper);
    });
  }

  function fetchPlantImage(symbol, wrapper) {
    fetch(`/landscape/plants/image?symbol=${encodeURIComponent(symbol)}`)
      .then(function (response) {
        if (!response.ok) return null;
        return response.json();
      })
      .then(function (data) {
        if (!data || !data.imageUrl) return;

        const img = document.createElement("img");
        img.src = data.imageUrl;
        img.alt = symbol;
        img.loading = "lazy";
        img.onload = function () {
          wrapper.innerHTML = "";
          wrapper.appendChild(img);
        };
      })
      .catch(function () {
        // Keep placeholder on error
      });
  }

  function initializeLoadPlanLinks() {
    document.querySelectorAll(".load-plan-link").forEach(function (link) {
      link.addEventListener("click", async function (e) {
        e.preventDefault();
        const planId = this.dataset.planId;
        await loadPlan(planId);
      });
    });
  }

  async function loadPlan(planId) {
    try {
      const response = await fetch(`/landscape/plans/${planId}`);
      if (response.ok) {
        const plan = await response.json();
        currentPlanId = plan.id;
        currentZone = plan.hardinessZone;

        displayPlan(plan);
        loadRecommendations(plan.id);

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

  function showNotification(message, type = "info") {
    const alertClass = `alert-${type}`;
    const alertEl = document.createElement("div");
    alertEl.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertEl.style.zIndex = "9999";
    alertEl.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
    document.body.appendChild(alertEl);

    setTimeout(function () {
      alertEl.remove();
    }, 5000);
  }

  window.LandscapePlanner = {
    loadPlan,
    showNotification,
  };
})();
