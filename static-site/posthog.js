(() => {
  const projectToken = window.PERSONAL_POSTHOG_PROJECT_TOKEN;
  if (!projectToken) return;

  const posthog = (window.posthog = window.posthog || []);
  posthog._i = posthog._i || [];
  posthog.init = (token, config) => posthog._i.push([token, config]);
  posthog.capture = (...args) => posthog.push(["capture", ...args]);

  const script = document.createElement("script");
  script.async = true;
  script.crossOrigin = "anonymous";
  script.src = "https://us-assets.i.posthog.com/static/array.js";
  script.onload = () => {
    if (!window.posthog || typeof window.posthog.init !== "function") return;

    window.posthog.init(projectToken, {
      api_host: "https://us.i.posthog.com",
      defaults: "2026-05-30",
      capture_pageview: true,
      capture_pageleave: true,
      autocapture: true,
      session_recording: { maskAllInputs: true },
    });

    const capture = (event, properties = {}) => window.posthog.capture(event, properties);

    document.addEventListener("click", (event) => {
      const link = event.target.closest("a");
      if (!link || !link.href) return;

      capture("static_site_link_clicked", {
        href: link.href,
        text: link.textContent.trim().slice(0, 120),
        external: link.origin !== window.location.origin,
        section: link.closest("section")?.id || "navigation",
      });
    });

    document.addEventListener("click", (event) => {
      if (event.target.closest("[data-theme-toggle]")) {
        capture("static_site_theme_toggled");
      }
      if (event.target.closest("[data-joke-toggle]")) {
        capture("static_site_joke_toggled");
      }
    });

    const milestones = [25, 50, 75, 90];
    const reported = new Set();
    const reportScrollDepth = () => {
      const scrollable = document.documentElement.scrollHeight - window.innerHeight;
      if (scrollable <= 0) return;

      const depth = Math.round((window.scrollY / scrollable) * 100);
      milestones.forEach((milestone) => {
        if (depth >= milestone && !reported.has(milestone)) {
          reported.add(milestone);
          capture("static_site_scroll_depth", { percent: milestone });
        }
      });
    };

    window.addEventListener("scroll", reportScrollDepth, { passive: true });
  };
  document.head.appendChild(script);
})();
