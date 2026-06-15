package top.howiehz.halo.plugin.minify.html;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Slf4j
@Component
public class MinifyHtmlPlugin extends BasePlugin {

    public MinifyHtmlPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        log.info("Minify HTML plugin started");
    }

    @Override
    public void stop() {
        log.info("Minify HTML plugin stopped");
    }
}
