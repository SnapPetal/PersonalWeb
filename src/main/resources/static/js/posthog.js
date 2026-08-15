(function () {
  const config = window.personalPostHogConfig;
  if (!config || !config.projectToken) {
    return;
  }

  !(function (t, e) {
    let o, n, p, r;
    e.__SV ||
      ((window.posthog = e),
      (e._i = []),
      (e.init = function (i, s, a) {
        function g(t, e) {
          const o = e.split(".");
          2 === o.length && ((t = t[o[0]]), (e = o[1]));
          t[e] = function () {
            t.push([e].concat(Array.prototype.slice.call(arguments, 0)));
          };
        }
        (p = t.createElement("script")).type = "text/javascript";
        p.crossOrigin = "anonymous";
        p.async = !0;
        p.src =
          s.api_host.replace(".i.posthog.com", "-assets.i.posthog.com") +
          "/static/array.js";
        (r = t.getElementsByTagName("script")[0]).parentNode.insertBefore(p, r);
        const u = e;
        for (
          void 0 !== a ? (u[a] = []) : (a = "posthog"),
            u.people = u.people || [],
            u.toString = function (t) {
              let e = "posthog";
              return (
                "posthog" !== a && (e += "." + a), t || (e += " (stub)"), e
              );
            },
            u.people.toString = function () {
              return u.toString(1) + ".people (stub)";
            },
            o =
              "capture identify reset isFeatureEnabled getFeatureFlag onFeatureFlags opt_in_capturing opt_out_capturing".split(
                " "
              ),
            n = 0;
          n < o.length;
          n++
        )
          g(u, o[n]);
        e._i.push([i, s, a]);
      }),
      (e.__SV = 1));
  })(document, window.posthog || []);

  posthog.init(config.projectToken, {
    api_host: config.apiHost,
    defaults: "2026-05-30",
    capture_pageview: true,
    capture_pageleave: true,
    autocapture: true,
    session_recording: { maskAllInputs: true },
  });

  window.personalPostHog = {
    capture: (...args) => posthog.capture(...args),
    isFeatureEnabled: (flagKey) => posthog.isFeatureEnabled(flagKey),
    getFeatureFlag: (flagKey) => posthog.getFeatureFlag(flagKey),
    onFeatureFlags: (callback) => posthog.onFeatureFlags(callback),
    identify: (...args) => posthog.identify(...args),
    reset: () => posthog.reset(),
  };

  fetch("/auth/me", { credentials: "same-origin" })
    .then((response) => (response.ok ? response.text() : null))
    .then((userId) => {
      if (userId) {
        posthog.identify(userId);
      }
    })
    .catch(() => undefined);
})();
