package top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki;

import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactiveSinglePageContentHandler;
import top.howiehz.halo.plugin.extra.api.service.basic.config.ShikiConfigSupplier;

/**
 * Handler for rendering code blocks in single page content using Shiki.
 * 使用 Shiki 渲染单页内容中代码块的处理器。
 *
 * <p>This handler automatically processes HTML content in single pages and applies
 * syntax highlighting to code blocks using the configured Shiki themes.</p>
 * <p>此处理器会自动处理单页中的 HTML 内容，并使用配置的 Shiki 主题
 * 对代码块应用语法高亮。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShikiSinglePageContentRenderHandler implements ReactiveSinglePageContentHandler {
    private final ShikiConfigSupplier shikiConfigSupplier;
    private final ShikiRenderCodeService shikiRenderCodeService;

    /**
     * Handle single page content rendering by applying Shiki code highlighting.
     * 处理单页内容渲染，应用 Shiki 代码高亮。
     *
     * @param contentContext the single page content context / 单页内容上下文
     * @return Mono emitting the processed content context / 发出处理后的内容上下文的 Mono
     */
    @Override
    public Mono<SinglePageContentContext> handle(@NotNull SinglePageContentContext contentContext) {
        return shikiConfigSupplier.get().map(shikiConfig -> {
            if (!shikiConfig.isEnabledShikiRender()) {
                return contentContext;
            }
            contentContext.setContent(
                shikiRenderCodeService.renderCode(contentContext.getContent(), shikiConfig));
            return contentContext;
        }).onErrorResume(e -> {
            log.error("Error occurred while rendering code with Shiki: {}",
                Throwables.getStackTraceAsString(e));
            return Mono.just(contentContext);
        });
    }
}
