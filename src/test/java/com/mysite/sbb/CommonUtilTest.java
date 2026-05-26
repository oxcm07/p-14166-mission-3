package com.mysite.sbb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilTest {

	private final CommonUtil commonUtil = new CommonUtil();

	@Test
	void markdownEscapesRawHtml() {
		String html = commonUtil.markdown("<script>alert('xss')</script>\n\n**굵게**");

		assertThat(html).doesNotContain("<script>");
		assertThat(html).contains("&lt;script&gt;alert('xss')&lt;/script&gt;");
		assertThat(html).contains("<strong>굵게</strong>");
	}

	@Test
	void markdownAllowsSafeLinkProtocols() {
		String html = commonUtil.markdown("""
				[http](http://example.com)
				[https](https://example.com)
				[mail](mailto:user@example.com)
				""");

		assertThat(html).contains("href=\"http://example.com\"");
		assertThat(html).contains("href=\"https://example.com\"");
		assertThat(html).contains("href=\"mailto:user@example.com\"");
	}

	@Test
	void markdownRemovesUnsafeLinkProtocols() {
		String html = commonUtil.markdown("""
				[script](javascript:alert('xss'))
				![image](data:text/html,<script>alert('xss')</script>)
				""");

		assertThat(html).doesNotContain("javascript:");
		assertThat(html).doesNotContain("data:text/html");
		assertThat(html).doesNotContain("<script>");
		assertThat(html).contains("href=\"\">script</a>");
		assertThat(html).contains("<img src=\"\"");
	}

	@Test
	void markdownHandlesNullAsEmptyText() {
		assertThat(commonUtil.markdown(null)).isEmpty();
	}
}
