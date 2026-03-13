document.addEventListener("DOMContentLoaded", () => {
  const themeToggle = document.getElementById("themeToggle");
  const themeIcon = document.getElementById("themeIcon");

  // Check for saved dark mode preference and set initial state
  const isDarkMode = localStorage.getItem("darkMode") === "enabled";
  if (isDarkMode) {
    document.documentElement.setAttribute("data-bs-theme", "dark");
    themeIcon.classList.remove("bi-sun-fill");
    themeIcon.classList.add("bi-moon-stars-fill");
  }

  // Listen for click on the theme toggle
  themeToggle.addEventListener("click", () => {
    const isCurrentlyDark = document.documentElement.getAttribute("data-bs-theme") === "dark";

    if (isCurrentlyDark) {
      // Switch to light mode
      document.documentElement.setAttribute("data-bs-theme", "light");
      localStorage.setItem("darkMode", "disabled");
      themeIcon.classList.remove("bi-moon-stars-fill");
      themeIcon.classList.add("bi-sun-fill");
    } else {
      // Switch to dark mode
      document.documentElement.setAttribute("data-bs-theme", "dark");
      localStorage.setItem("darkMode", "enabled");
      themeIcon.classList.remove("bi-sun-fill");
      themeIcon.classList.add("bi-moon-stars-fill");
    }
  });

  // Keyboard accessibility - toggle on Enter or Space
  themeToggle.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      themeToggle.click();
    }
  });
});
