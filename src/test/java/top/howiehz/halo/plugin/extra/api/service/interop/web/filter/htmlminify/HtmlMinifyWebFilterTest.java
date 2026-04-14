package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class HtmlMinifyWebFilterTest {
    private static final List<String> DEFAULT_EXCLUDE_PATHS = List.of(
        "/console/**",
        "/uc/**",
        "/login/**",
        "/signup/**",
        "/logout/**",
        "/themes/**",
        "/plugins/**",
        "/actuator/**",
        "/api/**",
        "/apis/**",
        "/upload/**"
    );

    private HtmlMinifyConfig config;
    private HtmlMinifyWebFilter filter;
    private CountingHtmlMinifyService service;

    @BeforeEach
    void setUp() {
        config = new HtmlMinifyConfig();
        config.setEnabledHtmlMinify(true);
        config.setExcludePaths(DEFAULT_EXCLUDE_PATHS);
        config.setKeepClosingTags(true);
        config.setKeepComments(true);
        config.setKeepHtmlAndHeadOpeningTags(true);
        config.setKeepInputTypeTextAttr(true);
        config.setKeepSsiComments(true);
        config.setMinifyCss(true);
        config.setMinifyJs(true);
        config.setPreserveBraceTemplateSyntax(true);
        config.setPreserveChevronPercentTemplateSyntax(true);
        config.setRemoveBangs(true);
        config.setRemoveProcessingInstructions(true);
        service = new CountingHtmlMinifyService();
        filter = new HtmlMinifyWebFilter(() -> Mono.just(config), service);
    }

    @Test
    void shouldMinifyImplicitOkHtmlResponsesWrittenViaWriteWith() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div><!-- comment --></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertTrue(result.contains("<!-- comment -->"));
        assertTrue(result.contains("<div>Hello</div>"));
        assertEquals(1, service.minifyCount.get());
    }

    @Test
    void shouldSkipEncodedHtmlResponses() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            response.getHeaders().set("Content-Encoding", "gzip");
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldSkipExcludedApisPaths() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/apis/content.halo.run/v1alpha1/posts")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldSkipExcludedApiPaths() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/content/posts")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldSkipConfiguredExcludedPaths() {
        config.setExcludePaths(List.of("/custom/**"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/custom/page")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldAllowApisPathWhenRemovedFromConfig() {
        config.setExcludePaths(List.of("/console/**"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/apis/content.halo.run/v1alpha1/posts")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertTrue(result.contains("<div>Hello</div>"));
        assertEquals(1, service.minifyCount.get());
    }

    @Test
    void shouldSkipNonUtf8HtmlResponses() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders()
                .setContentType(new MediaType("text", "html", StandardCharsets.ISO_8859_1));
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.ISO_8859_1));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldNotInvokeMinifierWhenFeatureDisabled() {
        config.setEnabledHtmlMinify(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div><!-- comment --></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertEquals("<html><body><div>  Hello  </div><!-- comment --></body></html>", result);
        assertEquals(0, service.minifyCount.get());
    }

    @Test
    void shouldMinifyHtmlResponsesWrittenViaWriteAndFlushWith() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div><!-- comment --></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeAndFlushWith(Mono.just(Mono.just(body)));
        }).block();

        String result = exchange.getResponse().getBodyAsString().block();

        assertTrue(result.contains("<!-- comment -->"));
        assertTrue(result.contains("<div>Hello</div>"));
        assertEquals(1, service.minifyCount.get());
    }

    @Test
    void shouldOffloadMinificationToBoundedElasticScheduler() {
        AtomicReference<String> threadName = new AtomicReference<>();
        service = new CountingHtmlMinifyService() {
            @Override
            public String minify(String html, HtmlMinifyConfig config) {
                threadName.set(Thread.currentThread().getName());
                return super.minify(html, config);
            }
        };
        filter = new HtmlMinifyWebFilter(() -> Mono.just(config), service);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/demo")
                .accept(MediaType.TEXT_HTML)
                .build()
        );

        filter.filter(exchange, decoratedExchange -> {
            var response = decoratedExchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            var body = response.bufferFactory()
                .wrap("<html><body><div>  Hello  </div></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(body));
        }).block();

        assertTrue(threadName.get().contains("boundedElastic"));
        assertEquals(1, service.minifyCount.get());
    }

    private static class CountingHtmlMinifyService extends HtmlMinifyService {
        private final AtomicInteger minifyCount = new AtomicInteger();

        @Override
        public String minify(String html, HtmlMinifyConfig config) {
            minifyCount.incrementAndGet();
            return super.minify(html, config);
        }
    }
}
