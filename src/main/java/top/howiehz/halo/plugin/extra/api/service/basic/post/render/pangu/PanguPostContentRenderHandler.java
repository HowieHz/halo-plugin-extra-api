package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu;

import com.google.common.base.Throwables;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactivePostContentHandler;
import top.howiehz.halo.plugin.extra.api.service.basic.config.PanguConfig;

/**
 * Handler for applying Pangu spacing to post content.
 * 为文章内容应用 Pangu 空格处理的处理器。
 *
 * <p>This handler automatically processes HTML content in posts and applies
 * Pangu spacing rules to paragraph elements, improving readability by inserting
 * whitespace between CJK and Latin characters.</p>
 * <p>此处理器会自动处理文章中的 HTML 内容，并对段落元素应用 Pangu 空格规则，
 * 通过在中日韩字符和拉丁字符之间插入空格来提高可读性。</p>
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PanguPostContentRenderHandler implements ReactivePostContentHandler {

    private final PanguSpacingService panguSpacingService;
    private final Supplier<Mono<PanguConfig>> panguConfigSupplier;

    /**
     * Handle post content rendering by applying Pangu spacing to paragraph elements.
     * 处理文章内容渲染，对段落元素应用 Pangu 空格规则。
     *
     * @param contentContext the post content context / 文章内容上下文
     * @return Mono emitting the processed content context / 发出处理后的内容上下文的 Mono
     */
    @Override
    public Mono<PostContentContext> handle(@NotNull PostContentContext contentContext) {
        return panguConfigSupplier.get()
            .flatMap(config -> {
                // 检查是否启用 Pangu 自动渲染
                if (!config.isEnabledPanguRender()) {
                    log.debug("Pangu auto-render is disabled, skipping processing");
                    return Mono.just(contentContext);
                }

                return Mono.fromCallable(() -> {
                    try {
                        String originalContent = contentContext.getContent();

                        // 对 <p> 标签应用 Pangu 空格处理
                        String processedContent = panguSpacingService.applySpacingInHtml(
                            Map.of("htmlContent", originalContent, "selector", "p"));

                        contentContext.setContent(processedContent);

                        log.debug("Successfully applied Pangu spacing to post content");
                        return contentContext;

                    } catch (Exception e) {
                        log.error("Error occurred while applying Pangu spacing: {}",
                            Throwables.getStackTraceAsString(e));
                        // 出错时返回原内容
                        return contentContext;
                    }
                });
            });
    }
}
