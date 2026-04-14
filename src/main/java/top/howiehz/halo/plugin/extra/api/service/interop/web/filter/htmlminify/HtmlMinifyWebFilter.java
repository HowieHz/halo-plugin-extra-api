package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import run.halo.app.security.AdditionalWebFilter;

/**
 * Additional web filter for minifying HTML page responses.
 * 用于压缩 HTML 页面响应的附加 Web 过滤器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlMinifyWebFilter implements AdditionalWebFilter {
    private final Supplier<Mono<HtmlMinifyConfig>> htmlMinifyConfigSupplier;
    private final HtmlMinifyService htmlMinifyService;
    private final ServerWebExchangeMatcher htmlCandidateRequestMatcher =
        pathMatchers(HttpMethod.GET, "/**");
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final Scheduler scheduler = Schedulers.boundedElastic();

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange,
        @NonNull WebFilterChain chain) {
        return htmlCandidateRequestMatcher.matches(exchange)
            .flatMap(matchResult -> {
                if (!matchResult.isMatch()) {
                    return chain.filter(exchange);
                }
                return htmlMinifyConfigSupplier.get()
                    .flatMap(config -> {
                        String path = exchange.getRequest().getPath().value();
                        if (!config.isEnabledHtmlMinify() || isExcludedPath(path, config)) {
                            return chain.filter(exchange);
                        }
                        var decoratedExchange = exchange.mutate()
                            .response(new HtmlMinifyResponseDecorator(exchange, config))
                            .build();
                        return chain.filter(decoratedExchange);
                    });
            });
    }

    boolean isExcludedPath(String path, HtmlMinifyConfig config) {
        if (path == null || path.isBlank()) {
            return false;
        }
        List<String> excludePaths = config.getExcludePaths();
        if (excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        return excludePaths.stream()
            .filter(pattern -> pattern != null && !pattern.isBlank())
            .anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    boolean isEligibleMinifyResponse(ServerHttpResponse response) {
        var statusCode = response.getStatusCode();
        return (statusCode == null || statusCode.isSameCodeAs(HttpStatus.OK))
            && isMinifiableHtmlResponse(response);
    }

    DataBuffer createHtmlResponseBuffer(String html, ServerHttpResponse response) {
        byte[] resultBytes = html.getBytes(StandardCharsets.UTF_8);
        response.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        response.getHeaders().remove(HttpHeaders.TRANSFER_ENCODING);
        response.getHeaders().setContentLength(resultBytes.length);
        return response.bufferFactory().wrap(resultBytes);
    }

    private boolean isHtmlResponse(ServerHttpResponse response) {
        return response.getHeaders().getContentType() != null
            && response.getHeaders().getContentType().includes(MediaType.TEXT_HTML);
    }

    private boolean isMinifiableHtmlResponse(ServerHttpResponse response) {
        return isHtmlResponse(response)
            && hasNoEncodedBody(response)
            && usesUtf8Charset(response);
    }

    private boolean hasNoEncodedBody(ServerHttpResponse response) {
        List<String> encodings = response.getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
        if (encodings.isEmpty()) {
            return true;
        }
        return encodings.stream()
            .filter(encoding -> encoding != null && !encoding.isBlank())
            .allMatch("identity"::equalsIgnoreCase);
    }

    private boolean usesUtf8Charset(ServerHttpResponse response) {
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null || contentType.getCharset() == null) {
            return true;
        }
        return StandardCharsets.UTF_8.equals(contentType.getCharset());
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 99;
    }

    class HtmlMinifyResponseDecorator extends ServerHttpResponseDecorator {
        private final ServerWebExchange exchange;
        private final HtmlMinifyConfig config;

        HtmlMinifyResponseDecorator(ServerWebExchange exchange, HtmlMinifyConfig config) {
            super(exchange.getResponse());
            this.exchange = exchange;
            this.config = config;
        }

        @Override
        public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
            var response = getDelegate();
            if (!isEligibleMinifyResponse(response)) {
                return super.writeWith(body);
            }
            String path = exchange.getRequest().getPath().value();
            if (path.isBlank()) {
                return super.writeWith(body);
            }
            return super.writeWith(rewriteHtmlBody(body, response, path));
        }

        @Override
        public @NonNull Mono<Void> writeAndFlushWith(
            @NonNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
            var response = getDelegate();
            if (!isEligibleMinifyResponse(response)) {
                return super.writeAndFlushWith(body);
            }
            String path = exchange.getRequest().getPath().value();
            if (path.isBlank()) {
                return super.writeAndFlushWith(body);
            }
            var flattenedBody = Flux.from(body).flatMapSequential(publisher -> publisher);
            var processedBody = rewriteHtmlBody(flattenedBody, response, path)
                .flux()
                .map(Flux::just);
            return super.writeAndFlushWith(processedBody);
        }

        private Mono<DataBuffer> rewriteHtmlBody(Publisher<? extends DataBuffer> body,
            ServerHttpResponse response,
            String path) {
            return DataBufferUtils.join(Flux.from(body)).flatMap(dataBuffer -> {
                try {
                    String html = dataBuffer.toString(StandardCharsets.UTF_8);
                    if (html.isBlank()) {
                        return Mono.just(createHtmlResponseBuffer(html, response));
                    }
                    return Mono.fromCallable(() ->
                            htmlMinifyService.minify(html, config))
                        .subscribeOn(scheduler)
                        .doOnError(error -> log.warn(
                            "Failed to minify HTML response for path [{}]", path, error))
                        .onErrorReturn(html)
                        .map(processedHtml -> createHtmlResponseBuffer(processedHtml, response));
                } finally {
                    DataBufferUtils.release(dataBuffer);
                }
            }).switchIfEmpty(Mono.fromSupplier(() -> createHtmlResponseBuffer("", response)));
        }
    }
}
