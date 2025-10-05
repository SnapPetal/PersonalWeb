/**
 * A simple, lightweight library for making tables sortable.
 *
 * To use, add the `sortable-table` class to your table and the `sortable` class to the `th` elements
 * of the columns you want to be sortable.
 */
document.addEventListener("DOMContentLoaded", () => {
  const getCellValue = (tr, idx) =>
    tr.children[idx].innerText || tr.children[idx].textContent;

  const comparer = (idx, asc) => (a, b) =>
    ((v1, v2) =>
      v1 !== "" && v2 !== "" && !isNaN(v1) && !isNaN(v2)
        ? v1 - v2
        : v1.toString().localeCompare(v2))(
      getCellValue(asc ? a : b, idx),
      getCellValue(asc ? b : a, idx)
    );

  document.querySelectorAll(".sortable-table .sortable").forEach((th) =>
    th.addEventListener("click", () => {
      const table = th.closest("table");
      const tbody = table.querySelector("tbody");
      Array.from(tbody.querySelectorAll("tr"))
        .sort(
          comparer(
            Array.from(th.parentNode.children).indexOf(th),
            (this.asc = !this.asc)
          )
        )
        .forEach((tr) => tbody.appendChild(tr));

      // Update sort indicators
      table.querySelectorAll(".sortable").forEach((header) => {
        header.classList.remove("sort-asc", "sort-desc");
      });

      th.classList.toggle("sort-asc", this.asc);
      th.classList.toggle("sort-desc", !this.asc);
    })
  );
});
