package com.mysite.sbb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilTest {

	private final CommonUtil commonUtil = new CommonUtil();

	@Test
	void markdownEscapesRawHtml() {
		String html = commonUtil.markdown("<script>alert('xss')</script>\n\n**굵게**");

		assertThat(html).doesNotContain("<script>");
		assertThat(html).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
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
		assertThat(html).contains("href=\"mailto:user&#64;example.com\"");
	}

	@Test
	void markdownKeepsSafeImages() {
		String html = commonUtil.markdown("![image alt](https://example.com/image.png)");

		assertThat(html).contains("<img");
		assertThat(html).contains("src=\"https://example.com/image.png\"");
		assertThat(html).contains("alt=\"image alt\"");
	}

	@Test
	void markdownSupportsGfmTables() {
		String html = commonUtil.markdown("""
				| 이름 | 값 |
				| --- | --- |
				| 표 | 동작 |
				""");

		assertThat(html).contains("<table>");
		assertThat(html).contains("<th>이름</th>");
		assertThat(html).contains("<td>동작</td>");
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
		assertThat(html).contains("href=\"\" rel=\"nofollow\">script</a>");
		assertThat(html).contains("<img src=\"\"");
	}

	@Test
	void markdownSanitizesRenderedHtmlBeforeOutput() {
		String html = commonUtil.markdown("""
				<script>alert('xss')</script>
				<img src=x onerror=alert('xss')>
				[click](javascript:alert('xss'))
				""");

		assertThat(html).doesNotContain("<script>");
		assertThat(html).doesNotContain("<img src=x onerror=");
		assertThat(html).doesNotContain("onerror=");
		assertThat(html).doesNotContain("javascript:");
		assertThat(html).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
		assertThat(html).contains("&lt;img src&#61;x onerror&#61;alert(&#39;xss&#39;)&gt;");
		assertThat(html).contains("href=\"\" rel=\"nofollow\">click</a>");
	}

	@Test
	void markdownHandlesNullAsEmptyText() {
		assertThat(commonUtil.markdown(null)).isEmpty();
	}
}
