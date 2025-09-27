package top.howiehz.halo.plugin.extra.api.finder.js;

import reactor.core.publisher.Mono;

public interface ExtraApiRenderFinder {
    Mono<String> renderCodeHtml(String htmlContent);
}
