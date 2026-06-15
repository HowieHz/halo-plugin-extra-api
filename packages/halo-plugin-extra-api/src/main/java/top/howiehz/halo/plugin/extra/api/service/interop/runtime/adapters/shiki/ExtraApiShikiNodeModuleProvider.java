package top.howiehz.halo.plugin.extra.api.service.interop.runtime.adapters.shiki;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.node.runtime.api.NodeModuleDescriptor;
import top.howiehz.halo.plugin.node.runtime.api.NodeModuleProvider;

@Component
@RequiredArgsConstructor
public class ExtraApiShikiNodeModuleProvider implements NodeModuleProvider {

    private final PluginContext pluginContext;

    @Override
    public PluginContext pluginContext() {
        return pluginContext;
    }

    @Override
    public List<NodeModuleDescriptor> modules() {
        return List.of(NodeModuleDescriptor.fromResource(getClass(), "shiki", "js/shiki.umd.cjs"));
    }
}
