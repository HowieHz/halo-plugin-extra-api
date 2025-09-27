package top.howiehz.halo.plugin.extra.api.finder.js.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.js.ExtraApiRenderFinder;
import top.howiehz.halo.plugin.extra.api.service.basic.config.ShikiConfigSupplier;
import top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki.ShikiRenderCodeService;

@Component
@RequiredArgsConstructor
@Finder("extraApiRenderFinder")
public class ExtraApiRenderFinerImpl implements ExtraApiRenderFinder {
    private final ShikiRenderCodeService shikiRenderCodeService;
    private final ShikiConfigSupplier shikiConfigSupplier;

    @Override
    public Mono<String> renderCodeHtml(String htmlContent) {
        return shikiConfigSupplier.get()
            .map(shikiConfig -> shikiRenderCodeService.renderCode(htmlContent, shikiConfig));
    }
}
