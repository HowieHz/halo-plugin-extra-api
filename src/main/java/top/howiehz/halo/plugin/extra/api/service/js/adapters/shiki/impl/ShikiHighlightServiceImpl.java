package top.howiehz.halo.plugin.extra.api.service.js.adapters.shiki.impl;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import top.howiehz.halo.plugin.extra.api.service.js.adapters.shiki.ShikiHighlightService;
import top.howiehz.halo.plugin.extra.api.service.js.engine.V8EnginePoolService;

/**
 * Implementation of Shiki highlight service.
 * Shiki 高亮服务实现，封装对 V8 引擎池的调用以执行高亮相关 JS 函数。
 */
@Service
public class ShikiHighlightServiceImpl implements ShikiHighlightService {

    private final V8EnginePoolService enginePoolService;
    private Set<String> cachedLanguages;
    private Set<String> cachedThemes;

    public ShikiHighlightServiceImpl(V8EnginePoolService enginePoolService) {
        this.enginePoolService = enginePoolService;
    }

    /**
     * Highlight code synchronously using Shiki.
     * 使用 Shiki 在 V8 中同步高亮代码。
     *
     * @param code source code / 源码
     * @param language language id / 语言标识
     * @param theme theme name / 主题名
     * @return highlighted result / 高亮结果（通常为 HTML）
     * @throws JavetException when JS execution fails / 当 JS 执行失败时抛出
     */
    @Override
    public String highlightCode(String code, String language, String theme) throws JavetException {
        return enginePoolService.withEngine(runtime -> {
            try (V8ValueObject global = runtime.getGlobalObject();
                 var value = global.get("highlightCode")) {
                if (!(value instanceof V8ValueFunction highlightFunc)) {
                    throw new IllegalStateException("highlightCode function not found");
                }

                Map<String, Object> options = Map.of("lang", language, "theme", theme);

                try (V8ValuePromise promise = highlightFunc.call(null, code, options)) {
                    while (promise.isPending()) {
                        runtime.await();
                    }

                    if (promise.isFulfilled()) {
                        return promise.getResultString();
                    } else if (promise.isRejected()) {
                        throw new RuntimeException(
                            "Highlight failed: " + promise.getResultString());
                    }

                    return "Unknown promise state";
                }
            }
        });
    }

    /**
     * Highlight code asynchronously.
     * 异步高亮，返回 CompletableFuture。
     *
     * @param code source code / 源码
     * @param language language id / 语言标识
     * @param theme theme name / 主题名
     * @return CompletableFuture with highlighted result / 包含高亮结果的 CompletableFuture
     */
    @Override
    public CompletableFuture<String> highlightCodeAsync(String code, String language,
        String theme) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return highlightCode(code, language, theme);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Batch highlight multiple requests in parallel.
     * 并行批量高亮多个请求，返回 key->result 的映射。
     *
     * @param requests map of id -> request / id 到请求的映射
     * @return map of id -> highlighted result / id 到高亮结果的映射
     */
    @Override
    public Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests) {
        return requests.entrySet().parallelStream()
            .collect(java.util.stream.Collectors.toConcurrentMap(Map.Entry::getKey, entry -> {
                try {
                    CodeHighlightRequest req = entry.getValue();
                    return highlightCode(req.code(), req.language(), req.theme());
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }));
    }

    /**
     * Get supported languages from the preloaded Shiki module.
     * 从预加载的 Shiki 模块中获取支持的语言列表。
     *
     * @return array of language identifiers / 语言标识数组
     * @throws JavetException when JS call fails / JS 调用失败时抛出
     */
    @Override
    public Set<String> getSupportedLanguages() throws JavetException {
        if (cachedLanguages == null) {
            String[] langs = enginePoolService.executeScript("getSupportedLanguages()", String[].class);
            cachedLanguages = new HashSet<>(Arrays.asList(langs));
        }
        return cachedLanguages;
    }

    /**
     * Get supported themes from the preloaded Shiki module.
     * 从预加载的 Shiki 模块中获取支持的主题列表。
     *
     * @return array of theme names / 主题名数组
     * @throws JavetException when JS call fails / JS 调用失败时抛出
     */
    @Override
    public Set<String> getSupportedThemes() throws JavetException {
        if (cachedThemes == null) {
            String[] themes = enginePoolService.executeScript("getSupportedThemes()", String[].class);
            cachedThemes = new HashSet<>(Arrays.asList(themes));
        }
        return cachedThemes;
    }
}