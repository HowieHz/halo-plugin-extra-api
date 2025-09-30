package top.howiehz.halo.plugin.extra.api.service.js.runtime.adapters.shiki;

import com.caoccao.javet.exceptions.JavetException;
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
     * @throws JavetException when JS execution fails / JS 执行失败时抛出
     */
    String highlightCode(String code, String language, String theme) throws JavetException;

    /**
     * Batch highlight multiple code requests in a single engine.
     * 在单个引擎中批量高亮多个代码块。
     *
     * @param requests map of id -> request / id 到请求的映射
     * @return map of id -> highlighted result / id 到高亮结果的映射
     * @throws JavetException when JS execution fails / JS 执行失败时抛出
     */
    Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests)
        throws JavetException;

    /**
     * Get supported languages.
     * 获取支持的语言列表。
     *
     * @return language identifiers set / 语言标识集合
     * @throws JavetException when JS call fails / JS 调用失败时抛出
     */
    Set<String> getSupportedLanguages() throws JavetException;

    /**
     * Get supported themes.
     * 获取支持的主题列表。
     *
     * @return theme names set / 主题名集合
     * @throws JavetException when JS call fails / JS 调用失败时抛出
     */
    Set<String> getSupportedThemes() throws JavetException;

    /**
     * Request record for batch highlighting.
     * 批量高亮请求记录结构。
     */
    record CodeHighlightRequest(String code, String language, String theme) {
    }

}
