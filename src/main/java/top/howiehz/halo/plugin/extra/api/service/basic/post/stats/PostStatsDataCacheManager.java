package top.howiehz.halo.plugin.extra.api.service.basic.post.stats;

import java.math.BigInteger;
import org.springframework.lang.Nullable;

/**
 * Interface for managing post statistics data caching.
 * 文章统计数据缓存管理接口。
 */
public interface PostStatsDataCacheManager {

    /**
     * Get the cached total word count for all posts.
     * 获取缓存的所有文章总字数。
     *
     * @param isDraft whether to get draft total (true) or release total (false) / 是否获取草稿总数（true）或发布总数（false）
     * @return the cached total word count, or null if not cached / 缓存的总字数，如果未缓存则返回 null
     */
    @Nullable
    BigInteger getCachedTotalWordCount(boolean isDraft);

    /**
     * Get the cached word count for a specific post.
     * 获取指定文章的缓存字数。
     *
     * @param postName the name of the post / 文章名称
     * @param isDraft whether to get draft count (true) or release count (false) / 是否获取草稿字数（true）或发布字数（false）
     * @return the cached word count, or null if not cached / 缓存的字数，如果未缓存则返回 null
     */
    @Nullable
    BigInteger getCachedPostWordCount(String postName, boolean isDraft);

    /**
     * Set the total word count in cache.
     * 在缓存中设置总字数。
     *
     * @param count the total word count to cache / 要缓存的总字数
     * @param isDraft whether this is draft total (true) or release total (false) / 是否为草稿总数（true）或发布总数（false）
     */
    void setTotalPostWordCount(BigInteger count, boolean isDraft);

    /**
     * Set the word count for a specific post in cache.
     * 在缓存中设置指定文章的字数。
     *
     * @param postName the name of the post / 文章名称
     * @param count the word count to cache / 要缓存的字数
     * @param isDraft whether this is draft count (true) or release count (false) / 是否为草稿字数（true）或发布字数（false）
     */
    void setPostWordCount(String postName, BigInteger count, boolean isDraft);

    /**
     * Clear the total word count cache.
     * 清除总字数缓存。
     *
     * @param isDraft whether to clear draft total cache (true) or release total cache (false) / 是否清除草稿总数缓存（true）或发布总数缓存（false）
     */
    void clearTotalPostWordCountCache(boolean isDraft);

    /**
     * Clear the word count cache for a specific post.
     * 清除指定文章的字数缓存。
     *
     * @param postName the name of the post / 文章名称
     * @param isDraft whether to clear draft cache (true) or release cache (false) / 是否清除草稿缓存（true）或发布缓存（false）
     */
    void clearPostWordCountCache(String postName, boolean isDraft);
}
