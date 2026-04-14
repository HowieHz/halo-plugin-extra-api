package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import in.wilsonl.minifyhtml.Configuration;
import in.wilsonl.minifyhtml.MinifyHtml;
import org.springframework.stereotype.Service;

/**
 * Service for minifying HTML responses with minify-html JNI bindings.
 * 使用 minify-html JNI 绑定压缩 HTML 响应的服务。
 */
@Service
public class HtmlMinifyService {

    /**
     * Minify HTML with the configured options.
     * 使用指定配置压缩 HTML。
     *
     * @param html HTML source / HTML 源码
     * @param config minify configuration / 压缩配置
     * @return minified HTML / 压缩后的 HTML
     */
    public String minify(String html, HtmlMinifyConfig config) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return MinifyHtml.minify(html, buildNativeConfiguration(config));
    }

    private Configuration buildNativeConfiguration(HtmlMinifyConfig config) {
        return new Configuration.Builder()
            .setAllowNoncompliantUnquotedAttributeValues(
                config.isAllowNoncompliantUnquotedAttributeValues())
            .setAllowOptimalEntities(config.isAllowOptimalEntities())
            .setAllowRemovingSpacesBetweenAttributes(
                config.isAllowRemovingSpacesBetweenAttributes())
            .setKeepClosingTags(config.isKeepClosingTags())
            .setKeepComments(config.isKeepComments())
            .setKeepHtmlAndHeadOpeningTags(config.isKeepHtmlAndHeadOpeningTags())
            .setKeepInputTypeTextAttr(config.isKeepInputTypeTextAttr())
            .setKeepSsiComments(config.isKeepSsiComments())
            .setMinifyCss(config.isMinifyCss())
            .setMinifyDoctype(config.isMinifyDoctype())
            .setMinifyJs(config.isMinifyJs())
            .setPreserveBraceTemplateSyntax(config.isPreserveBraceTemplateSyntax())
            .setPreserveChevronPercentTemplateSyntax(
                config.isPreserveChevronPercentTemplateSyntax())
            .setRemoveBangs(config.isRemoveBangs())
            .setRemoveProcessingInstructions(config.isRemoveProcessingInstructions())
            .build();
    }
}
