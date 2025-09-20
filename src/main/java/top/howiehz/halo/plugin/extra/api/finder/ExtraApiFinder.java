package top.howiehz.halo.plugin.extra.api.finder;

import reactor.core.publisher.Mono;
import java.util.Collections;

/**
 * Finder for calculating post word/character counts for themes to use.
 */
public interface ExtraApiFinder {
    /**
     * Unified word count API.
     * Parameters (all optional):
     * - name: metadata.name of the post
     * - version: 'release' or 'draft' (default 'release')
     * If name is provided, count the specific post; otherwise, count all posts.
     *
     * @param params parameter map from templates
     * @return word count as Mono (non-negative)
     */
    Mono<Integer> wordCount(java.util.Map<String, Object> params);

    /**
     * Get total word count of all published posts.
     * 获取所有已发布文章的总字数。
     *
     * @return word count as Mono (non-negative)
     */
    default Mono<Integer> wordCount(){
        return wordCount(Collections.emptyMap());
    };
}
