package top.howiehz.halo.plugin.extra.api.service.basic.config;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for V8 engine pool configuration that fetches settings reactively.
 * V8 引擎池配置的供应器，以响应式方式获取设置。
 */
@Component
@RequiredArgsConstructor
public class JsEnginePoolConfigSupplier implements Supplier<Mono<JsEnginePoolConfig>> {
    private final ReactiveSettingFetcher fetcher;

    /**
     * Get the V8 engine pool configuration from plugin settings.
     * 从插件设置中获取 V8 引擎池配置。
     *
     * @return Mono emitting the V8 engine pool configuration / 发出 V8 引擎池配置的 Mono
     */
    @Override
    public Mono<JsEnginePoolConfig> get() {
        return fetcher.fetch("jsEnginePool", JsEnginePoolConfig.class);
    }
}
