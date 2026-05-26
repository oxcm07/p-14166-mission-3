document.addEventListener("DOMContentLoaded", function() {
    if (!window.toastui || !window.toastui.Editor) {
        return;
    }

    function currentTheme() {
        return document.documentElement.getAttribute("data-bs-theme") === "dark" ? "dark" : "light";
    }

    function syncEditorTheme(editorElement) {
        const editorRoot = editorElement.querySelector(".toastui-editor-defaultUI");
        if (!editorRoot) {
            return;
        }

        editorRoot.classList.toggle("toastui-editor-dark", currentTheme() === "dark");
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
            theme: currentTheme(),
            usageStatistics: false
        });

        syncEditorTheme(editorElement);
        document.addEventListener("sbb:themechange", function() {
            syncEditorTheme(editorElement);
        });

        textarea.form.addEventListener("submit", function() {
            textarea.value = editor.getMarkdown();
        });
    });
});
