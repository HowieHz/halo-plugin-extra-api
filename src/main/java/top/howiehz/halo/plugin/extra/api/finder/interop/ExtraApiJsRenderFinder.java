package top.howiehz.halo.plugin.extra.api.finder.interop;

import reactor.core.publisher.Mono;

public interface ExtraApiJsRenderFinder {
    Mono<String> highlightCodeInHtml(String htmlContent);
}
