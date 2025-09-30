package top.howiehz.halo.plugin.extra.api.service.js.runtime.adapters.shiki.impl;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.adapters.shiki.ShikiHighlightService;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.V8EnginePoolService;

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
     * Batch highlight multiple code requests in a single engine.
     * 在单个引擎中批量高亮多个代码块,利用 Javet 的对象转换器自动处理 Java Map 和 JS Object 的转换。
     *
     * @param requests map of id -> request / id 到请求的映射
     * @return map of id -> highlighted result / id 到高亮结果的映射
     * @throws JavetException when JS execution fails / JS 执行失败时抛出
     */
    @Override
    public Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests) 
        throws JavetException {
        
        return enginePoolService.withEngine(runtime -> {
            try (V8ValueObject global = runtime.getGlobalObject();
                 var value = global.get("highlightCodeBatch")) {
                
                if (!(value instanceof V8ValueFunction batchFunc)) {
                    throw new IllegalStateException("highlightCodeBatch function not found");
                }

                // 将 Java Map<String, CodeHighlightRequest> 转换为 JS 可接受的格式
                // Javet 的对象转换器会自动处理 Map -> JS Object 的转换
                Map<String, Map<String, String>> jsRequests = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, CodeHighlightRequest> entry : requests.entrySet()) {
                    CodeHighlightRequest req = entry.getValue();
                    Map<String, String> reqMap = Map.of(
                        "code", req.code(),
                        "lang", req.language(),
                        "theme", req.theme()
                    );
                    jsRequests.put(entry.getKey(), reqMap);
                }

                // 调用 JS 批量处理函数,Javet 会自动转换参数和返回值
                try (V8ValuePromise promise = batchFunc.call(null, jsRequests)) {
                    while (promise.isPending()) {
                        runtime.await();
                    }

                    if (promise.isFulfilled()) {
                        // Javet 会自动将 JS Object 转换为 Java Map<String, String>
                        return promise.getResult();
                    } else if (promise.isRejected()) {
                        throw new RuntimeException(
                            "Batch highlight failed: " + promise.getResultString());
                    }

                    throw new RuntimeException("Unknown promise state");
                }
            }
        });
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
            String[] langs =
                enginePoolService.executeScript("getSupportedLanguages()", String[].class);
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
            String[] themes =
                enginePoolService.executeScript("getSupportedThemes()", String[].class);
            cachedThemes = new HashSet<>(Arrays.asList(themes));
        }
        return cachedThemes;
    }
}
