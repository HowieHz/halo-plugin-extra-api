package top.howiehz.halo.plugin.extra.api.finder;

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
}
