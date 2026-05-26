function confirmSubmit(formClassName, message) {
    const formElements = document.getElementsByClassName(formClassName);
    Array.from(formElements).forEach(function(element) {
        element.addEventListener("submit", function(event) {
            if (!confirm(message)) {
                event.preventDefault();
            }
        });
    });
}

confirmSubmit("delete-form", "정말로 삭제하시겠습니까?");
confirmSubmit("recommend-form", "정말로 추천하시겠습니까?");

const answerPageElements = document.getElementsByClassName("answer-page-link");
Array.from(answerPageElements).forEach(function(element) {
    element.addEventListener("click", function() {
        document.getElementById("answerPage").value = this.dataset.page;
        document.getElementById("answerSearchForm").submit();
    });
});

const answerSort = document.getElementById("answer_sort");
if (answerSort) {
    answerSort.addEventListener("change", function() {
        document.getElementById("answerSort").value = this.value;
        document.getElementById("answerPage").value = 1;
        document.getElementById("answerSearchForm").submit();
    });
}
