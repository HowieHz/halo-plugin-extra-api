package top.howiehz.halo.plugin.extra.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.event.post.PostUpdatedEvent;
import top.howiehz.halo.plugin.extra.api.finder.ExtraApiStatsFinderImpl;
import java.util.Map;
import java.math.BigInteger;

/**
 * Sentry for evicting and updating word count cache when posts are updated.
 * 文章更新时负责清理和更新字数缓存的哨兵。
 */
@Slf4j
@Component
public class PostStatsDataCacheSentry {
    private final PostStatsDataCacheManager postStatsDataCacheManager;
    private final ExtraApiStatsFinderImpl extraApiStatsFinderImpl;

    public PostStatsDataCacheSentry(PostStatsDataCacheManager postStatsDataCacheManager,
        ExtraApiStatsFinderImpl extraApiStatsFinderImpl) {
        this.postStatsDataCacheManager = postStatsDataCacheManager;
        this.extraApiStatsFinderImpl = extraApiStatsFinderImpl;
    }

    /**
     * Handle post updated event and refresh caches asynchronously for release and draft.
     * 处理文章更新事件，并异步刷新发布版与草稿版的缓存。
     */
    @EventListener
    void onPostUpdated(PostUpdatedEvent event) {
        String postName = event.getName();
        // Update release cache and invalidate total
        // 异步更新发布版本缓存并使总数缓存失效
        extraApiStatsFinderImpl.postWordCount(Map.of("name", postName, "version", "release"))
            .doOnNext(count -> {
                postStatsDataCacheManager.setPostWordCount(postName, count, false);
                postStatsDataCacheManager.clearTotalPostWordCountCache(false);
                log.debug("Updated release word count cache for post {}: {}", postName, count);
            }).subscribe();

        // Update draft cache and invalidate total
        // 异步更新草稿版本缓存并使总数缓存失效
        extraApiStatsFinderImpl.postWordCount(Map.of("name", postName, "version", "draft"))
            .doOnNext(count -> {
                postStatsDataCacheManager.setPostWordCount(postName, count, true);
                postStatsDataCacheManager.clearTotalPostWordCountCache(true);
                log.debug("Updated draft word count cache for post {}: {}", postName, count);
            }).subscribe();

        log.info("Received post updated event, and evicted page cache");
    }
}
