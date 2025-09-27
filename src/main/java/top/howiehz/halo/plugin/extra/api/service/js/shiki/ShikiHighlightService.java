package top.howiehz.halo.plugin.extra.api.service.js.shiki;

import com.caoccao.javet.exceptions.JavetException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Shiki 代码高亮服务接口
 */
public interface ShikiHighlightService {

    /**
     * 代码高亮（同步）
     */
    String highlightCode(String code, String language, String theme) throws JavetException;

    /**
     * 代码高亮（异步）
     */
    CompletableFuture<String> highlightCodeAsync(String code, String language, String theme);

    /**
     * 批量代码高亮
     */
    Map<String, String> highlightCodeBatch(Map<String, CodeHighlightRequest> requests);

    /**
     * 获取支持的语言列表
     */
    String[] getSupportedLanguages() throws JavetException;

    /**
     * 获取支持的主题列表
     */
    String[] getSupportedThemes() throws JavetException;

    /**
     * 代码高亮请求记录
     */
    record CodeHighlightRequest(String code, String language, String theme) {}
}