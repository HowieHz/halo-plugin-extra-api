package top.howiehz.halo.plugin.extra.api.service.basic.post.stats;

import org.springframework.context.event.EventListener;
import run.halo.app.event.post.PostUpdatedEvent;

public interface PostStatsDataCacheSentry {
    @EventListener
    void onPostUpdated(PostUpdatedEvent event);
}
