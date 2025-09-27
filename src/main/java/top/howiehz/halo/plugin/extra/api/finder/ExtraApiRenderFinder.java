package top.howiehz.halo.plugin.extra.api.finder;

import reactor.core.publisher.Mono;

public interface ExtraApiRenderFinder {
    Mono<String> renderCodeHtml(String htmlContent);
}
