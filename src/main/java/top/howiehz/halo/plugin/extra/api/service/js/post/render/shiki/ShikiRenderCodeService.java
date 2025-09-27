package top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import top.howiehz.halo.plugin.extra.api.service.basic.config.ShikiConfig;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.adapters.shiki.ShikiHighlightService;

/**
 * Service for rendering code blocks in HTML content using Shiki.
 * 使用 Shiki 渲染 HTML 内容中代码块的服务。
 *
 * <p>This service processes HTML content, finds code blocks within &lt;pre&gt;&lt;code&gt; tags,
 * and applies syntax highlighting using the configured Shiki themes.</p>
 * <p>此服务处理 HTML 内容，查找 &lt;pre&gt;&lt;code&gt; 标签内的代码块，
 * 并使用配置的 Shiki 主题应用语法高亮。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShikiRenderCodeService {
    private final ShikiHighlightService shikiHighlightService;

    /**
     * Render code blocks in HTML content using Shiki.
     * 使用 Shiki 渲染 HTML 内容中的代码块。
     *
     * @param content the HTML content to process / 要处理的 HTML 内容
     * @param shikiConfig the Shiki configuration / Shiki 配置
     * @return the processed HTML content with highlighted code blocks / 处理后的 HTML 内容，代码块已高亮
     */
    public String renderCode(String content, ShikiConfig shikiConfig) {
        Document doc = Jsoup.parse(content);
        Elements codeElements = doc.select("pre > code");

        for (Element codeElement : codeElements) {
            Element preElement = codeElement.parent();
            if (preElement != null && "pre".equals(preElement.tagName())) {
                try {
                    // 提取代码内容和语言
                    String code = codeElement.text();
                    String language = extractLanguage(codeElement, preElement);
                    if (language.isEmpty() || !shikiHighlightService.getSupportedLanguages()
                        .contains(language)) {
                        // 跳过渲染
                        continue;
                    }

                    if (!shikiConfig.isEnabledDoubleRenderMode()) {
                        String theme = shikiConfig.getTheme();
                        if (!shikiHighlightService.getSupportedThemes().contains(theme)) {
                            theme = "min-light"; // 默认主题
                        }

                        String highlightedHtml =
                            shikiHighlightService.highlightCode(code, language, theme);

                        Element highlightedDiv = doc.createElement("div");

                        // 添加渲染结果
                        highlightedDiv.append(highlightedHtml);

                        // 替换原始元素
                        preElement.replaceWith(highlightedDiv);
                    } else {
                        String lightTheme = shikiConfig.getLightTheme();
                        String darkTheme = shikiConfig.getDarkTheme();
                        if (!shikiHighlightService.getSupportedThemes().contains(lightTheme)) {
                            lightTheme = "min-light"; // 默认主题
                        }
                        if (!shikiHighlightService.getSupportedThemes().contains(darkTheme)) {
                            darkTheme = "nord"; // 默认主题
                        }

                        // 使用 Shiki 渲染
                        String lightHtml =
                            shikiHighlightService.highlightCode(code, language, lightTheme);
                        String darkHtml =
                            shikiHighlightService.highlightCode(code, language, darkTheme);

                        // 添加渲染结果
                        Element lightDiv =
                            doc.createElement("div").attr("class", shikiConfig.getLightCodeClass());

                        Element darkDiv =
                            doc.createElement("div").attr("class", shikiConfig.getDarkCodeClass());

                        // 添加渲染结果
                        lightDiv.append(lightHtml);
                        darkDiv.append(darkHtml);

                        // 插入两个同级 div
                        preElement.before(lightDiv);
                        preElement.before(darkDiv);
                        // 移除原始 pre 元素
                        preElement.remove();
                    }
                } catch (Exception e) {
                    log.warn("Failed to highlight code with Shiki, keeping original: {}",
                        e.getMessage());
                    // 保持原始代码块不变
                }
            }
        }

        return doc.body().html();
    }

    private String extractLanguage(Element codeElement, Element preElement) {
        // 尝试从 class 属性提取语言
        String codeClass = codeElement.attr("class");
        String preClass = preElement.attr("class");

        // 匹配 language-xxx 或 lang-xxx 格式
        if (codeClass.contains("language-")) {
            return codeClass.replaceFirst(".*language-([\\w-]+).*", "$1");
        }
        if (codeClass.contains("lang-")) {
            return codeClass.replaceFirst(".*lang-([\\w-]+).*", "$1");
        }
        if (preClass.contains("language-")) {
            return preClass.replaceFirst(".*language-([\\w-]+).*", "$1");
        }
        if (preClass.contains("lang-")) {
            return preClass.replaceFirst(".*lang-([\\w-]+).*", "$1");
        }

        // 默认语言
        return "text";
    }
}