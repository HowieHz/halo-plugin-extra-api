package top.howiehz.halo.plugin.node.runtime.api;

import java.util.Set;
import java.util.regex.Pattern;

public final class NodeRuntimeValidator {

    private static final Pattern MODULE_ID_PATTERN =
        Pattern.compile("^(?!/)(?!.*//)(?!.*/$)[A-Za-z0-9._/-]+$");
    private static final Pattern PLUGIN_NAME_PATTERN =
        Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");
    private static final Pattern FUNCTION_NAME_PATTERN =
        Pattern.compile("^[\\p{L}_$][\\p{L}\\p{N}_$]*$");
    private static final Set<String> RESERVED_WORDS = Set.of(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function", "if",
        "import", "in", "instanceof", "new", "return", "super", "switch", "this", "throw",
        "try", "typeof", "var", "void", "while", "with", "yield", "let", "static", "enum",
        "await", "implements", "package", "protected", "interface", "private", "public"
    );

    private NodeRuntimeValidator() {
    }

    public static void requireModuleId(String moduleId) {
        if (moduleId == null || !MODULE_ID_PATTERN.matcher(moduleId).matches()) {
            throw new IllegalArgumentException("Invalid moduleId: " + moduleId);
        }
    }

    public static void requirePluginName(String pluginName) {
        if (pluginName == null || !PLUGIN_NAME_PATTERN.matcher(pluginName).matches()) {
            throw new IllegalArgumentException("Invalid pluginName: " + pluginName);
        }
    }

    public static void requirePluginVersion(String pluginVersion) {
        if (pluginVersion == null || pluginVersion.isBlank()
            || pluginVersion.contains(" ") || pluginVersion.contains("/")
            || pluginVersion.contains("@")) {
            throw new IllegalArgumentException("Invalid pluginVersion: " + pluginVersion);
        }
    }

    public static void requireFunctionName(String functionName) {
        if (functionName == null || !FUNCTION_NAME_PATTERN.matcher(functionName).matches()
            || RESERVED_WORDS.contains(functionName)) {
            throw new IllegalArgumentException("Invalid functionName: " + functionName);
        }
    }
}
