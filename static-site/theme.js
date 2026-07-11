(() => {
  const root = document.documentElement;
  const cookie = document.cookie.split("; ").find((value) => value.startsWith("PERSONALWEB_THEME="));
  const saved = cookie ? cookie.split("=")[1] : localStorage.getItem("darkMode");
  const preferred = saved === "enabled" || (saved === null && matchMedia("(prefers-color-scheme: dark)").matches);
  root.dataset.theme = preferred ? "dark" : "light";
  addEventListener("DOMContentLoaded", () => {
    document.addEventListener("click", (event) => {
      const toggle = event.target.closest(".verse-toggle");
      if (!toggle) return;
      const text = document.querySelector(".verse-text");
      const greek = text.textContent === toggle.dataset.english;
      text.textContent = greek ? toggle.dataset.greek : toggle.dataset.english;
      toggle.textContent = greek ? "Show English" : "Show Greek";
    });
    const button = document.querySelector("[data-theme-toggle]");
    if (!button) return;
    const update = () => { button.textContent = root.dataset.theme === "dark" ? "☀" : "☾"; };
    button.addEventListener("click", () => {
      const dark = root.dataset.theme !== "dark";
      root.dataset.theme = dark ? "dark" : "light";
      localStorage.setItem("darkMode", dark ? "enabled" : "disabled");
      document.cookie = `PERSONALWEB_THEME=${dark ? "enabled" : "disabled"}; Max-Age=31536000; Path=/; Domain=.thonbecker.biz; SameSite=Lax`;
      update();
    });
    update();
  });
})();
