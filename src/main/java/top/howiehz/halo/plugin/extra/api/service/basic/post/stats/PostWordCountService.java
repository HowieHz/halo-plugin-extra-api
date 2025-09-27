package top.howiehz.halo.plugin.extra.api.service.basic.post.stats;

import java.math.BigInteger;
import reactor.core.publisher.Mono;

/**
 * Service interface for post word count statistics and caching.
 * 文章字数统计和缓存服务接口。
 */
public interface PostWordCountService {

    /**
     * Get the word count of a specific post.
     * 获取指定文章的字数。
     *
     * @param postName the name of the post / 文章名称
     * @param isDraft whether to get draft content (true) or release content (false) / 是否获取草稿内容（true）或发布内容（false）
     * @return Mono containing the word count / 包含字数的 Mono
     */
    Mono<BigInteger> getPostWordCount(String postName, boolean isDraft);

    /**
     * Get the total word count of all posts.
     * 获取所有文章的总字数。
     *
     * @param isDraft whether to count draft content (true) or release content (false) / 是否统计草稿内容（true）或发布内容（false）
     * @return Mono containing the total word count / 包含总字数的 Mono
     */
    Mono<BigInteger> getTotalPostWordCount(boolean isDraft);

    /**
     * Warm up all caches by pre-calculating word counts.
     * 预热所有缓存，通过预先计算字数。
     */
    void warmUpAllCache();

    /**
     * Refresh the word count cache for a specific post.
     * 刷新指定文章的字数缓存。
     *
     * @param postName the name of the post / 文章名称
     * @param isDraft whether to refresh draft cache (true) or release cache (false) / 是否刷新草稿缓存（true）或发布缓存（false）
     * @return Mono containing the refreshed word count / 包含刷新后字数的 Mono
     */
    Mono<BigInteger> refreshPostCountCache(String postName, boolean isDraft);

    /**
     * Refresh word count caches for all posts and update the total.
     * 刷新所有文章的字数缓存并更新总数。
     *
     * @param isDraft whether to refresh draft caches (true) or release caches (false) / 是否刷新草稿缓存（true）或发布缓存（false）
     * @return Mono containing the total word count after refresh / 包含刷新后总字数的 Mono
     */
    Mono<BigInteger> refreshAllPostCountCache(boolean isDraft);

    /**
     * Refresh the total word count by recalculating from cached post data.
     * 通过重新计算缓存的文章数据来刷新总字数。
     *
     * @param isDraft whether to refresh draft total (true) or release total (false) / 是否刷新草稿总数（true）或发布总数（false）
     * @return Mono containing the refreshed total word count / 包含刷新后总字数的 Mono
     */
    Mono<BigInteger> refreshTotalPostCountFromCache(boolean isDraft);
}
