package top.howiehz.halo.plugin.extra.api.service.basic.config;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for Pangu configuration that fetches settings reactively.
 * Pangu 配置的供应器，以响应式方式获取设置。
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PanguConfigSupplier implements Supplier<Mono<PanguConfig>> {
    private final ReactiveSettingFetcher fetcher;

    /**
     * Get the Pangu configuration from plugin settings.
     * 从插件设置中获取 Pangu 配置。
     *
     * @return Mono emitting the Pangu configuration / 发出 Pangu 配置的 Mono
     */
    @Override
    public Mono<PanguConfig> get() {
        return fetcher.fetch("pangu", PanguConfig.class)
            .defaultIfEmpty(new PanguConfig());
    }
}
