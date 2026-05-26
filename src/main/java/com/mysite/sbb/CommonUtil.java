package com.mysite.sbb;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonUtil {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto")))
            .build();

    // 마크다운 텍스트를 HTML로 변환하는 메서드
    public String markdown(String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        return renderer.render(document);
    }
}
