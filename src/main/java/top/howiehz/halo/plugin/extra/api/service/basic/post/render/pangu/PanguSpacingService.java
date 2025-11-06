package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu;

import java.util.Map;

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
     * @return processed HTML content / 处理后的 HTML 内容
     */
    String applySpacingInHtml(String htmlContent);

    /**
     * Apply Pangu spacing to HTML content with flexible parameters.
     * 使用灵活参数对 HTML 内容应用 Pangu 空格。
     *
     * <p>Supported parameters:</p>
     * <ul>
     *   <li>htmlContent (String, required): The HTML content to process</li>
     *   <li>selector (String, optional): CSS selector to target specific elements</li>
     * </ul>
     *
     * <p>支持的参数：</p>
     * <ul>
     *   <li>htmlContent (String, 必需)：要处理的 HTML 内容</li>
     *   <li>selector (String, 可选)：用于定位特定元素的 CSS 选择器</li>
     * </ul>
     *
     * @param params map containing htmlContent (required) and optional selector
     * 包含 htmlContent（必需）和可选 selector 的映射
     * @return processed HTML content / 处理后的 HTML 内容
     */
    String applySpacingInHtml(Map<String, Object> params);

    /**
     * Process plain text by applying Pangu spacing.
     * 通过应用 Pangu 空格处理纯文本。
     *
     * @param text the plain text to process / 要处理的纯文本
     * @return processed text with Pangu spacing applied / 应用 Pangu 空格处理后的文本
     */
    String applySpacingInText(String text);
}
