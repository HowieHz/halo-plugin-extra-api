package top.howiehz.halo.plugin.minify.html;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for HTML minify configuration that fetches settings reactively.
 * HTML 页面压缩配置的供应器，以响应式方式获取设置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlMinifyConfigSupplier implements Supplier<Mono<HtmlMinifyConfig>> {

    private final ReactiveSettingFetcher fetcher;

    @Override
    public Mono<HtmlMinifyConfig> get() {
        return fetcher.fetch("htmlMinify", HtmlMinifyConfig.class)
            .defaultIfEmpty(new HtmlMinifyConfig())
            .onErrorResume(error -> {
                log.warn("Failed to fetch html minify config, using defaults", error);
                return Mono.just(new HtmlMinifyConfig());
            });
    }
}
