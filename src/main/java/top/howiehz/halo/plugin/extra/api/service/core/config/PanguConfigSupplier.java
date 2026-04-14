package top.howiehz.halo.plugin.extra.api.service.core.config;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for Pangu configuration that fetches settings reactively.
 * Pangu 配置的供应器，以响应式方式获取设置。
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Component
public class PanguConfigSupplier extends AbstractPluginConfigSupplier<PanguConfig> {

    public PanguConfigSupplier(ReactiveSettingFetcher fetcher) {
        super(fetcher);
    }

    @Override
    protected String configKey() {
        return "pangu";
    }

    @Override
    protected Class<PanguConfig> configType() {
        return PanguConfig.class;
    }

    @Override
    protected PanguConfig fallbackConfig() {
        return new PanguConfig();
    }
}
