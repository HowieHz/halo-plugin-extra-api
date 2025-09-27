package top.howiehz.halo.plugin.extra.api.service.basic.post.stats;

import org.springframework.context.event.EventListener;
import run.halo.app.event.post.PostUpdatedEvent;

/**
 * Cache sentry that listens for post update events and invalidates related caches.
 * 缓存哨兵，监听文章更新事件并使相关缓存失效。
 */
public interface PostStatsDataCacheSentry {

    /**
     * Handle post update events by clearing affected caches.
     * 处理文章更新事件，通过清除受影响的缓存。
     *
     * @param event the post update event / 文章更新事件
     */
    @EventListener
    void onPostUpdated(PostUpdatedEvent event);
}
