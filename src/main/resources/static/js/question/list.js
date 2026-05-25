const pageElements = document.getElementsByClassName("page-link");
Array.from(pageElements).forEach(function(element) {
    element.addEventListener("click", function() {
        document.getElementById("page").value = this.dataset.page;
        document.getElementById("searchForm").submit();
    });
});

const searchButton = document.getElementById("btn_search");
if (searchButton) {
    searchButton.addEventListener("click", function() {
        document.getElementById("kw").value = document.getElementById("search_kw").value;
        document.getElementById("page").value = 1;
        document.getElementById("searchForm").submit();
    });
}
