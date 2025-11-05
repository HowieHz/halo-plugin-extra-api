package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu;

/**
 * Service for processing text with Pangu spacing.
 * 使用 Pangu 处理文本空格的服务。
 *
 * <p>Pangu is used to automatically insert whitespace between CJK (Chinese, Japanese, Korean)
 * characters and half-width English, digit, and symbol characters for better readability.</p>
 * <p>Pangu 用于自动在中日韩字符和半角英文、数字、符号字符之间插入空格，以提高可读性。</p>
 *
 * @author HowieXie
 * @since 1.0.0
 */
public interface PanguSpacingService {

    /**
     * Apply Pangu spacing to HTML content for specified tag elements.
     * 对 HTML 内容中的指定标签元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param tagName     the tag name to process (e.g., "p", "div") / 要处理的标签名称（例如 "p"、"div"）
     * @return processed HTML content / 处理后的 HTML 内容
     */
    String spacingElementByTagName(String htmlContent, String tagName);

    /**
     * Apply Pangu spacing to HTML element with specified ID.
     * 对 HTML 内容中具有指定 ID 的元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param id          the element ID to process / 要处理的元素 ID
     * @return processed HTML content / 处理后的 HTML 内容
     */
    String spacingElementById(String htmlContent, String id);

    /**
     * Apply Pangu spacing to HTML elements with specified class name.
     * 对 HTML 内容中具有指定 class 的元素应用 Pangu 空格。
     *
     * @param htmlContent the HTML content to process / 要处理的 HTML 内容
     * @param className   the class name to process / 要处理的 class 名称
     * @return processed HTML content / 处理后的 HTML 内容
     */
    String spacingElementByClassName(String htmlContent, String className);

    /**
     * Process plain text by applying Pangu spacing.
     * 通过应用 Pangu 空格处理纯文本。
     *
     * @param text the plain text to process / 要处理的纯文本
     * @return processed text with Pangu spacing applied / 应用 Pangu 空格处理后的文本
     */
    String spacingText(String text);
}
