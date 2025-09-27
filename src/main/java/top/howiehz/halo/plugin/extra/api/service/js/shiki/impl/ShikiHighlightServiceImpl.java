package top.howiehz.halo.plugin.extra.api.service.js.shiki.impl;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.values.primitive.V8ValueString;
import com.caoccao.javet.values.reference.V8ValueArray;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import top.howiehz.halo.plugin.extra.api.service.js.V8EnginePoolService;
import top.howiehz.halo.plugin.extra.api.service.js.shiki.ShikiHighlightService;

/**
 * Shiki 代码高亮服务实现
 */
@Service
public class ShikiHighlightServiceImpl implements ShikiHighlightService {

    private final V8EnginePoolService enginePoolService;

    public ShikiHighlightServiceImpl(V8EnginePoolService enginePoolService) {
        this.enginePoolService = enginePoolService;
    }

    @Override
    public String highlightCode(String code, String language, String theme) throws JavetException {
        return enginePoolService.withEngine(runtime -> {
            try (V8ValueObject global = runtime.getGlobalObject()) {
                try (var value = global.get("highlightCode")) {
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
                            throw new RuntimeException("Highlight failed: " + promise.getResultString());
                        }

                        return "Unknown promise state";
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<String> highlightCodeAsync(String code, String language, String theme) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return highlightCode(code, language, theme);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests) {
        return requests.entrySet().parallelStream()
            .collect(java.util.stream.Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry -> {
                    try {
                        CodeHighlightRequest req = entry.getValue();
                        return highlightCode(req.code(), req.language(), req.theme());
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
            ));
    }

    @Override
    public String[] getSupportedLanguages() throws JavetException {
        return enginePoolService.withEngine(runtime -> {
            try (V8ValueObject global = runtime.getGlobalObject();
                 var value = global.get("getSupportedLanguages")) {
                if (!(value instanceof V8ValueFunction getSupportedLanguagesFunc)) {
                    throw new IllegalStateException("getSupportedLanguages function not found");
                }
                try (V8ValueArray v8ValueArray = getSupportedLanguagesFunc.call(null);) {
                    String[] languages = new String[v8ValueArray.getLength()];
                    AtomicInteger index = new AtomicInteger(0);
                    v8ValueArray.forEach((V8ValueString v) -> {
                        languages[index.getAndIncrement()] = v.getValue();
                        v.close();
                    });
                    return languages;
                }
            }
        });
    }

    @Override
    public String[] getSupportedThemes() throws JavetException {
        // 修改：直接调用 JS 函数
        return enginePoolService.executeScript("getSupportedThemes()", String[].class);
    }
}