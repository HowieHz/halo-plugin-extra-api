package top.howiehz.halo.plugin.extra.api.service.basic.plugin;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class ShikiConfigSupplier implements Supplier<Mono<ShikiConfig>> {
    private final ReactiveSettingFetcher fetcher;

    @Override
    public Mono<ShikiConfig> get() {
        return fetcher.fetch("shiki", ShikiConfig.class);
    }
}
