package top.howiehz.halo.plugin.extra.api.service.basic.config;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for Shiki configuration that fetches settings reactively.
 * Shiki 配置的供应器，以响应式方式获取设置。
 */
@Component
@RequiredArgsConstructor
public class ShikiConfigSupplier implements Supplier<Mono<ShikiConfig>> {
    private final ReactiveSettingFetcher fetcher;

    /**
     * Get the Shiki configuration from plugin settings.
     * 从插件设置中获取 Shiki 配置。
     *
     * @return Mono emitting the Shiki configuration / 发出 Shiki 配置的 Mono
     */
    @Override
    public Mono<ShikiConfig> get() {
        return fetcher.fetch("shiki", ShikiConfig.class);
    }
}
