package top.howiehz.halo.plugin.extra.api;

import com.caoccao.javet.interop.NodeRuntime;
import com.google.common.base.Throwables;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;
import top.howiehz.halo.plugin.extra.api.service.js.adapters.shiki.ShikiHighlightService;
import top.howiehz.halo.plugin.extra.api.service.js.engine.V8EnginePoolService;

/**
 * Plugin main class to manage the lifecycle of the plugin.
 * 插件主类，负责管理插件的生命周期。
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 * <p>每个插件只能有一个继承 {@link BasePlugin} 的主类。</p>
 */
@Slf4j
@Component
public class HaloPluginExtraApiPlugin extends BasePlugin {

    private final PostWordCountService postWordCountService;
    private final ShikiHighlightService shikiHighlightService;
    private final V8EnginePoolService enginePoolService;
    private NodeRuntime nodeRuntime;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext,
        PostWordCountService postWordCountService,
        ShikiHighlightService shikiHighlightService,
        V8EnginePoolService enginePoolService) {
        super(pluginContext);
        this.postWordCountService = postWordCountService;
        this.shikiHighlightService = shikiHighlightService;
        this.enginePoolService = enginePoolService;
    }

    /**
     * Called when the plugin is starting.
     * 插件启动时调用。
     */
    @Override
    public void start() {
        log.info("插件启动成功！");
        // Preload all caches when the plugin starts
        // 插件启动时预加载所有缓存
        // post word count cache / 文章字数缓存
        postWordCountService.warmUpAllCache();

        // 测试代码高亮
        testShikiHighlight();

        // 打印池状态
        var stats = enginePoolService.getPoolStats();
        log.info("V8 Engine pool stats: {}", stats);
    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        log.info("插件停止！");
    }

    private void testShikiHighlight() {
        try {
            // 先验证函数是否存在
            Boolean highlightExists = enginePoolService.executeScript(
                "typeof highlightCode === 'function'", Boolean.class);
            Boolean languagesExists = enginePoolService.executeScript(
                "typeof getSupportedLanguages === 'function'", Boolean.class);

            log.debug("highlightCode 函数存在: {}", highlightExists);
            log.debug("getSupportedLanguages 函数存在: {}", languagesExists);

            if (!highlightExists || !languagesExists) {
                log.error("Shiki 模块未正确加载!");
                return;
            }

            // 测试获取支持列表
            Set<String> languages = shikiHighlightService.getSupportedLanguages();
            Set<String> themes = shikiHighlightService.getSupportedThemes();

            if (languages != null && themes != null) {
                log.debug("支持的语言数量: {}", languages.size());
                log.debug("支持的主题数量: {}", themes.size());

                // 只有在获取到列表后才进行高亮测试
                String code = "const greeting = 'Hello, World!';\nconsole.log(greeting);";
                String html = shikiHighlightService.highlightCode(code, "javascript", "github-light");
                log.debug("Shiki 高亮成功，结果长度: {}", html.length());
                log.debug(html);
            } else {
                log.error("无法获取 Shiki 语言或主题列表");
            }

        } catch (Exception e) {
            log.error("Shiki 高亮测试失败:", Throwables.getRootCause(e));
        }
    }
}
