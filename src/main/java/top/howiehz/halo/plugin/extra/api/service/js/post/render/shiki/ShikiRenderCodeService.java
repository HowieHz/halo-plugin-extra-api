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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for rendering code blocks in HTML content using Shiki with parallel processing.
 * 使用 Shiki 并行渲染 HTML 内容中代码块的服务。
 *
 * <p>This service uses Java's CompletableFuture to dispatch multiple highlight requests
 * concurrently to different V8 engines in the pool, achieving true parallelism.</p>
 * <p>此服务使用 Java 的 CompletableFuture 将多个高亮请求并发分发到引擎池中的不同 V8 引擎,
 * 实现真正的并行处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShikiRenderCodeService {
    private final ShikiHighlightService shikiHighlightService;

    /**
     * Render code blocks in HTML content using Shiki with parallel processing.
     * 使用 Shiki 并行渲染 HTML 内容中的代码块,利用引擎池实现真正的并发。
     *
     * @param content the HTML content to process / 要处理的 HTML 内容
     * @param shikiConfig the Shiki configuration / Shiki 配置
     * @return the processed HTML content with highlighted code blocks / 处理后的 HTML 内容,代码块已高亮
     */
    public String renderCode(String content, ShikiConfig shikiConfig) {
        Document doc = Jsoup.parse(content);
        Elements codeElements = doc.select("pre > code");

        if (codeElements.isEmpty()) {
            return doc.body().html();
        }

        // 收集所有需要高亮的代码块
        List<CodeBlockInfo> codeBlocks = new ArrayList<>();
        List<CompletableFuture<HighlightResult>> futures = new ArrayList<>();

        for (int i = 0; i < codeElements.size(); i++) {
            Element codeElement = codeElements.get(i);
            Element preElement = codeElement.parent();
            
            if (preElement != null && "pre".equals(preElement.tagName())) {
                try {
                    String code = codeElement.text();
                    String language = extractLanguage(codeElement, preElement);
                    
                    if (language.isEmpty() || !shikiHighlightService.getSupportedLanguages()
                        .contains(language)) {
                        // 跳过渲染
                        continue;
                    }

                    CodeBlockInfo blockInfo = new CodeBlockInfo(i, preElement, code, language);
                    codeBlocks.add(blockInfo);

                    // 为每个代码块创建异步任务,分发到不同的 V8 引擎
                    if (!shikiConfig.isEnabledDoubleRenderMode()) {
                        String theme = normalizeTheme(shikiConfig.getTheme());
                        futures.add(highlightAsync(i, code, language, theme, null));
                    } else {
                        String lightTheme = normalizeTheme(shikiConfig.getLightTheme(), "min-light");
                        String darkTheme = normalizeTheme(shikiConfig.getDarkTheme(), "nord");
                        
                        futures.add(highlightAsync(i, code, language, lightTheme, "light"));
                        futures.add(highlightAsync(i, code, language, darkTheme, "dark"));
                    }
                } catch (Exception e) {
                    log.warn("Failed to prepare code block {}: {}", i, e.getMessage());
                }
            }
        }

        if (futures.isEmpty()) {
            return doc.body().html();
        }

        // 等待所有高亮任务完成
        Map<String, String> results = new ConcurrentHashMap<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()))
                .thenAccept(resultList -> {
                    for (HighlightResult result : resultList) {
                        results.put(result.key, result.html);
                    }
                })
                .join();
        } catch (Exception e) {
            log.error("Parallel highlight failed: {}", e.getMessage());
            return doc.body().html();
        }

        // 应用高亮结果
        for (CodeBlockInfo blockInfo : codeBlocks) {
            try {
                if (!shikiConfig.isEnabledDoubleRenderMode()) {
                    String key = "block-" + blockInfo.index;
                    String highlightedHtml = results.get(key);
                    
                    if (highlightedHtml != null && !highlightedHtml.startsWith("Error:")) {
                        Element highlightedDiv = doc.createElement("div");
                        highlightedDiv.append(highlightedHtml);
                        blockInfo.preElement.replaceWith(highlightedDiv);
                    }
                } else {
                    String lightKey = "block-" + blockInfo.index + "-light";
                    String darkKey = "block-" + blockInfo.index + "-dark";
                    String lightHtml = results.get(lightKey);
                    String darkHtml = results.get(darkKey);

                    if (lightHtml != null && !lightHtml.startsWith("Error:") 
                        && darkHtml != null && !darkHtml.startsWith("Error:")) {
                        
                        Element lightDiv = doc.createElement("div")
                            .attr("class", shikiConfig.getLightCodeClass());
                        Element darkDiv = doc.createElement("div")
                            .attr("class", shikiConfig.getDarkCodeClass());

                        lightDiv.append(lightHtml);
                        darkDiv.append(darkHtml);

                        blockInfo.preElement.before(lightDiv);
                        blockInfo.preElement.before(darkDiv);
                        blockInfo.preElement.remove();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to apply highlight result for block {}: {}", 
                    blockInfo.index, e.getMessage());
            }
        }

        return doc.body().html();
    }

    /**
     * Highlight code asynchronously using CompletableFuture.
     * 使用 CompletableFuture 异步高亮代码,每个任务会被分发到引擎池中的不同引擎。
     *
     * @param index block index / 代码块索引
     * @param code source code / 源码
     * @param language language id / 语言标识
     * @param theme theme name / 主题名
     * @param mode "light", "dark", or null for single mode / 渲染模式标识
     * @return CompletableFuture with highlight result / 包含高亮结果的 CompletableFuture
     */
    private CompletableFuture<HighlightResult> highlightAsync(
        int index, String code, String language, String theme, String mode) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String html = shikiHighlightService.highlightCode(code, language, theme);
                String key = mode == null 
                    ? "block-" + index 
                    : "block-" + index + "-" + mode;
                return new HighlightResult(key, html);
            } catch (Exception e) {
                log.warn("Failed to highlight block {}: {}", index, e.getMessage());
                String key = mode == null 
                    ? "block-" + index 
                    : "block-" + index + "-" + mode;
                return new HighlightResult(key, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Normalize theme name with default fallback.
     * 标准化主题名称并提供默认值。
     */
    private String normalizeTheme(String theme) {
        return normalizeTheme(theme, "min-light");
    }

    /**
     * Normalize theme name with custom default.
     * 标准化主题名称并提供自定义默认值。
     */
    private String normalizeTheme(String theme, String defaultTheme) {
        try {
            if (!shikiHighlightService.getSupportedThemes().contains(theme)) {
                return defaultTheme;
            }
            return theme;
        } catch (Exception e) {
            log.warn("Failed to validate theme: {}", e.getMessage());
            return defaultTheme;
        }
    }

    /**
     * Internal record to store code block information during parallel processing.
     * 并行处理期间存储代码块信息的内部记录类。
     */
    private record CodeBlockInfo(int index, Element preElement, String code, String language) {
    }

    /**
     * Internal record to store highlight result.
     * 存储高亮结果的内部记录类。
     */
    private record HighlightResult(String key, String html) {
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
