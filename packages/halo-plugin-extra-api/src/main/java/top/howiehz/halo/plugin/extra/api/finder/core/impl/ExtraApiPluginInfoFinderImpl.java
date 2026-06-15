package top.howiehz.halo.plugin.extra.api.finder.core.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.core.ExtraApiPluginInfoFinder;

/**
 * Plugin Version Finder Implementation - 插件版本信息查询器实现
 * <p>
 * 检测逻辑：
 * 1. 通过检查 Shiki Node module provider 类是否存在来判断是否为完整版
 * 2. 轻量版构建时会排除 interop 包下的所有类，包括该 provider
 * 3. 这种方式比检查 JAR 文件名更可靠，因为运行时类路径是确定的
 */
@Slf4j
@Component
@Finder("extraApiPluginInfoFinder")
public class ExtraApiPluginInfoFinderImpl implements ExtraApiPluginInfoFinder {

    /**
     * 用于检测的关键类名 - Node module provider 只在完整版中存在
     */
    private static final String SHIKI_NODE_MODULE_PROVIDER_CLASS =
        "top.howiehz.halo.plugin.extra.api.service.interop.runtime.adapters.shiki"
            + ".ExtraApiShikiNodeModuleProvider";

    /**
     * 缓存检测结果 - 避免重复类加载检查
     */
    private volatile Boolean isFullVersionCached;

    @Override
    public Mono<Boolean> isFullVersion() {
        return Mono.fromCallable(this::detectFullVersion)
            .cache(); // 缓存结果，避免重复检测
    }

    @Override
    public Mono<Boolean> isLiteVersion() {
        return isFullVersion().map(isFull -> !isFull);
    }

    @Override
    public Mono<String> getVersionType() {
        return isFullVersion().map(isFull -> isFull ? "full" : "lite");
    }

    @Override
    public Mono<Boolean> isJavaScriptAvailable() {
        // JavaScript 功能可用性与是否为完整版直接相关
        return isFullVersion();
    }

    /**
     * 检测当前是否为完整版
     * 通过尝试加载 Shiki Node module provider 类来判断
     */
    private boolean detectFullVersion() {
        if (isFullVersionCached != null) {
            return isFullVersionCached;
        }

        try {
            Class.forName(SHIKI_NODE_MODULE_PROVIDER_CLASS);
            isFullVersionCached = true;
            log.debug("Detected full version: Shiki Node module provider class found");
            return true;
        } catch (ClassNotFoundException e) {
            isFullVersionCached = false;
            log.debug("Detected lite version: Shiki Node module provider class not found");
            return false;
        }
    }
}
