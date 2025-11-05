package top.howiehz.halo.plugin.extra.api.finder.basic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.basic.ExtraApiPanguFinder;
import top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.PanguSpacingService;

/**
 * Implementation of ExtraApiPanguFinder.
 * ExtraApiPanguFinder 的实现。
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
@Finder("extraApiPanguFinder")
public class ExtraApiPanguFinderImpl implements ExtraApiPanguFinder {

    private final PanguSpacingService panguSpacingService;

    @Override
    public Mono<String> spacingElementByTagName(String htmlContent, String tagName) {
        return Mono.fromCallable(() ->
            panguSpacingService.spacingElementByTagName(htmlContent, tagName));
    }

    @Override
    public Mono<String> spacingElementById(String htmlContent, String id) {
        return Mono.fromCallable(() ->
            panguSpacingService.spacingElementById(htmlContent, id));
    }

    @Override
    public Mono<String> spacingElementByClassName(String htmlContent, String className) {
        return Mono.fromCallable(() ->
            panguSpacingService.spacingElementByClassName(htmlContent, className));
    }

    @Override
    public Mono<String> spacingText(String text) {
        return Mono.fromCallable(() -> panguSpacingService.spacingText(text));
    }
}
