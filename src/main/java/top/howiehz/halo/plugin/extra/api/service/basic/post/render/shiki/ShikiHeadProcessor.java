package top.howiehz.halo.plugin.extra.api.service.basic.post.render.shiki;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.pattern.PathPatternParser;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateHeadProcessor;
import top.howiehz.halo.plugin.extra.api.service.basic.plugin.ShikiConfig;
import top.howiehz.halo.plugin.extra.api.service.basic.plugin.ShikiConfigSupplier;

/**
 * Thymeleaf processor that injects Shiki CSS styles into the HTML head.
 * Thymeleaf 处理器，在 HTML head 中注入 Shiki CSS 样式。
 *
 * <p>This processor automatically adds the necessary CSS styles for Shiki code highlighting
 * to the &lt;head&gt; section of pages that contain code blocks.</p>
 * <p>此处理器会自动将 Shiki 代码高亮所需的 CSS 样式添加到包含代码块的页面
 * 的 &lt;head&gt; 部分。</p>
 */
@Component
@RequiredArgsConstructor
public class ShikiHeadProcessor implements TemplateHeadProcessor {
    private final PluginContext pluginContext;

    private final ShikiConfigSupplier shikiConfigSupplier;

    /**
     * Check if the current request path matches any of the configured patterns.
     * 检查当前请求路径是否与任何配置的模式匹配。
     *
     * @param patterns the list of path patterns to check against / 要检查的路径模式列表
     * @param pathContainer the path container representing the current request path / 表示当前请求路径的路径容器
     * @return true if any pattern matches, false otherwise / 如果有任何模式匹配则返回 true，否则返回 false
     */
    private static boolean isPathMatch(List<String> patterns, PathContainer pathContainer) {
        var parser = PathPatternParser.defaultInstance;
        return patterns.stream().map(parser::parse)
            .anyMatch(pattern -> pattern.matches(pathContainer));
    }

    /**
     * Process the head element and inject Shiki styles if needed.
     * 处理 head 元素，根据需要注入 Shiki 样式。
     *
     * @param context the template context / 模板上下文
     * @param model the model to modify / 要修改的模型
     * @param structureHandler the structure handler / 结构处理器
     * @return Mono that completes when processing is done / 处理完成时完成的 Mono
     */
    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        if (!Contexts.isWebContext(context)) {
            return Mono.empty();
        }

        var webContext = Contexts.asWebContext(context);

        return shikiConfigSupplier.get().filter(ShikiConfig::isEnabledShikiRender)
            .doOnNext(shikiConfig -> {
                final IModelFactory modelFactory = context.getModelFactory();

                var extraInjectPaths = shikiConfig.getExtraInjectPaths();
                if (CollectionUtils.isEmpty(extraInjectPaths)) {
                    return;
                }
                var requestPath = webContext.getExchange().getRequest().getPathWithinApplication();
                var pathContainer = PathContainer.parsePath(requestPath);

                if ("post".equals(context.getVariable("_templateId")) || "page".equals(
                    context.getVariable("_templateId")) || isPathMatch(extraInjectPaths,
                    pathContainer)) {
                    model.add(modelFactory.createText(
                        "<style>" + shikiConfig.getInlineStyle() + "</style>"));
                }
            }).then();
    }
}