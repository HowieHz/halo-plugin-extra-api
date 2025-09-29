package top.howiehz.halo.plugin.extra.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.V8EnginePoolService;

import java.lang.reflect.Method;

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
    
    @Autowired(required = false)
    private V8EnginePoolService v8EnginePoolService;

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
        
        // Unload V8 JNI library if Javet is available
        // 如果 Javet 可用，卸载 V8 JNI 库
        if (v8EnginePoolService != null) {
            unloadV8Library();
        }
    }
    
    /**
     * Unload V8 JNI library using reflection to support both full and lite versions.
     * 使用反射卸载 V8 JNI 库，以支持完整版和精简版（lite 版本不包含 Javet）。
     */
    private void unloadV8Library() {
        try {
            // Check if Javet classes are available
            // 检查 Javet 类是否可用
            Class<?> v8HostClass = Class.forName("com.caoccao.javet.interop.V8Host");
            Class<?> jsRuntimeTypeClass = Class.forName("com.caoccao.javet.interop.JSRuntimeType");
            
            log.info("开始卸载 V8 JNI 库...");
            
            // Set library reloadable to true
            // 设置库为可重新加载
            Method setLibraryReloadableMethod = v8HostClass.getMethod("setLibraryReloadable", boolean.class);
            setLibraryReloadableMethod.invoke(null, true);
            log.debug("V8Host.setLibraryReloadable(true) 调用成功");
            
            // Get V8Host instance for V8 runtime type
            // 获取 V8 运行时类型的 V8Host 实例
            // Use V8 mode (not Node mode)
            Object jsRuntimeType = jsRuntimeTypeClass.getField("V8").get(null);
            Method getInstanceMethod = v8HostClass.getMethod("getInstance", jsRuntimeTypeClass);
            Object v8Host = getInstanceMethod.invoke(null, jsRuntimeType);
            log.debug("V8Host 实例获取成功");
            
            // Unload the library
            // 卸载库
            Method unloadLibraryMethod = v8HostClass.getMethod("unloadLibrary");
            unloadLibraryMethod.invoke(v8Host);
            log.info("V8 JNI 库卸载调用成功");

            // Restore the library reloadable switch
            // 恢复库重新加载开关
            setLibraryReloadableMethod.invoke(null, false);
            log.debug("V8Host.setLibraryReloadable(false) 恢复成功");
            
            log.info("V8 JNI 库卸载完成！");
            
        } catch (ClassNotFoundException e) {
            // Javet classes not found - this is expected in lite version
            // Javet 类未找到 - 这在精简版中是预期的
            log.debug("Javet 类未找到，跳过 V8 库卸载（可能是 lite 版本）");
        } catch (Exception e) {
            // Log error but don't throw - allow plugin to stop gracefully
            // 记录错误但不抛出 - 允许插件正常停止
            log.warn("卸载 V8 JNI 库时出错: {}", e.getMessage(), e);
        }
    }
}
