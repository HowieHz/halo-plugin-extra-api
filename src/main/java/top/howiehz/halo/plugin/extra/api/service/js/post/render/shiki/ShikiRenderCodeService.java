package top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import top.howiehz.halo.plugin.extra.api.service.basic.config.ShikiConfig;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.adapters.shiki.ShikiHighlightService;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.V8EnginePoolService;

/**
 * Service for rendering code blocks using intelligent batch distribution strategy.
 * 使用智能批量分配策略渲染代码块的服务。
 *
 * <p>This service intelligently groups highlight requests based on available engine pool size,
 * using batch processing within each engine to minimize overhead while maximizing parallelism.</p>
 * <p>此服务根据可用引擎池大小智能分组高亮请求,在每个引擎中使用批量处理以最小化开销并最大化并行度。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShikiRenderCodeService {
    private final ShikiHighlightService shikiHighlightService;
    private final ShikiRenderCache renderCache;
    private final V8EnginePoolService v8EnginePoolService;
    private final ShikiCacheMetrics metrics;

    /**
     * Render code blocks with intelligent batch distribution.
     * 使用智能批量分配策略渲染代码块。
     *
     * <p><b>策略:</b> 根据引擎池大小动态分组任务,例如:
     * <ul>
     *   <li>14 个任务 + 5 个引擎 → 5 组(每组 2-3 个任务)</li>
     *   <li>每组任务在同一个引擎中批量处理,避免任务过度分散</li>
     * </ul></p>
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

        // 收集所有需要高亮的请求,同时进行去重
        List<CodeBlockInfo> codeBlocks = new ArrayList<>();
        List<HighlightRequest> allRequests = new ArrayList<>();

        // 用于去重: key = code|language|theme, value = list of block indices
        Map<String, List<Integer>> deduplicationMap = new java.util.LinkedHashMap<>();

        for (int i = 0; i < codeElements.size(); i++) {
            Element codeElement = codeElements.get(i);
            Element preElement = codeElement.parent();

            if (preElement != null && "pre".equals(preElement.tagName())) {
                try {
                    String code = codeElement.text();
                    String language = extractLanguage(codeElement, preElement);

                    if (language.isEmpty() || !shikiHighlightService.getSupportedLanguages()
                        .contains(language)) {
                        continue;
                    }

                    CodeBlockInfo blockInfo = new CodeBlockInfo(i, preElement, code, language);
                    codeBlocks.add(blockInfo);

                    // 创建高亮请求并记录去重信息
                    if (!shikiConfig.isEnabledDoubleRenderMode()) {
                        String theme = normalizeTheme(shikiConfig.getTheme());
                        String dedupKey = code + "|" + language + "|" + theme;

                        deduplicationMap.computeIfAbsent(dedupKey, k -> new ArrayList<>()).add(i);

                        // 只为第一次出现的代码块创建请求
                        if (deduplicationMap.get(dedupKey).size() == 1) {
                            allRequests.add(
                                new HighlightRequest("block-" + i, code, language, theme));
                        }
                    } else {
                        String lightTheme =
                            normalizeTheme(shikiConfig.getLightTheme(), "min-light");
                        String darkTheme = normalizeTheme(shikiConfig.getDarkTheme(), "nord");

                        String lightDedupKey = code + "|" + language + "|" + lightTheme;
                        String darkDedupKey = code + "|" + language + "|" + darkTheme;

                        deduplicationMap.computeIfAbsent(lightDedupKey, k -> new ArrayList<>())
                            .add(i);
                        deduplicationMap.computeIfAbsent(darkDedupKey, k -> new ArrayList<>())
                            .add(i);

                        // 只为第一次出现的代码块创建请求
                        if (deduplicationMap.get(lightDedupKey).size() == 1) {
                            allRequests.add(
                                new HighlightRequest("block-" + i + "-light", code, language,
                                    lightTheme));
                        }
                        if (deduplicationMap.get(darkDedupKey).size() == 1) {
                            allRequests.add(
                                new HighlightRequest("block-" + i + "-dark", code, language,
                                    darkTheme));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to prepare code block {}: {}", i, e.getMessage());
                }
            }
        }

        if (allRequests.isEmpty()) {
            return doc.body().html();
        }

        // 统计去重效果
        int totalBlocks = codeBlocks.size();
        int uniqueRequests = allRequests.size();
        int duplicates = (shikiConfig.isEnabledDoubleRenderMode() ? totalBlocks * 2 : totalBlocks)
            - uniqueRequests;
        if (duplicates > 0) {
            metrics.recordDeduplication(duplicates);
            log.debug("代码块去重: 总块数={}, 唯一请求={}, 去重节省={}",
                totalBlocks, uniqueRequests, duplicates);
        }

        // 智能分组并并行处理
        Instant startTime = Instant.now();
        Map<String, String> results = processRequestsIntelligently(allRequests);
        metrics.recordRenderTime(startTime);

        // 输出统计信息
        if (log.isDebugEnabled()) {
            ShikiCacheMetrics.MetricsSnapshot snapshot = metrics.getSnapshot();
            log.debug("Shiki 渲染统计: 缓存命中率={}%, 去重节省={}, 平均耗时={}ms, 缓存大小={}",
                String.format("%.1f", snapshot.getHitRatePercent()),
                snapshot.getDeduplicatedRequests(),
                String.format("%.1f", snapshot.getAvgRenderTimeMs()),
                renderCache.size());
        }

        // 批量应用高亮结果 - 使用两阶段 DOM 操作减少性能开销
        // 阶段1: 收集所有新节点和需要删除的旧节点
        Map<Element, List<Element>> replacementMap = new java.util.LinkedHashMap<>();
        List<Element> toRemove = new ArrayList<>();

        for (CodeBlockInfo blockInfo : codeBlocks) {
            try {
                if (!shikiConfig.isEnabledDoubleRenderMode()) {
                    String theme = normalizeTheme(shikiConfig.getTheme());
                    String dedupKey = blockInfo.code + "|" + blockInfo.language + "|" + theme;
                    List<Integer> sameBlocks = deduplicationMap.get(dedupKey);

                    // 找到第一个代码块的渲染结果
                    int firstIndex = sameBlocks.getFirst();
                    String key = "block-" + firstIndex;
                    String highlightedHtml = results.get(key);

                    if (highlightedHtml != null && !highlightedHtml.startsWith("Error:")) {
                        Element highlightedDiv = doc.createElement("div");
                        highlightedDiv.html(highlightedHtml);

                        // 收集替换操作
                        replacementMap.put(blockInfo.preElement, List.of(highlightedDiv));
                        toRemove.add(blockInfo.preElement);
                    }
                } else {
                    String lightTheme = normalizeTheme(shikiConfig.getLightTheme(), "min-light");
                    String darkTheme = normalizeTheme(shikiConfig.getDarkTheme(), "nord");
                    String lightDedupKey =
                        blockInfo.code + "|" + blockInfo.language + "|" + lightTheme;
                    String darkDedupKey =
                        blockInfo.code + "|" + blockInfo.language + "|" + darkTheme;

                    // 找到第一个代码块的渲染结果
                    int firstLightIndex = deduplicationMap.get(lightDedupKey).getFirst();
                    int firstDarkIndex = deduplicationMap.get(darkDedupKey).getFirst();

                    String lightKey = "block-" + firstLightIndex + "-light";
                    String darkKey = "block-" + firstDarkIndex + "-dark";
                    String lightHtml = results.get(lightKey);
                    String darkHtml = results.get(darkKey);

                    if (lightHtml != null && !lightHtml.startsWith("Error:")
                        && darkHtml != null && !darkHtml.startsWith("Error:")) {

                        Element lightDiv = doc.createElement("div")
                            .attr("class", shikiConfig.getLightCodeClass());
                        Element darkDiv = doc.createElement("div")
                            .attr("class", shikiConfig.getDarkCodeClass());

                        lightDiv.html(lightHtml);
                        darkDiv.html(darkHtml);

                        // 收集替换操作(双主题需要插入两个元素)
                        replacementMap.put(blockInfo.preElement, List.of(lightDiv, darkDiv));
                        toRemove.add(blockInfo.preElement);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to prepare replacement for block {}: {}",
                    blockInfo.index, e.getMessage());
            }
        }

        // 阶段2: 批量执行 DOM 操作
        // 2.1 批量插入所有新节点
        for (Map.Entry<Element, List<Element>> entry : replacementMap.entrySet()) {
            Element oldElement = entry.getKey();
            List<Element> newElements = entry.getValue();

            for (Element newElement : newElements) {
                oldElement.before(newElement);
            }
        }

        // 2.2 批量删除所有旧节点
        for (Element element : toRemove) {
            element.remove();
        }

        log.debug("DOM 批量操作完成: 替换了 {} 个代码块", toRemove.size());

        return doc.body().html();
    }

    /**
     * Process requests intelligently by grouping them based on engine pool size.
     * 根据引擎池大小智能分组并处理请求。
     *
     * <p><b>算法:</b>
     * <ol>
     *   <li>计算最优分组数 = min(请求数, 引擎池大小)</li>
     *   <li>将请求均匀分配到各组</li>
     *   <li>每组在一个引擎中批量处理</li>
     *   <li>多个组并行执行</li>
     * </ol></p>
     *
     * @param allRequests all highlight requests / 所有高亮请求
     * @return map of id -> highlighted result / id 到高亮结果的映射
     */
    private Map<String, String> processRequestsIntelligently(List<HighlightRequest> allRequests) {
        int totalRequests = allRequests.size();
        Map<String, String> allResults = new ConcurrentHashMap<>();

        // 第一步:检查缓存,分离出需要实际渲染的请求
        List<HighlightRequest> requestsToRender = new ArrayList<>();
        int cacheHits = 0;

        for (HighlightRequest req : allRequests) {
            String cached = renderCache.get(req.code, req.language, req.theme);
            if (cached != null) {
                // 缓存命中
                allResults.put(req.id, cached);
                cacheHits++;
            } else {
                // 缓存未命中,需要渲染
                requestsToRender.add(req);
            }
        }

        log.debug("缓存检查: 总请求={}, 缓存命中={}, 需要渲染={}, 命中率={}%",
            totalRequests, cacheHits, requestsToRender.size(),
            totalRequests > 0 ? String.format("%.1f", (cacheHits * 100.0 / totalRequests)) : "0.0");

        // 如果全部命中缓存,直接返回
        if (requestsToRender.isEmpty()) {
            log.debug("所有请求均命中缓存,跳过渲染");
            return allResults;
        }

        // 第二步:对未命中的请求进行批量渲染
        // 计算最优分组数:如果请求少于引擎数,就按请求数分组;否则充分利用引擎池
        int numGroups = Math.min(requestsToRender.size(), v8EnginePoolService.getPoolMaxSize());

        log.debug("智能分组: {} 个请求分配到 {} 个引擎(池大小: {})",
            requestsToRender.size(), numGroups, v8EnginePoolService.getPoolMaxSize());

        // 将需要渲染的请求分组
        List<List<HighlightRequest>> groups = partitionRequests(requestsToRender, numGroups);

        // 为每组创建异步批量处理任务
        List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            List<HighlightRequest> group = groups.get(i);
            int groupIndex = i;

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("组 {} 开始处理 {} 个请求", groupIndex, group.size());

                    // 转换为批量请求格式
                    Map<String, ShikiHighlightService.CodeHighlightRequest> batchRequests
                        = new java.util.LinkedHashMap<>();

                    for (HighlightRequest req : group) {
                        batchRequests.put(req.id,
                            new ShikiHighlightService.CodeHighlightRequest(
                                req.code, req.language, req.theme));
                    }

                    // 在单个引擎中批量处理
                    Map<String, String> results =
                        shikiHighlightService.highlightCodeBatch(batchRequests);

                    log.debug("组 {} 完成处理", groupIndex);
                    return results;

                } catch (Exception e) {
                    log.error("组 {} 处理失败: {}", groupIndex, e.getMessage());

                    // 返回错误结果
                    Map<String, String> errorResults = new java.util.HashMap<>();
                    for (HighlightRequest req : group) {
                        errorResults.put(req.id, "Error: " + e.getMessage());
                    }
                    return errorResults;
                }
            }));
        }

        // 等待所有组完成并合并渲染结果
        Map<String, String> renderResults = new ConcurrentHashMap<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()))
                .thenAccept(resultsList -> {
                    for (Map<String, String> results : resultsList) {
                        renderResults.putAll(results);
                    }
                })
                .join();
        } catch (Exception e) {
            log.error("智能批量处理失败: {}", e.getMessage());
        }

        // 第三步:将新渲染的结果写入缓存
        for (HighlightRequest req : requestsToRender) {
            String html = renderResults.get(req.id);
            if (html != null && !html.startsWith("Error:")) {
                // 只缓存成功的结果,错误结果不缓存
                renderCache.put(req.code, req.language, req.theme, html);
            }
        }

        // 合并缓存结果和新渲染结果
        allResults.putAll(renderResults);

        log.debug("渲染完成: 总结果={}, 缓存大小={}", allResults.size(), renderCache.size());

        return allResults;
    }

    /**
     * Partition requests into groups evenly.
     * 将请求均匀分配到各组。
     *
     * @param requests all requests / 所有请求
     * @param numGroups number of groups / 分组数
     * @return list of request groups / 请求分组列表
     */
    private List<List<HighlightRequest>> partitionRequests(
        List<HighlightRequest> requests, int numGroups) {

        List<List<HighlightRequest>> groups = new ArrayList<>();
        int groupSize = (int) Math.ceil((double) requests.size() / numGroups);

        for (int i = 0; i < requests.size(); i += groupSize) {
            int end = Math.min(i + groupSize, requests.size());
            groups.add(new ArrayList<>(requests.subList(i, end)));
        }

        return groups;
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

    /**
     * Internal record for highlight request.
     * 高亮请求的内部记录类。
     */
    private record HighlightRequest(String id, String code, String language, String theme) {
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
}
