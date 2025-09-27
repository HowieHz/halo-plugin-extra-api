package top.howiehz.halo.plugin.extra.api.service.basic.post.render.shiki;

import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactiveSinglePageContentHandler;
import top.howiehz.halo.plugin.extra.api.service.basic.plugin.ShikiConfigSupplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShikiSinglePageContentRenderHandler implements ReactiveSinglePageContentHandler {
    private final ShikiConfigSupplier shikiConfigSupplier;
    private final ShikiRenderCodeService shikiRenderCodeService;

    @Override
    public Mono<SinglePageContentContext> handle(@NotNull SinglePageContentContext contentContext) {
        return shikiConfigSupplier.get().map(shikiConfig -> {
            if (!shikiConfig.isEnabledShikiRender()) {
                return contentContext;
            }
            contentContext.setContent(shikiRenderCodeService.renderCode(contentContext.getContent(), shikiConfig));
            return contentContext;
        }).onErrorResume(e -> {
            log.error("Error occurred while rendering code with Shiki: {}", Throwables.getStackTraceAsString(e));
            return Mono.just(contentContext);
        });
    }
}
