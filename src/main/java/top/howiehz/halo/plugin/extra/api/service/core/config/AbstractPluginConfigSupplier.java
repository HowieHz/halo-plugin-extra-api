package top.howiehz.halo.plugin.extra.api.service.core.config;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Shared supplier for plugin config objects backed by Halo settings.
 * 基于 Halo 设置读取插件配置对象的共享供应器。
 *
 * @param <T> config type / 配置类型
 */
@Slf4j
public abstract class AbstractPluginConfigSupplier<T> implements Supplier<Mono<T>> {
    private final ReactiveSettingFetcher fetcher;

    protected AbstractPluginConfigSupplier(ReactiveSettingFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public Mono<T> get() {
        return fetcher.fetch(configKey(), configType())
            .map(this::normalizeConfig)
            .switchIfEmpty(Mono.fromSupplier(() -> normalizeConfig(defaultConfig())))
            .onErrorResume(error -> {
                log.warn("Failed to fetch {} config, falling back to defaults", configKey(),
                    error);
                return Mono.just(normalizeConfig(defaultConfig()));
            });
    }

    protected T normalizeConfig(T config) {
        return config == null ? defaultConfig() : config;
    }

    protected abstract String configKey();

    protected abstract Class<T> configType();

    protected abstract T defaultConfig();
}
