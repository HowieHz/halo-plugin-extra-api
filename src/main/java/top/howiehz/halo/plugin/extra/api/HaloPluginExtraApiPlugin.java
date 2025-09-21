package top.howiehz.halo.plugin.extra.api;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.PostWordCountService;
import top.howiehz.halo.plugin.extra.api.service.PostWordCountServiceImpl;

/**
 * Plugin main class to manage the lifecycle of the plugin.
 * 插件主类，负责管理插件的生命周期。
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 * <p>每个插件只能有一个继承 {@link BasePlugin} 的主类。</p>
 */
@Component
public class HaloPluginExtraApiPlugin extends BasePlugin {

    private final PostWordCountService postWordCountService;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext, PostWordCountService postWordCountServiceImpl) {
        super(pluginContext);
        this.postWordCountService = postWordCountServiceImpl;
    }

    /**
     * Called when the plugin is starting.
     * 插件启动时调用。
     */
    @Override
    public void start() {
        System.out.println("插件启动成功！");

    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        System.out.println("插件停止！");
    }
}
