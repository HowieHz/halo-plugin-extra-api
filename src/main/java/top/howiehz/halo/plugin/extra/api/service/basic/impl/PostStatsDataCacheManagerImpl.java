package top.howiehz.halo.plugin.extra.api.service.basic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import top.howiehz.halo.plugin.extra.api.service.basic.PostStatsDataCacheManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigInteger;

/**
 * Stats Data Manager - 统计数据管理器。
 * 分别缓存发布版本和草稿版本的字数统计。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostStatsDataCacheManagerImpl implements PostStatsDataCacheManager {

    // 文章发布版本字数统计缓存 / Total & per-post caches for release version
    private final AtomicReference<BigInteger> totalReleasePostWordCount =
        new AtomicReference<>(null);
    final ConcurrentHashMap<String, BigInteger> releasePostWordCounts = new ConcurrentHashMap<>();
    // 文章草稿版本字数统计缓存 / Total & per-post caches for draft version
    private final AtomicReference<BigInteger> totalDraftPostWordCount = new AtomicReference<>(null);
    final ConcurrentHashMap<String, BigInteger> draftPostWordCounts = new ConcurrentHashMap<>();

    /**
     * 获取缓存的总字数。
     * Get cached total word count.
     *
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     * @return 总字数，未缓存时返回 null / Total word count, returns null if not cached
     */
    @Nullable
    @Override
    public BigInteger getCachedTotalWordCount(boolean isDraft) {
        return isDraft ? totalDraftPostWordCount.get() : totalReleasePostWordCount.get();
    }

    /**
     * 获取缓存的文章字数。
     * Get cached word count for a specific post.
     *
     * @param postName 文章名称 / Post name
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     * @return 文章字数，未缓存时返回 null / Post word count, returns null if not cached
     */
    @Nullable
    @Override
    public BigInteger getCachedPostWordCount(String postName, boolean isDraft) {
        return isDraft ? draftPostWordCounts.get(postName) : releasePostWordCounts.get(postName);
    }

    /**
     * 设置/更新总字数缓存。
     * Set/Update total word count cache.
     *
     * @param count 总字数 / Total word count
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     */
    @Override
    public void setTotalPostWordCount(BigInteger count, boolean isDraft) {
        if (isDraft) {
            totalDraftPostWordCount.set(count);
            log.debug("Set total DRAFT word count cache: {}", count);
        } else {
            totalReleasePostWordCount.set(count);
            log.debug("Set total RELEASE word count cache: {}", count);
        }
    }

    /**
     * 设置/更新特定文章的字数缓存。
     * Set/Update word count cache for a specific post.
     *
     * @param postName 文章名称 / Post name
     * @param count 字数 / Word count
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     */
    @Override
    public void setPostWordCount(String postName, BigInteger count, boolean isDraft) {
        if (isDraft) {
            draftPostWordCounts.put(postName, count);
            log.debug("Set DRAFT word count cache for post {}: {}", postName, count);
        } else {
            releasePostWordCounts.put(postName, count);
            log.debug("Set RELEASE word count cache for post {}: {}", postName, count);
        }
    }

    /**
     * 清除总字数缓存。
     * Clear total word count cache.
     *
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     */
    @Override
    public void clearTotalPostWordCountCache(boolean isDraft) {
        if (isDraft) {
            totalDraftPostWordCount.set(null);
            log.debug("Cleared total DRAFT word count cache");
        } else {
            totalReleasePostWordCount.set(null);
            log.debug("Cleared total RELEASE word count cache");
        }
    }

    /**
     * 清除特定文章的字数缓存。
     * Clear word count cache for a specific post.
     *
     * @param postName 文章名称 / Post name
     * @param isDraft 是否为草稿版本 / Whether it's draft version
     */
    @Override
    public void clearPostWordCountCache(String postName, boolean isDraft) {
        if (postName == null || postName.trim().isEmpty()) {
            log.warn(
                "Post name is null or empty, skipping cache clear / 文章名称为空，跳过缓存清理");
            return;
        }

        if (isDraft) {
            draftPostWordCounts.remove(postName);
            log.debug("Cleared DRAFT word count cache for post");
        } else {
            releasePostWordCounts.remove(postName);
            log.debug("Cleared RELEASE word count cache for post");
        }
    }
}