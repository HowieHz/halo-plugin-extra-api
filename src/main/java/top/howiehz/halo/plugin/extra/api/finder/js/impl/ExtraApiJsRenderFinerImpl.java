package top.howiehz.halo.plugin.extra.api.finder.js.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.js.ExtraApiJsRenderFinder;
import top.howiehz.halo.plugin.extra.api.service.basic.config.ShikiConfigSupplier;
import top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki.ShikiRenderCodeService;

@Component
@RequiredArgsConstructor
@Finder("extraApiJsRenderFinder")
public class ExtraApiJsRenderFinerImpl implements ExtraApiJsRenderFinder {
    private final ShikiRenderCodeService shikiRenderCodeService;
    private final ShikiConfigSupplier shikiConfigSupplier;

    @Override
    public Mono<String> highlightCodeInHtml(String htmlContent) {
        return shikiConfigSupplier.get()
            .map(shikiConfig -> shikiRenderCodeService.renderCode(htmlContent, shikiConfig));
    }
}
