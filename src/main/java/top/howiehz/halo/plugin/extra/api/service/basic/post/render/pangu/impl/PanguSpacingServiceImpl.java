package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.impl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
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
    public String spacingElementByTagName(String htmlContent, String tagName) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        if (tagName == null || tagName.isEmpty()) {
            log.warn("Tag name is null or empty, returning original content");
            return htmlContent;
        }

        try {
            // 解析 HTML
            Document doc = Jsoup.parse(htmlContent);
            
            // 选择指定标签的所有元素
            Elements elements = doc.select(tagName);
            
            if (elements.isEmpty()) {
                log.debug("No elements found with tag name: {}", tagName);
                return htmlContent;
            }

            log.debug("Processing {} <{}> elements with Pangu spacing", elements.size(), tagName);

            // 处理每个元素中的文本节点
            for (Element element : elements) {
                processTextNodes(element);
            }

            // 返回处理后的 HTML，保持原始格式
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            return doc.body().html();

        } catch (Exception e) {
            log.error("Error processing HTML content with Pangu: {}", e.getMessage(), e);
            return htmlContent; // 出错时返回原内容
        }
    }

    @Override
    public String spacingElementById(String htmlContent, String id) {
        if (htmlContent == null || htmlContent.isEmpty() || id == null || id.isEmpty()) {
            return htmlContent;
        }
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            Element element = doc.getElementById(id);
            
            if (element != null) {
                processElement(element);
            }
            
            return doc.body().html();
        } catch (Exception e) {
            log.error("Error processing HTML by ID with Pangu", e);
            return htmlContent;
        }
    }

    @Override
    public String spacingElementByClassName(String htmlContent, String className) {
        if (htmlContent == null || htmlContent.isEmpty() || className == null || className.isEmpty()) {
            return htmlContent;
        }
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements elements = doc.getElementsByClass(className);
            
            for (Element element : elements) {
                processElement(element);
            }
            
            return doc.body().html();
        } catch (Exception e) {
            log.error("Error processing HTML by class name with Pangu", e);
            return htmlContent;
        }
    }

    @Override
    public String spacingText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            return pangu.spacingText(text);
        } catch (Exception e) {
            log.error("Error processing text with Pangu", e);
            return text;
        }
    }

    /**
     * Process a single element with Pangu spacing.
     * 使用 Pangu 空格处理单个元素。
     *
     * @param element the element to process / 要处理的元素
     */
    private void processElement(Element element) {
        processTextNodes(element);
    }

    /**
     * Process all text nodes within an element, applying Pangu spacing.
     * 处理元素中的所有文本节点，应用 Pangu 空格规则。
     *
     * @param element the element to process / 要处理的元素
     */
    private void processTextNodes(Element element) {
        // 递归处理所有子节点
        for (int i = 0; i < element.childNodeSize(); i++) {
            var node = element.childNode(i);
            
            if (node instanceof TextNode textNode) {
                // 对文本节点应用 Pangu 处理
                String originalText = textNode.text();
                if (!originalText.isEmpty() && !originalText.isBlank()) {
                    String spacedText = pangu.spacingText(originalText);
                    textNode.text(spacedText);
                }
            } else if (node instanceof Element childElement) {
                // 递归处理子元素（但不处理 code、pre 等应该保留原始格式的标签）
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
        
        // 跳过代码、预格式化文本等应保留原始格式的标签
        return tagName.equals("code") 
            || tagName.equals("pre") 
            || tagName.equals("script") 
            || tagName.equals("style")
            || tagName.equals("textarea");
    }
}
