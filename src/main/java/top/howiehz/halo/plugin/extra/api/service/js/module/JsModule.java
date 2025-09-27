package top.howiehz.halo.plugin.extra.api.service.js.module;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;

/**
 * Enumeration of embedded JS modules used by the runtime.
 * 内置 JS 模块枚举，包含模块名称、资源文件名与类型。
 */
public enum JsModule {
    /**
     * Shiki code highlighting library.
     * Shiki 代码高亮库。
     */
    SHIKI("shiki", "shiki.umd.cjs", JsModuleType.UMD);

    private static final Map<String, JsModule> moduleMap = Stream.of(JsModule.values())
        .collect(Collectors.toMap(JsModule::getModuleName, Function.identity()));

    private final String name;
    private final String fileName;
    private final String moduleName;
    private final JsModuleType type;

    JsModule(String name, String fileName, JsModuleType type) {
        this.name = Objects.requireNonNull(name);
        this.fileName = Objects.requireNonNull(fileName);
        this.moduleName = "js/" + name;
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Get module by module name.
     * 根据模块名称获取模块。
     *
     * @param moduleName the module name / 模块名称
     * @return the module, or null if not found / 模块，如果未找到则返回 null
     */
    public static JsModule of(String moduleName) {
        return moduleMap.get(moduleName);
    }

    /**
     * Get the simple name of the module.
     * 获取模块的简单名称。
     *
     * @return module name / 模块名称
     */
    public String getName() {
        return name;
    }

    /**
     * Get the resource file name.
     * 获取资源文件名。
     *
     * @return file name / 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the full module name with path.
     * 获取包含路径的完整模块名称。
     *
     * @return module name with path / 包含路径的模块名称
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Get the module type.
     * 获取模块类型。
     *
     * @return module type / 模块类型
     */
    public JsModuleType getType() {
        return type;
    }

    /**
     * Load module source code from resources.
     * 从资源加载模块源码（UTF-8）。
     *
     * @return source code string / 源码字符串
     * @throws IOException when resource read fails / 读取资源失败时抛出
     */
    public String getSourceCode() throws IOException {
        String resourcePath = "js/" + fileName;
        return IOUtils.resourceToString(resourcePath, StandardCharsets.UTF_8,
            JsModule.class.getClassLoader());
    }
}