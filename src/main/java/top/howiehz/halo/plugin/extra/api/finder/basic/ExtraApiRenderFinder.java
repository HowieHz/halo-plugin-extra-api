package top.howiehz.halo.plugin.extra.api.finder.basic;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Finder for render operations (Pangu spacing).
 * 渲染操作的 Finder（Pangu 空格）。
 *
 * <p>This finder provides template-accessible methods for applying Pangu spacing
 * to text content, improving readability by inserting whitespace between CJK
 * and Latin characters.</p>
 * <p>此 Finder 提供模板可访问的方法，用于对文本内容应用 Pangu 空格，
 * 通过在中日韩字符和拉丁字符之间插入空格来提高可读性。</p>
 *
 * @author HowieXie
 * @since 1.0.0
 */
public interface ExtraApiRenderFinder {

    /**
     * Apply Pangu spacing to entire HTML content.
     * 对整个 HTML 内容应用 Pangu 空格。
     *
     * <p>This method processes the entire HTML document, automatically skipping
     * certain tags like code, pre, script, style, and textarea to preserve their
     * original formatting.</p>
     * <p>此方法处理整个 HTML 文档，自动跳过某些标签（如 code、pre、script、style 和 textarea），
     * 以保留其原始格式。</p>
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @return Mono emitting processed HTML content / 发出处理后的 HTML 内容的 Mono
     */
    Mono<String> applySpacingInHtml(String htmlContent);

    /**
     * Apply Pangu spacing to HTML content with flexible parameters.
     * 使用灵活参数对 HTML 内容应用 Pangu 空格。
     *
     * <p>Supported parameters:</p>
     * <ul>
     *   <li>htmlContent (String, required): The HTML content to process</li>
     *   <li>selector (String, optional): CSS selector to target specific elements.
     *       Supports all JSoup CSS selectors including element, class, ID, attribute,
     *       pseudo-class selectors, and combinators.</li>
     * </ul>
     *
     * <p>支持的参数：</p>
     * <ul>
     *   <li>htmlContent (String, 必需)：要处理的 HTML 内容</li>
     *   <li>selector (String, 可选)：用于定位特定元素的 CSS 选择器。
     *       支持所有 JSoup CSS 选择器，包括元素、class、ID、属性、伪类选择器和组合器。</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Process entire HTML
     * applySpacingInHtml(Map.of("htmlContent", html))
     *
     * // Process only paragraphs
     * applySpacingInHtml(Map.of("htmlContent", html, "selector", "p"))
     *
     * // Process elements with specific class
     * applySpacingInHtml(Map.of("htmlContent", html, "selector", ".article-content"))
     *
     * // Process using complex selector
     * applySpacingInHtml(Map.of("htmlContent", html, "selector", "div.content > p"))
     * </pre>
     *
     * @param params map containing htmlContent (required) and optional selector
     * 包含 htmlContent（必需）和可选 selector 的映射
     * @return Mono emitting processed HTML content / 发出处理后的 HTML 内容的 Mono
     */
    Mono<String> applySpacingInHtml(Map<String, Object> params);

    /**
     * Apply Pangu spacing to plain text.
     * 对纯文本应用 Pangu 空格。
     *
     * @param text the plain text to process / 要处理的纯文本
     * @return Mono emitting processed text / 发出处理后的文本的 Mono
     */
    Mono<String> applySpacingInText(String text);
}
