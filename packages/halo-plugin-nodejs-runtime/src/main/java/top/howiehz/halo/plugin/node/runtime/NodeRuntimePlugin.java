package top.howiehz.halo.plugin.node.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Slf4j
@Component
public class NodeRuntimePlugin extends BasePlugin {

    static {
        System.setProperty("javet.lib.loading.suppress.error", "true");
    }

    public NodeRuntimePlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        log.info("Node.js runtime plugin started");
    }

    @Override
    public void stop() {
        log.info("Node.js runtime plugin stopped");
    }
}
