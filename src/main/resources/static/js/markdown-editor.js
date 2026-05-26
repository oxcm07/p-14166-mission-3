document.addEventListener("DOMContentLoaded", function() {
    if (!window.toastui || !window.toastui.Editor) {
        return;
    }

    const textareaElements = document.querySelectorAll("textarea.markdown-editor:not([disabled])");
    textareaElements.forEach(function(textarea) {
        const editorElement = document.createElement("div");
        textarea.insertAdjacentElement("afterend", editorElement);
        textarea.style.display = "none";

        const editor = new toastui.Editor({
            el: editorElement,
            height: textarea.dataset.editorHeight || "400px",
            initialEditType: "markdown",
            previewStyle: "vertical",
            initialValue: textarea.value || "",
            usageStatistics: false
        });

        textarea.form.addEventListener("submit", function() {
            textarea.value = editor.getMarkdown();
        });
    });
});
