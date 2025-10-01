package top.howiehz.halo.plugin.extra.api.finder.basic;

import java.math.BigInteger;
import java.util.Collections;
import reactor.core.publisher.Mono;

/**
 * Finder for calculating post word/character counts for themes to use.
 * 供主题使用的文章字数/字符数统计 Finder。
 */
public interface ExtraApiStatsFinder {
    /**
     * Unified word count API.
     * 统一的字数统计接口。
     * <p>
     * Parameters (all optional):
     * 参数（均为可选）：
     * - name: metadata.name of the post / 文章的 metadata.name
     * - version: 'release' or 'draft' (default 'release') / 版本：发布版或草稿（默认发布版）
     * If name is provided, count the specific post; otherwise, count all posts.
     * 若提供 name 参数，则统计对应文章；否则统计所有文章的总字数。
     * </p>
     *
     * @param params parameter map from templates / 来自模板的参数映射
     * @return word count as Mono (non-negative) / 返回字数（非负）的 Mono
     */
    Mono<BigInteger> getPostWordCount(java.util.Map<String, Object> params);

    /**
     * Get total word count of all published posts.
     * 获取所有已发布文章的总字数。
     *
     * @return word count as Mono (non-negative) / 返回字数（非负）的 Mono
     */
    default Mono<BigInteger> getPostWordCount() {
        return getPostWordCount(Collections.emptyMap());
    }

    /**
     * Count words in the provided HTML content.
     * 统计提供的 HTML 内容的字数。
     * <p>
     * This method accepts raw HTML content and returns the word count.
     * CJK characters are counted individually, ASCII letters/digits are grouped as words.
     * 此方法接收原始 HTML 内容并返回字数统计。
     * 中日韩字符单独计数，ASCII 字母/数字按单词分组计数。
     * </p>
     *
     * @param params parameter map containing 'htmlContent' key / 包含 'htmlContent' 键的参数映射
     * @return word count as Mono (non-negative) / 返回字数（非负）的 Mono
     */
    Mono<BigInteger> getContentWordCount(java.util.Map<String, Object> params);

    /**
     * Count words in the provided HTML content.
     * 统计提供的 HTML 内容的字数。
     *
     * @param htmlContent the HTML content to count / 要统计的 HTML 内容
     * @return word count as Mono (non-negative) / 返回字数（非负）的 Mono
     */
    default Mono<BigInteger> getContentWordCount(String htmlContent) {
        return getContentWordCount(Collections.singletonMap("htmlContent", htmlContent));
    }
}
