package top.howiehz.halo.plugin.extra.api.service.interop.runtime.adapters.shiki;

import java.util.Map;
import java.util.Set;

/**
 * Shiki code highlight service interface.
 * Shiki 代码高亮服务接口，定义同步/异步/批量及能力查询方法。
 */
public interface ShikiHighlightService {

    /**
     * Synchronously highlight code.
     * 同步高亮代码。
     *
     * @param code source code / 源码
     * @param language language id / 语言标识
     * @param theme theme name / 主题名
     * @return highlighted result / 高亮结果
     */
    String highlightCode(String code, String language, String theme);

    /**
     * Batch highlight multiple code requests in a single runtime call.
     * 在单次运行时调用中批量高亮多个代码块。
     *
     * @param requests map of id -> request / id 到请求的映射
     * @return map of id -> highlighted result / id 到高亮结果的映射
     */
    Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests);

    /**
     * Get supported languages.
     * 获取支持的语言列表。
     *
     * @return language identifiers set / 语言标识集合
     */
    Set<String> getSupportedLanguages();

    /**
     * Get supported themes.
     * 获取支持的主题列表。
     *
     * @return theme names set / 主题名集合
     */
    Set<String> getSupportedThemes();

    /**
     * Request record for batch highlighting.
     * 批量高亮请求记录结构。
     */
    record CodeHighlightRequest(String code, String language, String theme) {
    }

}
