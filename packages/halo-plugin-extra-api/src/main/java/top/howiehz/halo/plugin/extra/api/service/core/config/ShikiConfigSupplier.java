package top.howiehz.halo.plugin.extra.api.service.core.config;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Supplier for Shiki configuration that fetches settings reactively.
 * Shiki 配置的供应器，以响应式方式获取设置。
 */
@Component
public class ShikiConfigSupplier extends AbstractPluginConfigSupplier<ShikiConfig> {

    public ShikiConfigSupplier(ReactiveSettingFetcher fetcher) {
        super(fetcher);
    }

    @Override
    protected String configKey() {
        return "shiki";
    }

    @Override
    protected Class<ShikiConfig> configType() {
        return ShikiConfig.class;
    }

    @Override
    protected ShikiConfig fallbackConfig() {
        return new ShikiConfig();
    }
}
