package top.howiehz.halo.plugin.node.runtime;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeRuntimeConfigSupplier implements Supplier<Mono<NodeRuntimeConfig>> {

    private final ReactiveSettingFetcher fetcher;

    @Override
    public Mono<NodeRuntimeConfig> get() {
        return fetcher.fetch("runtime", NodeRuntimeConfig.class)
            .defaultIfEmpty(new NodeRuntimeConfig())
            .onErrorResume(error -> {
                log.warn("Failed to fetch node runtime config, using defaults", error);
                return Mono.just(new NodeRuntimeConfig());
            });
    }
}
