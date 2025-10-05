document.addEventListener('DOMContentLoaded', () => {
  const darkModeToggle = document.getElementById('darkModeToggle');

  // Check for saved dark mode preference
  if (localStorage.getItem('darkMode') === 'enabled') {
    document.documentElement.setAttribute('data-bs-theme', 'dark');
    darkModeToggle.checked = true;
  }

  // Listen for toggle changes
  darkModeToggle.addEventListener('change', () => {
    if (darkModeToggle.checked) {
      document.documentElement.setAttribute('data-bs-theme', 'dark');
      localStorage.setItem('darkMode', 'enabled');
    } else {
      document.documentElement.setAttribute('data-bs-theme', 'light');
      localStorage.setItem('darkMode', 'disabled');
    }
  });
});
