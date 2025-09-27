package top.howiehz.halo.plugin.extra.api;

import com.caoccao.javet.interop.NodeRuntime;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.PostWordCountService;
import top.howiehz.halo.plugin.extra.api.service.js.V8EnginePoolService;
import top.howiehz.halo.plugin.extra.api.service.js.shiki.ShikiHighlightService;
import java.util.Arrays;

/**
 * Plugin main class to manage the lifecycle of the plugin.
 * 插件主类，负责管理插件的生命周期。
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 * <p>每个插件只能有一个继承 {@link BasePlugin} 的主类。</p>
 */
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
        System.out.println("插件启动成功！");
        // Preload all caches when the plugin starts
        // 插件启动时预加载所有缓存
        // post word count cache / 文章字数缓存
        postWordCountService.warmUpAllCache();

        // 测试代码高亮
        testShikiHighlight();

        // 打印池状态
        var stats = enginePoolService.getPoolStats();
        System.out.println("V8 Engine pool stats: " + stats);
    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        System.out.println("插件停止！");
    }

    private void testShikiHighlight() {
        try {
            // 先验证函数是否存在
            Boolean highlightExists = enginePoolService.executeScript(
                "typeof highlightCode === 'function'", Boolean.class);
            Boolean languagesExists = enginePoolService.executeScript(
                "typeof getSupportedLanguages === 'function'", Boolean.class);

            System.out.println("highlightCode 函数存在: " + highlightExists);
            System.out.println("getSupportedLanguages 函数存在: " + languagesExists);

            if (!highlightExists || !languagesExists) {
                System.err.println("Shiki 模块未正确加载!");
                return;
            }

            // 测试获取支持列表
            String[] languages = shikiHighlightService.getSupportedLanguages();
            String[] themes = shikiHighlightService.getSupportedThemes();

            if (languages != null && themes != null) {
                System.out.println("支持的语言数量: " + languages.length);
                System.out.println("支持的主题数量: " + themes.length);

                // 只有在获取到列表后才进行高亮测试
                String code = "const greeting = 'Hello, World!';\nconsole.log(greeting);";
                String html = shikiHighlightService.highlightCode(code, "javascript", "github-light");
                System.out.println("Shiki 高亮成功，结果长度: " + html.length());
                System.out.println(html);
            } else {
                System.err.println("无法获取语言或主题列表");
            }

        } catch (Exception e) {
            System.err.println("Shiki 高亮测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
