package top.howiehz.halo.plugin.extra.api.service.basic.post.render.shiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertyPlaceholderHelper;
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

@Component
@RequiredArgsConstructor
public class ShikiHeadProcessor implements TemplateHeadProcessor {
    private final PluginContext pluginContext;

    private final ShikiConfigSupplier shikiConfigSupplier;

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

    private static boolean isPathMatch(List<String> patterns, PathContainer pathContainer) {
        var parser = PathPatternParser.defaultInstance;
        return patterns.stream().map(parser::parse)
            .anyMatch(pattern -> pattern.matches(pathContainer));
    }
}