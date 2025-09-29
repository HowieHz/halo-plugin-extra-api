package top.howiehz.halo.plugin.extra.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;

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
    
    private Object v8EnginePoolService;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext,
        PostWordCountService postWordCountService) {
        super(pluginContext);
        this.postWordCountService = postWordCountService;
    }
    
    /**
     * Setter injection for V8EnginePoolService to avoid ClassNotFoundException in lite version.
     * 使用 setter 注入 V8EnginePoolService 以避免 lite 版本中的 ClassNotFoundException。
     * 
     * We use Object type and setter injection to avoid Spring trying to load the class
     * during component scanning, which would fail in lite version where the class doesn't exist.
     * 我们使用 Object 类型和 setter 注入，以避免 Spring 在组件扫描时尝试加载该类，
     * 这在类不存在的 lite 版本中会失败。
     */
    @Autowired(required = false)
    public void setV8EnginePoolService(Object v8EnginePoolService) {
        this.v8EnginePoolService = v8EnginePoolService;
    }

    /**
     * Called when the plugin is starting.
     * 插件启动时调用。
     */
    @Override
    public void start() {
        log.info("插件启动成功！");
        
        // Setup library loading listener to suppress "already loaded" errors
        // 设置库加载监听器以抑制"已加载"错误
        setupLibLoadingListener();
        
        // Preload all caches when the plugin starts
        // 插件启动时预加载所有缓存
        // post word count cache / 文章字数缓存
        postWordCountService.warmUpAllCache();
    }
    
    /**
     * Setup Javet library loading listener to suppress "already loaded" errors.
     * 设置 Javet 库加载监听器以抑制"已加载"错误。
     * 
     * This is necessary because after unloadLibrary() is called, the actual unloading
     * depends on GC. If the plugin is reinstalled before GC completes, the library
     * may still be in memory, causing an "already loaded" error.
     * 
     * 这是必要的，因为调用 unloadLibrary() 后，实际的卸载依赖于 GC。
     * 如果在 GC 完成之前重新安装插件，库可能仍在内存中，导致"已加载"错误。
     */
    private void setupLibLoadingListener() {
        try {
            Class<?> javetLibLoaderClass = Class.forName("com.caoccao.javet.interop.loader.JavetLibLoader");
            Class<?> listenerInterface = Class.forName("com.caoccao.javet.interop.loader.IJavetLibLoadingListener");
            
            // Create a listener that suppresses "already loaded" errors
            // 创建一个抑制"已加载"错误的监听器
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[] { listenerInterface },
                (proxy, method, args) -> {
                    if ("isSuppressingError".equals(method.getName())) {
                        // Suppress the "already loaded" error
                        // 抑制"已加载"错误
                        return true;
                    }
                    // Use default behavior for other methods
                    // 其他方法使用默认行为
                    return null;
                }
            );
            
            // Set the listener - must be called before V8Host is called
            // 设置监听器 - 必须在 V8Host 被调用之前
            Method setListenerMethod = javetLibLoaderClass.getMethod("setLibLoadingListener", listenerInterface);
            setListenerMethod.invoke(null, listener);
            
            log.info("Javet 库加载监听器设置成功，已启用 'already loaded' 错误抑制");
            
        } catch (ClassNotFoundException e) {
            // Javet not available - expected in lite version
            // Javet 不可用 - lite 版本中的预期行为
            log.debug("Javet 类未找到，跳过库加载监听器设置（可能是 lite 版本）");
        } catch (Exception e) {
            log.warn("设置 Javet 库加载监听器时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        log.info("插件停止！");
        
        // Close engine pool and unload V8 JNI library if Javet is available
        // 关闭引擎池并卸载 V8 JNI 库（如果 Javet 可用）
        if (v8EnginePoolService != null) {
            closeEnginePoolAndUnloadV8();
        }
    }
    
    /**
     * Close engine pool and unload V8 library following Javet official documentation.
     * 按照 Javet 官方文档关闭引擎池并卸载 V8 库。
     * 
     * Steps according to documentation:
     * 1. Close all V8 values and V8 runtimes (close engine pool)
     * 2. Set library reloadable to true
     * 3. Get V8Host instance
     * 4. Call unloadLibrary()
     * 5. Restore library reloadable to false
     */
    private void closeEnginePoolAndUnloadV8() {
        try {
            // Step 1: Close engine pool first to release all V8 values and runtimes
            // 步骤 1: 首先关闭引擎池以释放所有 V8 values 和 runtimes
            if (v8EnginePoolService != null) {
                log.info("正在关闭 V8 引擎池...");
                try {
                    Method destroyMethod = v8EnginePoolService.getClass().getMethod("destroy");
                    destroyMethod.invoke(v8EnginePoolService);
                    log.info("V8 引擎池已关闭");
                } catch (Exception e) {
                    log.warn("关闭引擎池时出错: {}", e.getMessage(), e);
                }
            }
            
            // Step 2-5: Unload V8 library
            // 步骤 2-5: 卸载 V8 库
            unloadV8Library();
            
        } catch (Exception e) {
            log.warn("关闭引擎池和卸载 V8 库时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Unload V8 JNI library using reflection following Javet documentation steps.
     * 使用反射按照 Javet 文档步骤卸载 V8 JNI 库。
     * 
     * Official unload steps:
     * Step 1: Set library reloadable to true
     * Step 2: Get V8Host per JS runtime type
     * Step 3: Unload the library
     * Step 4: Restore the switch to false
     */
    private void unloadV8Library() {
        try {
            // Check if Javet classes are available
            // 检查 Javet 类是否可用
            Class<?> v8HostClass = Class.forName("com.caoccao.javet.interop.V8Host");
            Class<?> jsRuntimeTypeClass = Class.forName("com.caoccao.javet.interop.JSRuntimeType");
            
            log.info("开始卸载 V8 JNI 库...");
            
            // Step 1: Set library reloadable to true
            // 步骤 1: 设置库为可重新加载
            Method setLibraryReloadableMethod = v8HostClass.getMethod("setLibraryReloadable", boolean.class);
            setLibraryReloadableMethod.invoke(null, true);
            log.debug("Step 1: V8Host.setLibraryReloadable(true) 完成");
            
            // Step 2: Get V8Host instance for V8 runtime type
            // 步骤 2: 获取 V8 运行时类型的 V8Host 实例
            Object jsRuntimeType = jsRuntimeTypeClass.getField("V8").get(null);
            Method getInstanceMethod = v8HostClass.getMethod("getInstance", jsRuntimeTypeClass);
            Object v8Host = getInstanceMethod.invoke(null, jsRuntimeType);
            log.debug("Step 2: V8Host 实例获取完成");
            
            // Step 3: Unload the library
            // 步骤 3: 卸载库
            Method unloadLibraryMethod = v8HostClass.getMethod("unloadLibrary");
            unloadLibraryMethod.invoke(v8Host);
            log.debug("Step 3: unloadLibrary() 调用完成");
            
            // Step 4: Restore the switch to false
            // 步骤 4: 恢复开关为 false
            setLibraryReloadableMethod.invoke(null, false);
            log.debug("Step 4: V8Host.setLibraryReloadable(false) 完成");
            
            // Trigger GC to help release resources
            // 触发 GC 帮助释放资源
            System.gc();
            
            log.info("V8 JNI 库卸载完成（实际卸载将在 GC 回收所有引用后生效）");
            
        } catch (ClassNotFoundException e) {
            // Javet classes not found - expected in lite version
            // Javet 类未找到 - lite 版本中的预期行为
            log.debug("Javet 类未找到，跳过 V8 库卸载（可能是 lite 版本）");
        } catch (Exception e) {
            // Log error but don't throw - allow plugin to stop gracefully
            // 记录错误但不抛出 - 允许插件正常停止
            log.warn("卸载 V8 JNI 库时出错: {}", e.getMessage(), e);
        }
    }
}
