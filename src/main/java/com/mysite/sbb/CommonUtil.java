package com.mysite.sbb;

import org.commonmark.node.Node;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonUtil {
    private final List<Extension> extensions = List.of(TablesExtension.create());
    private final Parser parser = Parser.builder()
            .extensions(extensions)
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto")))
            .build();
    private final PolicyFactory sanitizer = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.TABLES);

    // 마크다운 텍스트를 HTML로 변환하는 메서드
    public String markdown(String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        return sanitizer.sanitize(renderer.render(document));
    }
}
