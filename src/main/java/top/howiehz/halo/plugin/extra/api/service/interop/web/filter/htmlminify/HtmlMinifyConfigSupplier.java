package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.ReactiveSettingFetcher;
import top.howiehz.halo.plugin.extra.api.service.core.config.AbstractPluginConfigSupplier;

/**
 * Supplier for HTML minify configuration that fetches settings reactively.
 * HTML 页面压缩配置的供应器，以响应式方式获取设置。
 */
@Component
public class HtmlMinifyConfigSupplier extends AbstractPluginConfigSupplier<HtmlMinifyConfig> {

    public HtmlMinifyConfigSupplier(ReactiveSettingFetcher fetcher) {
        super(fetcher);
    }

    @Override
    protected String configKey() {
        return "htmlMinify";
    }

    @Override
    protected Class<HtmlMinifyConfig> configType() {
        return HtmlMinifyConfig.class;
    }

    @Override
    protected HtmlMinifyConfig defaultConfig() {
        return new HtmlMinifyConfig();
    }

    @Override
    protected HtmlMinifyConfig normalizeConfig(HtmlMinifyConfig config) {
        if (config == null) {
            return new HtmlMinifyConfig();
        }
        if (config.getExcludePaths() == null) {
            config.setExcludePaths(new HtmlMinifyConfig().getExcludePaths());
        }
        return config;
    }
}
