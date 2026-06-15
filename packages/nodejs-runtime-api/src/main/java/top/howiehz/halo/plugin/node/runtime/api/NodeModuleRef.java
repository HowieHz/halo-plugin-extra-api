package top.howiehz.halo.plugin.node.runtime.api;

import run.halo.app.plugin.PluginContext;

public record NodeModuleRef(
    String pluginName,
    String pluginVersion,
    String moduleId
) {

    public static NodeModuleRef of(PluginContext pluginContext, String moduleId) {
        return new NodeModuleRef(pluginContext.getName(), pluginContext.getVersion(), moduleId);
    }

    public String fullId() {
        NodeRuntimeValidator.requirePluginName(pluginName);
        NodeRuntimeValidator.requirePluginVersion(pluginVersion);
        NodeRuntimeValidator.requireModuleId(moduleId);
        return "@" + pluginName + "/" + moduleId + "@" + pluginVersion;
    }
}
