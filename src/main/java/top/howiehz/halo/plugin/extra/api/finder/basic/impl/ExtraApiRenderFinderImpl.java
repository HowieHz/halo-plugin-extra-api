package top.howiehz.halo.plugin.extra.api.finder.basic.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.basic.ExtraApiRenderFinder;
import top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.PanguSpacingService;

/**
 * Implementation of ExtraApiRenderFinder.
 * ExtraApiRenderFinder 的实现。
 *
 * <p>This finder implementation provides template-accessible methods for applying
 * Pangu spacing to text content in Halo themes.</p>
 * <p>此 Finder 实现为 Halo 主题提供模板可访问的方法，用于对文本内容应用 Pangu 空格。</p>
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Finder("extraApiRenderFinder")
public class ExtraApiRenderFinderImpl implements ExtraApiRenderFinder {

    private final PanguSpacingService panguSpacingService;

    @Override
    public Mono<String> applySpacingInHtml(String htmlContent) {
        return Mono.fromCallable(() ->
            panguSpacingService.applySpacingInHtml(htmlContent));
    }

    @Override
    public Mono<String> applySpacingInHtml(Map<String, Object> params) {
        return Mono.fromCallable(() ->
            panguSpacingService.applySpacingInHtml(params));
    }

    @Override
    public Mono<String> applySpacingInText(String text) {
        return Mono.fromCallable(() -> panguSpacingService.applySpacingInText(text));
    }
}
