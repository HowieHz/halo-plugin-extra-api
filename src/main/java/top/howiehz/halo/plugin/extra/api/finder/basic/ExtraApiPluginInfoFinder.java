package top.howiehz.halo.plugin.extra.api.finder.basic;

import reactor.core.publisher.Mono;

/**
 * Plugin Version Finder - 插件版本信息查询器
 * 为什么需要：让主题和其他代码能够检测当前插件版本类型，以便有条件地使用高级功能
 */
public interface ExtraApiPluginInfoFinder {

    /**
     * 检查当前插件是否为完整版
     * 完整版包含 JavaScript 功能，轻量版则不包含
     *
     * @return true 如果是完整版，false 如果是轻量版
     */
    Mono<Boolean> isFullVersion();

    /**
     * 检查当前插件是否为轻量版
     * 轻量版不包含 JavaScript 功能，但仍提供基础 API
     *
     * @return true 如果是轻量版，false 如果是完整版
     */
    Mono<Boolean> isLiteVersion();

    /**
     * 获取当前插件版本类型的字符串描述
     *
     * @return "full" 或 "lite"
     */
    Mono<String> getVersionType();

    /**
     * 检查 JavaScript 功能是否可用
     * 这是检查完整版的另一种方式，通过实际检测 JS 服务是否存在
     *
     * @return true 如果 JavaScript 功能可用
     */
    Mono<Boolean> isJavaScriptAvailable();
}
