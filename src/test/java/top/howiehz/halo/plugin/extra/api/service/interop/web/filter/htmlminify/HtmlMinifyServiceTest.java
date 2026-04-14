package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import in.wilsonl.minifyhtml.Configuration;
import org.junit.jupiter.api.Test;

class HtmlMinifyServiceTest {
    private final HtmlMinifyService service = new HtmlMinifyService();

    @Test
    void shouldUseJavaTypeDefaultsWithoutSchemaValues() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();

        assertFalse(config.isAllowNoncompliantUnquotedAttributeValues());
        assertFalse(config.isAllowOptimalEntities());
        assertFalse(config.isAllowRemovingSpacesBetweenAttributes());
        assertFalse(config.isMinifyDoctype());
        assertFalse(config.isKeepClosingTags());
        assertFalse(config.isKeepComments());
        assertFalse(config.isKeepHtmlAndHeadOpeningTags());
        assertFalse(config.isKeepInputTypeTextAttr());
        assertFalse(config.isKeepSsiComments());
        assertFalse(config.isMinifyCss());
        assertFalse(config.isMinifyJs());
        assertFalse(config.isRemoveBangs());
        assertFalse(config.isRemoveProcessingInstructions());
        assertFalse(config.isPreserveBraceTemplateSyntax());
        assertFalse(config.isPreserveChevronPercentTemplateSyntax());
        assertNull(config.getExcludePaths());
    }

    @Test
    void shouldMapConfigToNativeConfiguration() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();
        config.setAllowNoncompliantUnquotedAttributeValues(true);
        config.setAllowOptimalEntities(true);
        config.setAllowRemovingSpacesBetweenAttributes(true);
        config.setKeepClosingTags(true);
        config.setKeepComments(true);
        config.setKeepHtmlAndHeadOpeningTags(true);
        config.setKeepInputTypeTextAttr(true);
        config.setKeepSsiComments(true);
        config.setMinifyCss(false);
        config.setMinifyDoctype(true);
        config.setMinifyJs(false);
        config.setPreserveBraceTemplateSyntax(false);
        config.setPreserveChevronPercentTemplateSyntax(false);
        config.setRemoveBangs(false);
        config.setRemoveProcessingInstructions(true);

        Configuration nativeConfig = service.toNativeConfiguration(config);

        assertTrue(nativeConfig.allow_noncompliant_unquoted_attribute_values);
        assertTrue(nativeConfig.allow_optimal_entities);
        assertTrue(nativeConfig.allow_removing_spaces_between_attributes);
        assertTrue(nativeConfig.keep_closing_tags);
        assertTrue(nativeConfig.keep_comments);
        assertTrue(nativeConfig.keep_html_and_head_opening_tags);
        assertTrue(nativeConfig.keep_input_type_text_attr);
        assertTrue(nativeConfig.keep_ssi_comments);
        assertFalse(nativeConfig.minify_css);
        assertTrue(nativeConfig.minify_doctype);
        assertFalse(nativeConfig.minify_js);
        assertFalse(nativeConfig.preserve_brace_template_syntax);
        assertFalse(nativeConfig.preserve_chevron_percent_template_syntax);
        assertFalse(nativeConfig.remove_bangs);
        assertTrue(nativeConfig.remove_processing_instructions);
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
