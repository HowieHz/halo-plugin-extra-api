package top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Metrics collector for Shiki cache performance.
 * Shiki 缓存性能指标收集器。
 *
 * <p>收集的指标包括:
 * <ul>
 *   <li>缓存命中/未命中次数</li>
 *   <li>渲染总耗时</li>
 *   <li>渲染请求数</li>
 *   <li>去重节省的请求数</li>
 * </ul>
 */
@Slf4j
@Component
public class ShikiCacheMetrics {

    // 使用 LongAdder 提供更好的并发性能
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final LongAdder totalRenderTimeMs = new LongAdder();
    private final LongAdder renderCount = new LongAdder();
    private final LongAdder deduplicatedRequests = new LongAdder();
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());

    /**
     * Record a cache hit.
     * 记录缓存命中。
     */
    public void recordCacheHit() {
        cacheHits.increment();
    }

    /**
     * Record a cache miss.
     * 记录缓存未命中。
     */
    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    /**
     * Record render time for a batch.
     * 记录批量渲染耗时。
     *
     * @param startTime render start time / 渲染开始时间
     */
    public void recordRenderTime(Instant startTime) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        totalRenderTimeMs.add(durationMs);
        renderCount.increment();
    }

    /**
     * Record deduplicated requests count.
     * 记录去重节省的请求数。
     *
     * @param count number of duplicates removed / 去除的重复数
     */
    public void recordDeduplication(int count) {
        deduplicatedRequests.add(count);
    }

    /**
     * Get current metrics snapshot.
     * 获取当前指标快照。
     *
     * @return metrics snapshot / 指标快照
     */
    public MetricsSnapshot getSnapshot() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;

        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;
        double missRate = total > 0 ? (misses * 100.0 / total) : 0.0;

        long renders = renderCount.sum();
        long totalTime = totalRenderTimeMs.sum();
        double avgRenderTime = renders > 0 ? (totalTime * 1.0 / renders) : 0.0;

        long deduped = deduplicatedRequests.sum();
        long uptimeSeconds = (System.currentTimeMillis() - lastResetTime.get()) / 1000;

        return new MetricsSnapshot(
            hits,
            misses,
            total,
            hitRate,
            missRate,
            renders,
            totalTime,
            avgRenderTime,
            deduped,
            uptimeSeconds
        );
    }

    /**
     * Reset all metrics.
     * 重置所有指标。
     */
    public void reset() {
        cacheHits.reset();
        cacheMisses.reset();
        totalRenderTimeMs.reset();
        renderCount.reset();
        deduplicatedRequests.reset();
        lastResetTime.set(System.currentTimeMillis());
        log.info("Shiki 缓存指标已重置");
    }

    /**
     * Metrics snapshot record.
     * 指标快照记录。
     */
    @Getter
    public static class MetricsSnapshot {
        private final long cacheHits;
        private final long cacheMisses;
        private final long totalRequests;
        private final double hitRatePercent;
        private final double missRatePercent;
        private final long renderBatchCount;
        private final long totalRenderTimeMs;
        private final double avgRenderTimeMs;
        private final long deduplicatedRequests;
        private final long uptimeSeconds;

        public MetricsSnapshot(
            long cacheHits,
            long cacheMisses,
            long totalRequests,
            double hitRatePercent,
            double missRatePercent,
            long renderBatchCount,
            long totalRenderTimeMs,
            double avgRenderTimeMs,
            long deduplicatedRequests,
            long uptimeSeconds) {
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.totalRequests = totalRequests;
            this.hitRatePercent = hitRatePercent;
            this.missRatePercent = missRatePercent;
            this.renderBatchCount = renderBatchCount;
            this.totalRenderTimeMs = totalRenderTimeMs;
            this.avgRenderTimeMs = avgRenderTimeMs;
            this.deduplicatedRequests = deduplicatedRequests;
            this.uptimeSeconds = uptimeSeconds;
        }

        @Override
        public String toString() {
            return String.format(
                "ShikiCacheMetrics{缓存命中=%d, 未命中=%d, 总请求=%d, 命中率=%.2f%%, "
                    + "渲染批次=%d, 总耗时=%dms, 平均耗时=%.2fms, 去重节省=%d, 运行时间=%ds}",
                cacheHits, cacheMisses, totalRequests, hitRatePercent,
                renderBatchCount, totalRenderTimeMs, avgRenderTimeMs,
                deduplicatedRequests, uptimeSeconds
            );
        }
    }
}
