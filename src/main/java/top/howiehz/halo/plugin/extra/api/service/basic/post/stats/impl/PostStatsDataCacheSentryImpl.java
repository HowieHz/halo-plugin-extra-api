package top.howiehz.halo.plugin.extra.api.service.basic.post.stats.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import run.halo.app.event.post.PostUpdatedEvent;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostStatsDataCacheSentry;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;

/**
 * Sentry for evicting and updating word count cache when posts are updated.
 * 文章更新时负责清理和更新字数缓存的哨兵。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostStatsDataCacheSentryImpl implements PostStatsDataCacheSentry {
    private final PostWordCountService postWordCountService;

    /**
     * Handle post updated event and refresh caches asynchronously for release and draft.
     * 处理文章更新事件，并异步刷新发布版与草稿版的缓存。
     */
    @EventListener
    @Override
    public void onPostUpdated(PostUpdatedEvent event) {
        String postName = event.getName();
        // Update release cache and invalidate total
        // 异步更新发布版本缓存
        postWordCountService.refreshPostCountCache(postName, false)
            .subscribeOn(Schedulers.parallel())
            .then(postWordCountService.refreshTotalPostCountFromCache(false)).subscribe(v -> {
            }, e -> log.warn("Update release word count failed for {}: {}", postName,
                e.toString()));

        // Update draft cache and invalidate total
        // 异步更新草稿版本缓存
        postWordCountService.refreshPostCountCache(postName, true)
            .subscribeOn(Schedulers.parallel())
            .then(postWordCountService.refreshTotalPostCountFromCache(true)).subscribe(v -> {
            }, e -> log.warn("Update draft word count failed for {}: {}", postName, e.toString()));

        log.info("Received post updated event, and refresh page count cache");
    }
}
