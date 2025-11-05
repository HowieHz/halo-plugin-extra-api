package top.howiehz.halo.plugin.extra.api.finder.basic;

import reactor.core.publisher.Mono;

/**
 * Finder for Pangu spacing operations.
 * Pangu 空格操作的 Finder。
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
public interface ExtraApiPanguFinder {

    /**
     * Apply Pangu spacing to HTML content for specified tag elements.
     * 对 HTML 内容中的指定标签元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param tagName the tag name to process (e.g., "p", "div") / 要处理的标签名称（例如 "p"、"div"）
     * @return Mono emitting processed HTML content / 发出处理后的 HTML 内容的 Mono
     */
    Mono<String> spacingElementByTagName(String htmlContent, String tagName);

    /**
     * Apply Pangu spacing to HTML element with specified ID.
     * 对 HTML 内容中具有指定 ID 的元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param id the element ID to process / 要处理的元素 ID
     * @return Mono emitting processed HTML content / 发出处理后的 HTML 内容的 Mono
     */
    Mono<String> spacingElementById(String htmlContent, String id);

    /**
     * Apply Pangu spacing to HTML elements with specified class name.
     * 对 HTML 内容中具有指定 class 的元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param className the class name to process / 要处理的 class 名称
     * @return Mono emitting processed HTML content / 发出处理后的 HTML 内容的 Mono
     */
    Mono<String> spacingElementByClassName(String htmlContent, String className);

    /**
     * Apply Pangu spacing to plain text.
     * 对纯文本应用 Pangu 空格。
     *
     * @param text the plain text to process / 要处理的纯文本
     * @return Mono emitting processed text / 发出处理后的文本的 Mono
     */
    Mono<String> spacingText(String text);
}
