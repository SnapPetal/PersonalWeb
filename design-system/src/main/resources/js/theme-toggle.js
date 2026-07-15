const readThemePreference = () => {
  const cookie = document.cookie
    .split("; ")
    .find((value) => value.startsWith("PERSONALWEB_THEME="));
  return cookie ? cookie.split("=")[1] : null;
};

const saveThemePreference = (value) => {
  document.cookie = `PERSONALWEB_THEME=${value}; Max-Age=31536000; Path=/; Domain=.thonbecker.biz; SameSite=Lax`;
};

const savedDarkMode = readThemePreference() === "enabled";

if (savedDarkMode) {
  document.documentElement.setAttribute("data-bs-theme", "dark");
}

document.addEventListener("alpine:init", () => {
  Alpine.data("themeToggle", () => ({
    isDark: savedDarkMode,

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
      saveThemePreference(this.isDark ? "enabled" : "disabled");
    },

    get iconClass() {
      return this.isDark ? "bi bi-moon-stars-fill" : "bi bi-sun-fill";
    },
  }));
});
