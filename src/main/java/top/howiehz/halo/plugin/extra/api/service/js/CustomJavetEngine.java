package top.howiehz.halo.plugin.extra.api.service.js;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.logging.JavetStandardConsoleInterceptor;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngine;
import java.nio.charset.StandardCharsets;
import top.howiehz.halo.plugin.extra.api.service.js.module.CustomV8ModuleResolver;

public class CustomJavetEngine extends JavetEngine<V8Runtime> {

    private JavetStandardConsoleInterceptor consoleInterceptor;

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
                getConfig().getJavetLogger().logError("Error handling promise rejection: " + e.getMessage());
            }
        });

        // 预加载 Shiki 模块
        preloadModules();
    }

    private void preloadModules() throws JavetException {
        System.out.println("=== 开始预加载 Shiki 模块 ===");
        try {
            // 检查资源文件是否存在
            try (var inputStream = getClass().getClassLoader().getResourceAsStream("js/shiki.umd.cjs")) {
                if (inputStream == null) {
                    System.err.println("ERROR: 找不到资源文件 js/shiki.umd.cjs");
                    return;
                }

                String shikiCode = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Shiki 代码已读取，长度: " + shikiCode.length());

                if (shikiCode.length() == 0) {
                    System.err.println("ERROR: Shiki 文件为空");
                    return;
                }

                // 执行代码
                System.out.println("开始执行 Shiki 代码...");
                v8Runtime.getExecutor(shikiCode).executeVoid();
                System.out.println("Shiki 代码执行完成");

                // 立即验证
                try {
                    boolean highlightExists = v8Runtime.getExecutor("typeof highlightCode === 'function'").executeBoolean();
                    boolean languagesExists = v8Runtime.getExecutor("typeof getSupportedLanguages === 'function'").executeBoolean();

                    System.out.println("验证结果 - highlightCode: " + highlightExists + ", getSupportedLanguages: " + languagesExists);

                    if (highlightExists && languagesExists) {
                        System.out.println("✅ Shiki 模块预加载成功!");
                    } else {
                        System.err.println("❌ Shiki 函数未正确暴露");

                        // 检查全局对象
                        String globals = v8Runtime.getExecutor("Object.getOwnPropertyNames(globalThis).filter(name => name.includes('highlight') || name.includes('Language') || name.includes('Theme')).join(', ')").executeString();
                        System.out.println("全局对象中相关属性: " + globals);
                    }
                } catch (Exception e) {
                    System.err.println("验证时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("预加载 Shiki 失败: " + e.getMessage());
            e.printStackTrace();
            // 不要抛出异常，让引擎继续初始化
        }
        System.out.println("=== 预加载过程结束 ===");
    }

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