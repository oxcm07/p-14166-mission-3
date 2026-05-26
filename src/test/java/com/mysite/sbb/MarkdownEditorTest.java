package com.mysite.sbb;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.user.SiteUser;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MarkdownEditorTest extends AbstractSbbIntegrationTest {

	@Test
	void markdownEditorScriptIsServedFromStaticResources() throws Exception {
		mockMvc.perform(get("/js/markdown-editor.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("toastui.Editor")))
				.andExpect(content().string(containsString("textarea.markdown-editor:not([disabled])")))
				.andExpect(content().string(containsString("editor.getMarkdown()")))
				.andExpect(content().string(containsString("data-bs-theme")))
				.andExpect(content().string(containsString("toastui-editor-dark")))
				.andExpect(content().string(containsString("sbb:themechange")));
	}

	@Test
	void customStyleSupportsDarkMarkdownContentText() throws Exception {
		mockMvc.perform(get("/css/style.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("[data-bs-theme=\"dark\"] .markdown-content")))
				.andExpect(content().string(containsString("color: var(--bs-body-color)")))
				.andExpect(content().string(containsString(".toastui-editor-contents *")))
				.andExpect(content().string(containsString(".ProseMirror")))
				.andExpect(content().string(containsString(".toastui-editor-md-heading")))
				.andExpect(content().string(containsString(".toastui-editor-mode-switch")))
				.andExpect(content().string(containsString("filter: invert(1) brightness(1.85) contrast(0.9)")))
				.andExpect(content().string(containsString(".toastui-editor-popup-add-heading")))
				.andExpect(content().string(containsString(".toastui-editor-popup-add-image")))
				.andExpect(content().string(containsString(".toastui-editor-popup-add-link")))
				.andExpect(content().string(containsString(".toastui-editor-md-preview-highlight")))
				.andExpect(content().string(containsString("background-color: transparent !important")))
				.andExpect(content().string(containsString("box-shadow: none")))
				.andExpect(content().string(containsString("width: 1px")))
				.andExpect(content().string(containsString(".toastui-editor-md-preview-highlight::after")))
				.andExpect(content().string(containsString("content: none !important")))
				.andExpect(content().string(containsString("display: none !important")))
				.andExpect(content().string(containsString(".toastui-editor-md-vertical-style .toastui-editor-md-splitter")))
				.andExpect(content().string(containsString("background-color: #6c757d")))
				.andExpect(content().string(containsString("background-color: #f8f9fa")))
				.andExpect(content().string(containsString(".ProseMirror table")));
	}

	@Test
	void toastUiEditorVendorFilesAreServedFromStaticResources() throws Exception {
		mockMvc.perform(get("/vendor/toastui-editor/3.2.2/toastui-editor.min.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("toastui-editor")));

		mockMvc.perform(get("/vendor/toastui-editor/3.2.2/toastui-editor-all.min.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("toastui")));
	}

	@Test
	void pagesWithoutMarkdownEditingDoNotLoadEditorResources() throws Exception {
		mockMvc.perform(get("/question/qna/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("toastui-editor"))))
				.andExpect(content().string(not(containsString("/js/markdown-editor.js"))));
	}

	@Test
	void questionFormLoadsToastUiEditorForQuestionContent() throws Exception {
		createUser();

		mockMvc.perform(get("/question/create")
				.param("categoryCode", "qna")
				.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("question/form"))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor.min.css")))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor-all.min.js")))
				.andExpect(content().string(containsString("/js/markdown-editor.js")))
				.andExpect(content().string(not(containsString("uicdn.toast.com"))))
				.andExpect(content().string(containsString("class=\"form-control markdown-editor\"")))
				.andExpect(content().string(containsString("data-editor-height=\"420px\"")));
	}

	@Test
	void answerModifyFormLoadsToastUiEditorForAnswerContent() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("에디터 답변 수정 질문", "질문 내용", category("qna"), author, 0);
		Answer answer = answerService.create(question, "수정할 답변 내용", author);

		mockMvc.perform(get("/answer/modify/{id}", answer.getId())
				.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("answer/form"))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor.min.css")))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor-all.min.js")))
				.andExpect(content().string(containsString("/js/markdown-editor.js")))
				.andExpect(content().string(not(containsString("uicdn.toast.com"))))
				.andExpect(content().string(containsString("class=\"form-control markdown-editor\"")))
				.andExpect(content().string(containsString("수정할 답변 내용")));
	}

	@Test
	void questionDetailLoadsEditorForAuthenticatedAnswerForm() throws Exception {
		SiteUser author = createUser();
		createQuestion("에디터 상세 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(get("/question/qna/detail/1")
				.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("question/detail"))
				.andExpect(content().string(containsString("card-text markdown-content")))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor.min.css")))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor-all.min.js")))
				.andExpect(content().string(containsString("/js/markdown-editor.js")))
				.andExpect(content().string(not(containsString("uicdn.toast.com"))))
				.andExpect(content().string(containsString("class=\"form-control markdown-editor\"")))
				.andExpect(content().string(containsString("data-editor-height=\"360px\"")));
	}

	@Test
	void disabledAnonymousAnswerTextareaIsNotMarkedAsMarkdownEditor() throws Exception {
		SiteUser author = createUser();
		createQuestion("비로그인 에디터 제외 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(get("/question/qna/detail/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("question/detail"))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor.min.css")))
				.andExpect(content().string(containsString("/vendor/toastui-editor/3.2.2/toastui-editor-all.min.js")))
				.andExpect(content().string(containsString("/js/markdown-editor.js")))
				.andExpect(content().string(not(containsString("uicdn.toast.com"))))
				.andExpect(content().string(containsString("disabled class=\"form-control\"")))
				.andExpect(content().string(not(containsString("disabled class=\"form-control markdown-editor\""))));
	}
}
