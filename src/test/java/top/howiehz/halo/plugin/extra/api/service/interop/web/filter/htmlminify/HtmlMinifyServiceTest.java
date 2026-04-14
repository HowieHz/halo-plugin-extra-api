package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlMinifyServiceTest {
    private final HtmlMinifyService service = new HtmlMinifyService();

    @Test
    void shouldReturnBlankInputAsIs() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();

        assertNull(service.minify(null, config));
        assertEquals("", service.minify("", config));
        assertEquals("   ", service.minify("   ", config));
    }

    @Test
    void shouldMinifyHtmlUsingExplicitConfiguration() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();
        config.setKeepComments(true);
        config.setMinifyCss(true);
        config.setMinifyJs(true);
        config.setPreserveBraceTemplateSyntax(true);

        String minified = service.minify(
            "<html><body><div>  Hello  </div><!-- comment --><style>body { color: red; }</style>"
                + "<script>function add(a, b) { return a + b; }</script>{{ user.name }}</body>"
                + "</html>",
            config
        );

        assertTrue(minified.contains("<!-- comment -->"));
        assertTrue(minified.contains("<div>Hello</div>"));
        assertTrue(minified.contains("body{color:red}"));
        assertTrue(minified.contains("function add(a,b){return a+b}"));
        assertTrue(minified.contains("{{ user.name }}"));
    }

    @Test
    void shouldRemoveCommentsWhenConfigured() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();
        config.setKeepComments(false);

        String minified = service.minify(
            "<html><body><div>  Hello  </div><!-- comment --></body></html>",
            config
        );

        assertFalse(minified.contains("<!-- comment -->"));
        assertTrue(minified.contains("<div>Hello</div>"));
    }
}
