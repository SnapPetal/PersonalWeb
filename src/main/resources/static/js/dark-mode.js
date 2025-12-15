document.addEventListener("DOMContentLoaded", () => {
  const lightSwitch = document.getElementById("lightSwitch");

  // Check for saved dark mode preference and set initial state
  const isDarkMode = localStorage.getItem("darkMode") === "enabled";
  if (isDarkMode) {
    document.documentElement.setAttribute("data-bs-theme", "dark");
    lightSwitch.classList.add("dark");
  }

  // Listen for click on the light switch
  lightSwitch.addEventListener("click", () => {
    // Add pulling animation
    lightSwitch.classList.add("pulling");

    // Toggle dark mode after brief delay for animation
    setTimeout(() => {
      const isCurrentlyDark = lightSwitch.classList.contains("dark");

      if (isCurrentlyDark) {
        // Turn light on (disable dark mode)
        lightSwitch.classList.remove("dark");
        document.documentElement.setAttribute("data-bs-theme", "light");
        localStorage.setItem("darkMode", "disabled");
      } else {
        // Turn light off (enable dark mode)
        lightSwitch.classList.add("dark");
        document.documentElement.setAttribute("data-bs-theme", "dark");
        localStorage.setItem("darkMode", "enabled");
      }

      // Remove pulling animation class
      lightSwitch.classList.remove("pulling");
    }, 200);
  });

  // Keyboard accessibility - toggle on Enter or Space
  lightSwitch.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      lightSwitch.click();
    }
  });
});
