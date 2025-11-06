package top.howiehz.halo.plugin.extra.api.finder.js;

import reactor.core.publisher.Mono;

public interface ExtraApiJsRenderFinder {
    Mono<String> highlightCodeInHtml(String htmlContent);
}
