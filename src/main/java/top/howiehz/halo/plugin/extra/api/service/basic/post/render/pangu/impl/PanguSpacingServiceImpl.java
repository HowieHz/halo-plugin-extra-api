package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.impl;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.PanguSpacingService;
import ws.vinta.pangu.Pangu;

/**
 * Implementation of PanguSpacingService using Pangu.java library.
 * 使用 Pangu.java 库实现的 PanguSpacingService。
 *
 * <p>This service processes HTML content and applies Pangu spacing rules to text content
 * within specified HTML elements. It automatically inserts whitespace between CJK characters
 * and Latin characters, numbers, and symbols for better readability.</p>
 * <p>此服务处理 HTML 内容并对指定 HTML 元素中的文本内容应用 Pangu 空格规则。
 * 它自动在中日韩字符与拉丁字符、数字和符号之间插入空格，以提高可读性。</p>
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Slf4j
@Component
public class PanguSpacingServiceImpl implements PanguSpacingService {

    private final Pangu pangu;

    public PanguSpacingServiceImpl() {
        this.pangu = new Pangu();
    }

    @Override
    public String applySpacingInHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }

        try {
            Document doc = Jsoup.parse(htmlContent);

            // Process all text nodes in the body, skipping certain tags
            processTextNodes(doc.body());

            // Return processed HTML, preserving original format
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            return doc.body().html();
        } catch (Exception e) {
            log.error("Error processing HTML content with Pangu: {}", e.getMessage(), e);
            return htmlContent;
        }
    }

    @Override
    public String applySpacingInHtml(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            log.warn("Empty parameters provided");
            return "";
        }

        // Extract htmlContent (required)
        Object htmlContentObj = params.get("htmlContent");
        if (htmlContentObj == null) {
            log.warn("Missing required parameter: htmlContent");
            return "";
        }

        String htmlContent = htmlContentObj.toString();
        if (htmlContent.isEmpty()) {
            return "";
        }

        // Extract selector (optional)
        Object selectorObj = params.get("selector");
        String selector = selectorObj != null ? selectorObj.toString().trim() : null;

        try {
            Document doc = Jsoup.parse(htmlContent);

            if (StringUtils.hasText(selector)) {
                // Process only elements matching the selector
                Elements elements = doc.select(selector);

                if (elements.isEmpty()) {
                    log.debug("No elements found matching selector: {}", selector);
                    return htmlContent;
                }

                log.debug("Processing {} elements matching selector: {}", elements.size(),
                    selector);

                for (Element element : elements) {
                    processTextNodes(element);
                }
            } else {
                // No selector provided, process entire document
                processTextNodes(doc.body());
            }

            // Return processed HTML, preserving original format
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            return doc.body().html();
        } catch (Exception e) {
            log.error("Error processing HTML content with Pangu using selector '{}': {}",
                selector, e.getMessage(), e);
            return htmlContent;
        }
    }

    @Override
    public String applySpacingInText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        try {
            return pangu.spacingText(text);
        } catch (Exception e) {
            log.error("Error processing text with Pangu", e);
            return text;
        }
    }

    /**
     * Process all text nodes within an element, applying Pangu spacing.
     * 处理元素中的所有文本节点，应用 Pangu 空格规则。
     *
     * @param element the element to process / 要处理的元素
     */
    private void processTextNodes(Element element) {
        // Recursively process all child nodes
        for (int i = 0; i < element.childNodeSize(); i++) {
            var node = element.childNode(i);

            if (node instanceof TextNode textNode) {
                // Apply Pangu processing to text nodes
                String originalText = textNode.text();
                if (!originalText.isBlank()) {
                    String spacedText = pangu.spacingText(originalText);
                    textNode.text(spacedText);
                }
            } else if (node instanceof Element childElement) {
                // Recursively process child elements (but skip certain tags)
                if (!shouldSkipElement(childElement)) {
                    processTextNodes(childElement);
                }
            }
        }
    }

    /**
     * Check if an element should be skipped during Pangu processing.
     * 检查元素是否应在 Pangu 处理时跳过。
     *
     * <p>Certain elements like code blocks should preserve their original formatting
     * and not have spacing applied.</p>
     * <p>某些元素（如代码块）应保留其原始格式，不应用空格处理。</p>
     *
     * @param element the element to check / 要检查的元素
     * @return true if element should be skipped / 如果应跳过该元素则返回 true
     */
    private boolean shouldSkipElement(Element element) {
        String tagName = element.tagName().toLowerCase();

        // Skip tags that should preserve their original formatting
        return tagName.equals("code")
            || tagName.equals("pre")
            || tagName.equals("script")
            || tagName.equals("style")
            || tagName.equals("textarea");
    }
}
