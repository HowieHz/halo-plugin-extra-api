package top.howiehz.halo.plugin.extra.api.service.interop.runtime.adapters.shiki.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.interop.runtime.adapters.shiki.ShikiHighlightService;
import top.howiehz.halo.plugin.node.runtime.api.NodeCall;
import top.howiehz.halo.plugin.node.runtime.api.NodeModuleRef;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntime;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeException;

@Service
public class ShikiHighlightServiceImpl implements ShikiHighlightService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final ExtensionGetter extensionGetter;
    private final NodeModuleRef moduleRef;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Set<String> cachedLanguages;
    private Set<String> cachedThemes;

    public ShikiHighlightServiceImpl(ExtensionGetter extensionGetter, PluginContext pluginContext) {
        this.extensionGetter = extensionGetter;
        this.moduleRef = NodeModuleRef.of(pluginContext, "shiki");
    }

    @Override
    public String highlightCode(String code, String language, String theme) {
        return call("highlightCode", Map.of(
            "code", code,
            "lang", language,
            "theme", theme
        ));
    }

    @Override
    public Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests) {
        String result = call("highlightCodeBatch", requests);
        try {
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new NodeRuntimeException(moduleRef.fullId(), "highlightCodeBatch",
                "Failed to parse Shiki batch result", null, e);
        }
    }

    @Override
    public Set<String> getSupportedLanguages() {
        if (cachedLanguages == null) {
            cachedLanguages = new HashSet<>(Arrays.asList(readArray("getSupportedLanguages")));
        }
        return cachedLanguages;
    }

    @Override
    public Set<String> getSupportedThemes() {
        if (cachedThemes == null) {
            cachedThemes = new HashSet<>(Arrays.asList(readArray("getSupportedThemes")));
        }
        return cachedThemes;
    }

    private String[] readArray(String functionName) {
        String result = call(functionName, Map.of());
        try {
            return objectMapper.readValue(result, String[].class);
        } catch (JsonProcessingException e) {
            throw new NodeRuntimeException(moduleRef.fullId(), functionName,
                "Failed to parse Shiki metadata result", null, e);
        }
    }

    private String call(String functionName, Object input) {
        try {
            String argsJson = objectMapper.writeValueAsString(input);
            NodeCall call = new NodeCall(moduleRef, functionName, argsJson, DEFAULT_TIMEOUT);
            return extensionGetter.getEnabledExtension(NodeRuntime.class)
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                    new NodeRuntimeException("nodejs-runtime extension is unavailable")))
                .flatMap(runtime -> runtime.call(call))
                .block();
        } catch (JsonProcessingException e) {
            throw new NodeRuntimeException(moduleRef.fullId(), functionName,
                "Failed to serialize Shiki call input", null, e);
        }
    }
}
