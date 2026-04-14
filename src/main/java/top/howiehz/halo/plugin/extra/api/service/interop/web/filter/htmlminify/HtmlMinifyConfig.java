package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import java.util.List;
import lombok.Data;

/**
 * Configuration class for HTML response minification.
 * HTML 页面压缩配置文件类。
 */
@Data
public class HtmlMinifyConfig {

    /**
     * Whether to enable HTML response minification.
     * 是否启用 HTML 页面压缩。
     */
    private boolean enabledHtmlMinify;

    /**
     * Excluded path patterns for HTML minification.
     * HTML 页面压缩的排除路径规则。
     */
    private List<String> excludePaths;

    /**
     * Allow noncompliant unquoted attribute values.
     * 是否允许非规范的无引号属性值。
     */
    private boolean allowNoncompliantUnquotedAttributeValues;

    /**
     * Allow more aggressive entity optimizations.
     * 是否允许更激进的实体优化。
     */
    private boolean allowOptimalEntities;

    /**
     * Allow removing spaces between attributes when possible.
     * 是否允许在可能时移除属性之间的空格。
     */
    private boolean allowRemovingSpacesBetweenAttributes;

    /**
     * Keep optional closing tags.
     * 是否保留可省略的闭合标签。
     */
    private boolean keepClosingTags;

    /**
     * Keep HTML comments.
     * 是否保留 HTML 注释。
     */
    private boolean keepComments;

    /**
     * Keep opening html/head tags when possible.
     * 是否保留可省略属性时的 html/head 起始标签。
     */
    private boolean keepHtmlAndHeadOpeningTags;

    /**
     * Keep type=text on input elements.
     * 是否保留 input 元素的 type=text 属性。
     */
    private boolean keepInputTypeTextAttr;

    /**
     * Keep SSI comments.
     * 是否保留 SSI 注释。
     */
    private boolean keepSsiComments;

    /**
     * Minify inline CSS.
     * 是否压缩 style 标签和 style 属性中的 CSS。
     */
    private boolean minifyCss;

    /**
     * Minify DOCTYPE declarations.
     * 是否压缩 DOCTYPE 声明。
     */
    private boolean minifyDoctype;

    /**
     * Minify inline JavaScript.
     * 是否压缩 script 标签中的 JavaScript。
     */
    private boolean minifyJs;

    /**
     * Preserve {{ }}, {% %}, and {# #} template syntax.
     * 是否保留花括号模板语法。
     */
    private boolean preserveBraceTemplateSyntax;

    /**
     * Preserve <% %> template syntax.
     * 是否保留 <% %> 模板语法。
     */
    private boolean preserveChevronPercentTemplateSyntax;

    /**
     * Remove bangs.
     * 是否移除 bang 声明。
     */
    private boolean removeBangs;

    /**
     * Remove processing instructions.
     * 是否移除处理指令。
     */
    private boolean removeProcessingInstructions;
}
