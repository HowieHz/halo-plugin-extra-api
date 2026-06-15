package top.howiehz.halo.plugin.node.runtime.api;

import java.util.List;
import org.pf4j.ExtensionPoint;
import run.halo.app.plugin.PluginContext;

public interface NodeModuleProvider extends ExtensionPoint {

    PluginContext pluginContext();

    List<NodeModuleDescriptor> modules();
}
