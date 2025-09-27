package top.howiehz.halo.plugin.extra.api.service.js.engine;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.logging.JavetStandardConsoleInterceptor;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngine;
import com.google.common.base.Throwables;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import top.howiehz.halo.plugin.extra.api.service.js.module.CustomV8ModuleResolver;

/**
 * Custom Javet engine that registers console interceptor, module resolver and preloads modules.
 * 自定义 Javet 引擎，注册控制台拦截、模块解析器并预加载需要的 JS 模块（如 Shiki）。
 */
@Slf4j
public class CustomJavetEngine extends JavetEngine<V8Runtime> {

    private JavetStandardConsoleInterceptor consoleInterceptor;

    /**
     * Constructor that initializes interceptors, resolver and preloads modules.
     * 构造函数：初始化拦截器、模块解析器并触发预加载逻辑。
     *
     * @param iJavetEnginePool engine pool reference / 引擎池引用
     * @param v8Runtime the V8 runtime instance / V8 运行时实例
     * @throws JavetException when initialization fails / 初始化失败抛出
     */
    public CustomJavetEngine(IJavetEnginePool<V8Runtime> iJavetEnginePool, V8Runtime v8Runtime)
        throws JavetException {
        super(iJavetEnginePool, v8Runtime);

        // 注册控制台拦截器
        consoleInterceptor = new JavetStandardConsoleInterceptor(v8Runtime);
        consoleInterceptor.register(v8Runtime.getGlobalObject());

        // 设置自定义模块解析器
        v8Runtime.setV8ModuleResolver(new CustomV8ModuleResolver());

        // 设置 Promise 拒绝回调
        v8Runtime.setPromiseRejectCallback((event, promise, value) -> {
            try {
                String errorMessage = value.toString();
                getConfig().getJavetLogger().logError("Promise rejected: " + errorMessage);
            } catch (Exception e) {
                getConfig().getJavetLogger()
                    .logError("Error handling promise rejection: " + e.getMessage());
            }
        });

        // 预加载 Shiki 模块
        preloadModules();
    }

    /**
     * Preload embedded modules like Shiki into the runtime to avoid runtime latency on first use.
     * 预加载嵌入的模块（例如 Shiki），避免首次使用时的延迟。
     * <p>
     * This method logs diagnostic information but intentionally swallows errors to avoid failing
     * engine creation.
     * 该方法会记录诊断信息，但为了不阻塞引擎创建会捕获并忽略异常。
     *
     * @throws JavetException if underlying JS operations fail / 底层 JS 操作失败时抛出（实践中通常被捕获）
     */
    private void preloadModules() throws JavetException {
        log.debug("开始预加载 Shiki 模块");
        try {
            // 检查资源文件是否存在
            try (var inputStream = getClass().getClassLoader()
                .getResourceAsStream("js/shiki.umd.cjs")) {
                if (inputStream == null) {
                    log.error("找不到资源文件 js/shiki.umd.cjs");
                    return;
                }

                String shikiCode = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                log.debug("Shiki 代码已读取，长度: {}", shikiCode.length());

                if (shikiCode.isEmpty()) {
                    log.error("Shiki 文件为空");
                    return;
                }

                // 执行代码
                log.debug("开始执行 Shiki 代码...");
                v8Runtime.getExecutor(shikiCode).executeVoid();
                log.debug("Shiki 代码执行完成");

                // 立即验证
                try {
                    boolean highlightExists =
                        v8Runtime.getExecutor("typeof highlightCode === 'function'")
                            .executeBoolean();
                    boolean languagesExists =
                        v8Runtime.getExecutor("typeof getSupportedLanguages === 'function'")
                            .executeBoolean();

                    log.debug("验证结果 - highlightCode: {}, getSupportedLanguages: {}",
                        highlightExists, languagesExists);

                    if (highlightExists && languagesExists) {
                        log.info("✅ Shiki 模块预加载成功!");
                    } else {
                        log.error("❌ Shiki 函数未正确暴露");

                        // 检查全局对象
                        String globals = v8Runtime.getExecutor(
                                "Object.getOwnPropertyNames(globalThis).filter(name => name"
                                    + ".includes('highlight') || name.includes('Language') || "
                                    + "name.includes('Theme')).join(', ')")
                            .executeString();
                        log.info("全局对象中相关属性: {}", globals);
                    }
                } catch (Exception e) {
                    log.error("验证时出错:", Throwables.getRootCause(e));
                }
            }

        } catch (Exception e) {
            log.error("预加载失败:", Throwables.getRootCause(e));
            // 不要抛出异常，让引擎继续初始化
        }
        log.debug("=== 预加载过程结束 ===");
    }

    /**
     * Close the engine and cleanup resources; will unregister interceptors on forced close.
     * 关闭引擎并清理资源；在强制关闭时会注销拦截器。
     *
     * @param forceClose whether to force close / 是否强制关闭
     * @throws JavetException when close operations fail / 关闭操作失败时抛出
     */
    @Override
    protected void close(boolean forceClose) throws JavetException {
        if (forceClose && consoleInterceptor != null) {
            // 注销控制台拦截器
            consoleInterceptor.unregister(v8Runtime.getGlobalObject());
            // 释放内存
            v8Runtime.lowMemoryNotification();
            consoleInterceptor = null;
        }
        super.close(forceClose);
    }
}