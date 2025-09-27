package top.howiehz.halo.plugin.extra.api.service.js.module;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;

public enum JsModule {
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

    public static JsModule of(String moduleName) {
        return moduleMap.get(moduleName);
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public JsModuleType getType() {
        return type;
    }

    public String getSourceCode() throws IOException {
        String resourcePath = "js/" + fileName;
        return IOUtils.resourceToString(resourcePath, StandardCharsets.UTF_8,
            JsModule.class.getClassLoader());
    }
}