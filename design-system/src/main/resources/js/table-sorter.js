document.addEventListener("DOMContentLoaded", () => {
  const getCellValue = (row, index) =>
    row.children[index].innerText || row.children[index].textContent;

  const comparer = (index, ascending) => (first, second) =>
    ((firstValue, secondValue) =>
      firstValue !== "" &&
      secondValue !== "" &&
      !Number.isNaN(Number(firstValue)) &&
      !Number.isNaN(Number(secondValue))
        ? firstValue - secondValue
        : firstValue.toString().localeCompare(secondValue))(
      getCellValue(ascending ? first : second, index),
      getCellValue(ascending ? second : first, index)
    );

  document.querySelectorAll(".sortable-table .sortable").forEach((header) => {
    header.addEventListener("click", () => {
      const table = header.closest("table");
      const tbody = table.querySelector("tbody");
      const ascending = header.dataset.sortDirection !== "asc";
      header.dataset.sortDirection = ascending ? "asc" : "desc";

      Array.from(tbody.querySelectorAll("tr"))
        .sort(
          comparer(
            Array.from(header.parentNode.children).indexOf(header),
            ascending
          )
        )
        .forEach((row) => tbody.appendChild(row));

      table.querySelectorAll(".sortable").forEach((item) => {
        item.classList.remove("sort-asc", "sort-desc");
      });

      header.classList.toggle("sort-asc", ascending);
      header.classList.toggle("sort-desc", !ascending);
    });
  });
});
