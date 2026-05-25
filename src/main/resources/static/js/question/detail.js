const deleteElements = document.getElementsByClassName("delete");
Array.from(deleteElements).forEach(function(element) {
    element.addEventListener("click", function() {
        if (confirm("정말로 삭제하시겠습니까?")) {
            location.href = this.dataset.uri;
        }
    });
});

const recommendElements = document.getElementsByClassName("recommend");
Array.from(recommendElements).forEach(function(element) {
    element.addEventListener("click", function() {
        if (confirm("정말로 추천하시겠습니까?")) {
            location.href = this.dataset.uri;
        }
    });
});

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
