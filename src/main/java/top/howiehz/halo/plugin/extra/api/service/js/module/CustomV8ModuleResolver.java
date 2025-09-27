package top.howiehz.halo.plugin.extra.api.service.js.module;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.IV8ModuleResolver;
import com.caoccao.javet.values.reference.IV8Module;
import java.io.IOException;

/**
 * Custom V8 module resolver that loads modules from embedded resources.
 * 自定义 V8 模块解析器，从资源加载并按模块类型处理（UMD/ESM/CJS）。
 */
public class CustomV8ModuleResolver implements IV8ModuleResolver {

    private static final String GLOBAL_THIS = "globalThis";

    /**
     * Resolve a module by resource name and return a compiled IV8Module for ESM/CJS as needed.
     * 根据资源名解析模块，对于 ESM 会返回编译后的 IV8Module；UMD 直接执行；CJS 将模拟 module.exports。
     *
     * @param v8Runtime v8 runtime / V8 运行时
     * @param resourceName module resource name / 模块资源名称
     * @param v8ModuleReferrer optional referrer module / 可选的引用者模块
     * @return IV8Module when module is ESM/CJS and a module object can be produced, otherwise
     * null / ESM/CJS 返回模块实例，UMD 返回 null
     * @throws JavetException when JS execution or compilation fails / JS 执行或编译失败时抛出
     */
    @Override
    public IV8Module resolve(V8Runtime v8Runtime, String resourceName, IV8Module v8ModuleReferrer)
        throws JavetException {

        JsModule module = JsModule.of(resourceName);
        if (module != null) {
            try {
                String sourceCode = module.getSourceCode();

                switch (module.getType()) {
                    case UMD -> {
                        // UMD 模块直接执行，通常会在 globalThis 上注册函数
                        v8Runtime.getExecutor(sourceCode).executeVoid();
                        // UMD 模块不需要返回 IV8Module，因为它们直接在全局作用域注册
                        return null;
                    }
                    case ESM -> {
                        // ESM 模块需要编译为 V8Module
                        return v8Runtime.getExecutor(sourceCode)
                            .setResourceName(module.getModuleName())
                            .setModule(true)
                            .compileV8Module();
                    }
                    case CJS -> {
                        // CommonJS 模块处理
                        var globalObject = v8Runtime.getGlobalObject();
                        try (var moduleObject = v8Runtime.createV8ValueObject()) {
                            // 创建 module.exports 对象
                            try (var exportsObject = v8Runtime.createV8ValueObject()) {
                                moduleObject.set("exports", exportsObject);
                                globalObject.set("module", moduleObject);
                                globalObject.set("exports", exportsObject);

                                // 执行 CommonJS 代码
                                v8Runtime.getExecutor(sourceCode).executeVoid();

                                // 返回合成模块
                                return v8Runtime.createV8Module(module.getModuleName(),
                                    exportsObject);
                            }
                        } finally {
                            // 清理全局变量
                            globalObject.delete("module");
                            globalObject.delete("exports");
                        }
                    }
                    default -> {
                        v8Runtime.getLogger()
                            .logWarn("Unknown module type: " + module.getType().name());
                    }
                }
            } catch (IOException e) {
                v8Runtime.getLogger()
                    .logError(e, "Failed to load module: " + module.getModuleName());
            }
        }

        return null;
    }
}