(() => {
  const root = document.documentElement;
  const saved = localStorage.getItem("darkMode");
  const preferred = saved === "enabled" || (saved === null && matchMedia("(prefers-color-scheme: dark)").matches);
  root.dataset.theme = preferred ? "dark" : "light";
  addEventListener("DOMContentLoaded", () => {
    const button = document.querySelector("[data-theme-toggle]");
    if (!button) return;
    const update = () => { button.textContent = root.dataset.theme === "dark" ? "☀" : "☾"; };
    button.addEventListener("click", () => {
      const dark = root.dataset.theme !== "dark";
      root.dataset.theme = dark ? "dark" : "light";
      localStorage.setItem("darkMode", dark ? "enabled" : "disabled");
      update();
    });
    update();
  });
})();
