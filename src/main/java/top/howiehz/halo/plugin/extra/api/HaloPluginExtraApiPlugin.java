package top.howiehz.halo.plugin.extra.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;

/**
 * Plugin main class to manage the lifecycle of the plugin.
 * 插件主类，负责管理插件的生命周期。
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 * <p>每个插件只能有一个继承 {@link BasePlugin} 的主类。</p>
 */
@Slf4j
@Component
public class HaloPluginExtraApiPlugin extends BasePlugin {

    // Static initializer to configure Javet before any library loading
    // 静态初始化块，在任何库加载之前配置 Javet
    static {
        // 配置 Javet 库加载行为，避免重复加载原生库错误
        // 参考: https://www.caoccao.com/Javet/reference/resource_management/load_and_unload.html
        // 
        // 在 Halo 插件环境中，可能会出现以下情况：
        // 1. 插件被重新加载时，Javet 原生库已经在另一个 classloader 中加载
        // 2. 多个插件都使用 Javet，导致重复加载尝试
        // 
        // 通过设置系统属性 javet.lib.loading.suppress.error=true 来抑制
        // "already loaded in another classloader" 错误，直接使用已加载的库
        // 
        // 重要：这必须在类加载时就设置,而不是在运行时设置
        System.setProperty("javet.lib.loading.suppress.error", "true");
        log.info("Static initialization: Set javet.lib.loading.suppress.error=true");
    }

    private final PostWordCountService postWordCountService;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext,
        PostWordCountService postWordCountService) {
        super(pluginContext);
        this.postWordCountService = postWordCountService;
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
    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        log.info("插件停止！");
    }
}
