// theme-toggle.js — Alpine.js component
document.addEventListener("alpine:init", () => {
  Alpine.data("themeToggle", () => ({
    isDark: localStorage.getItem("darkMode") === "enabled",

    init() {
      if (this.isDark) {
        document.documentElement.setAttribute("data-bs-theme", "dark");
      }
    },

    toggle() {
      this.isDark = !this.isDark;
      document.documentElement.setAttribute(
        "data-bs-theme",
        this.isDark ? "dark" : "light"
      );
      localStorage.setItem("darkMode", this.isDark ? "enabled" : "disabled");
    },

    get iconClass() {
      return this.isDark ? "bi bi-moon-stars-fill" : "bi bi-sun-fill";
    },
  }));
});
