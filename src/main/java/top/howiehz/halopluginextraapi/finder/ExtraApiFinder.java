package top.howiehz.halopluginextraapi.finder;

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

    /**
     * Unified post list query with optional pagination, filters, and sorting.
     * Accepts parameters: page(int, 1-based), size(int), tagName(String),
     * categoryName(String), ownerName(String), sort(String[] in the form of "field,direction").
     * Delegates to Halo built-in postFinder when available, with a safe fallback.
     *
     * @param params query parameters map as described above
     * @return Mono of a page-like result that can be iterated in templates
     */
    Mono<?> list(java.util.Map<String, Object> params);
}
