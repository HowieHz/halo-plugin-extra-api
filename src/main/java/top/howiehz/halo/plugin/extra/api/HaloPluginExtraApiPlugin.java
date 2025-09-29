package top.howiehz.halo.plugin.extra.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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

    private final PostWordCountService postWordCountService;
    private final ApplicationContext applicationContext;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext,
        PostWordCountService postWordCountService,
        ApplicationContext applicationContext) {
        super(pluginContext);
        this.postWordCountService = postWordCountService;
        this.applicationContext = applicationContext;
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
        log.info("插件停止中...");

        // 尝试关闭 V8 引擎池（如果存在）
        // Try to close V8 engine pool if it exists (for full version)
        try {
            // 使用反射检查是否存在 V8EnginePoolService
            // Use reflection to check if V8EnginePoolService exists
            Class<?> v8ServiceClass = Class.forName(
                "top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.impl"
                    + ".V8EnginePoolServiceImpl"
            );

            // 如果类存在，尝试从 Spring 容器获取并关闭
            // If class exists, try to get from Spring context and close
            Object v8Service = applicationContext.getBean(v8ServiceClass);
            if (v8Service instanceof org.springframework.beans.factory.DisposableBean) {
                log.info("正在关闭 V8 引擎池...");
                ((org.springframework.beans.factory.DisposableBean) v8Service).destroy();
                log.info("V8 引擎池已关闭");
            }
        } catch (ClassNotFoundException e) {
            // Lite 版本不包含 V8 引擎，这是正常情况
            // Lite version doesn't have V8 engine, this is expected
            log.debug("当前版本不包含 V8 引擎（Lite 版本）");
        } catch (Exception e) {
            // 记录但不抛出异常，避免影响插件停止流程
            // Log but don't throw, to avoid affecting plugin stop process
            log.warn("关闭 V8 引擎池时出现异常: {}", e.getMessage(), e);
        }

        log.info("插件已停止！");
    }
}
