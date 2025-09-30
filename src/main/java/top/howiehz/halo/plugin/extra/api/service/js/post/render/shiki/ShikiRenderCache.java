package top.howiehz.halo.plugin.extra.api.service.js.post.render.shiki;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LRU + TTL cache for Shiki code highlight results.
 * 基于 LRU + TTL 的 Shiki 代码高亮结果缓存。
 *
 * <p><b>缓存策略:</b></p>
 * <ul>
 *   <li>LRU(最近最少使用): 限制最大缓存条目数,自动淘汰最少使用的条目</li>
 *   <li>TTL(过期时间): 每个条目有独立的过期时间,默认 24 小时</li>
 *   <li>缓存键: SHA-256(code) + language + theme</li>
 * </ul>
 *
 * <p><b>为什么使用这种策略:</b></p>
 * <ul>
 *   <li>代码高亮是纯函数,相同输入总是产生相同输出,适合长期缓存</li>
 *   <li>但 Shiki 版本更新或主题更新时需要刷新缓存</li>
 *   <li>LRU 保证内存可控,TTL 保证数据新鲜度</li>
 * </ul>
 *
 * <p><b>线程安全:</b> 使用 ReadWriteLock 保护 LRU 操作,ConcurrentHashMap 用于快速查找</p>
 */
@Slf4j
@Component
public class ShikiRenderCache {

    /**
     * Maximum number of cache entries.
     * 最大缓存条目数。
     * <p>
     * 为什么选择 10000:
     * - 假设每条缓存 5KB HTML,10000 条约 50MB 内存
     * - 对于大多数博客站点足够(通常不会超过几千个不同的代码块)
     * - 可通过配置调整
     */
    private static final int MAX_CACHE_SIZE = 10_000;

    /**
     * Default TTL for cache entries.
     * 缓存条目的默认过期时间。
     * <p>
     * 为什么选择 24 小时:
     * - 足够长以提供良好的缓存命中率
     * - 足够短以在插件更新或主题变更后及时刷新
     * - 用户修改主题配置后最多等待 24 小时生效
     */
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * LRU map for maintaining access order.
     * 维护访问顺序的 LRU 映射。
     * <p>
     * 为什么使用 LinkedHashMap:
     * - 内置 LRU 支持(accessOrder=true)
     * - 自动维护访问顺序
     * - removeEldestEntry 方法可自定义淘汰策略
     */
    private final Map<String, CacheEntry> lruMap;

    /**
     * Fast lookup map for O(1) get operations.
     * 用于 O(1) 查找操作的快速查找映射。
     * <p>
     * 为什么额外使用 ConcurrentHashMap:
     * - LinkedHashMap 在多线程下需要外部同步
     * - ConcurrentHashMap 提供更好的读性能
     * - 写操作时同时更新两个 map
     */
    private final Map<String, CacheEntry> fastLookup;

    /**
     * Lock for protecting LRU operations.
     * 保护 LRU 操作的锁。
     * <p>
     * 为什么使用读写锁:
     * - 读操作(get)频繁,写操作(put)相对较少
     * - 读写锁允许多个读操作并发执行
     * - 只有写操作需要独占锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * MessageDigest instance for computing cache keys.
     * 用于计算缓存键的 MessageDigest 实例。
     */
    private final ThreadLocal<MessageDigest> digestThreadLocal = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    });

    public ShikiRenderCache() {
        // 初始化 LRU map,设置为访问顺序模式
        this.lruMap = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                // 当条目数超过最大值时,自动移除最旧的条目
                return size() > MAX_CACHE_SIZE;
            }
        };

        this.fastLookup = new ConcurrentHashMap<>();

        log.info("初始化 Shiki 渲染缓存: 最大条目数={}, TTL={}", MAX_CACHE_SIZE, DEFAULT_TTL);
    }

    /**
     * Get cached highlight result.
     * 获取缓存的高亮结果。
     *
     * @param code the source code / 源代码
     * @param language the language identifier / 语言标识
     * @param theme the theme name / 主题名称
     * @return the cached HTML or null if not found or expired / 缓存的 HTML 或 null(未找到或已过期)
     */
    public String get(String code, String language, String theme) {
        String key = computeCacheKey(code, language, theme);

        // 先从快速查找 map 中获取
        CacheEntry entry = fastLookup.get(key);

        if (entry == null) {
            return null;
        }

        // 检查是否过期
        if (entry.isExpired()) {
            // 过期则删除
            invalidate(code, language, theme);
            return null;
        }

        // 更新 LRU 访问顺序
        lock.writeLock().lock();
        try {
            // 通过 get 操作更新访问顺序
            lruMap.get(key);
        } finally {
            lock.writeLock().unlock();
        }

        return entry.html;
    }

    /**
     * Put highlight result into cache.
     * 将高亮结果放入缓存。
     *
     * @param code the source code / 源代码
     * @param language the language identifier / 语言标识
     * @param theme the theme name / 主题名称
     * @param html the highlighted HTML / 高亮后的 HTML
     */
    public void put(String code, String language, String theme, String html) {
        String key = computeCacheKey(code, language, theme);
        CacheEntry entry = new CacheEntry(html, Instant.now().plus(DEFAULT_TTL));

        lock.writeLock().lock();
        try {
            lruMap.put(key, entry);
            fastLookup.put(key, entry);

            // 如果 LRU map 触发了淘汰,也需要从 fastLookup 中删除
            if (lruMap.size() < fastLookup.size()) {
                // 找出被淘汰的 key
                fastLookup.keySet().removeIf(k -> !lruMap.containsKey(k));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Invalidate a specific cache entry.
     * 使特定缓存条目失效。
     *
     * @param code the source code / 源代码
     * @param language the language identifier / 语言标识
     * @param theme the theme name / 主题名称
     */
    public void invalidate(String code, String language, String theme) {
        String key = computeCacheKey(code, language, theme);

        lock.writeLock().lock();
        try {
            lruMap.remove(key);
            fastLookup.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear all cache entries.
     * 清空所有缓存条目。
     * <p>
     * 使用场景:
     * - 插件配置变更时
     * - Shiki 主题更新时
     * - 手动触发缓存刷新时
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            int size = lruMap.size();
            lruMap.clear();
            fastLookup.clear();
            log.info("清空 Shiki 渲染缓存,已删除 {} 条记录", size);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove expired entries.
     * 移除过期的条目。
     * <p>
     * 建议定期调用(如每小时一次)以释放内存。
     * 可以通过 Spring @Scheduled 注解实现定时清理。
     *
     * @return number of removed entries / 移除的条目数
     */
    public int removeExpired() {
        lock.writeLock().lock();
        try {
            int removed = 0;
            var iterator = lruMap.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    fastLookup.remove(entry.getKey());
                    removed++;
                }
            }

            if (removed > 0) {
                log.debug("移除了 {} 条过期的缓存记录", removed);
            }

            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current cache size.
     * 获取当前缓存大小。
     *
     * @return number of cached entries / 缓存条目数
     */
    public int size() {
        return fastLookup.size();
    }

    /**
     * Get cache statistics.
     * 获取缓存统计信息。
     *
     * @return cache statistics string / 缓存统计信息字符串
     */
    public String getStats() {
        lock.readLock().lock();
        try {
            long expired = lruMap.values().stream()
                .filter(CacheEntry::isExpired)
                .count();

            return String.format("缓存统计: 总数=%d, 过期=%d, 有效=%d, 容量=%d%%",
                lruMap.size(), expired, lruMap.size() - expired,
                (lruMap.size() * 100) / MAX_CACHE_SIZE);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Compute cache key from code, language, and theme.
     * 从代码、语言和主题计算缓存键。
     * <p>
     * 为什么使用 SHA-256:
     * - 代码内容可能很长,直接作为 key 会占用大量内存
     * - SHA-256 产生固定长度的 hash(32 字节 hex = 64 字符)
     * - 碰撞概率极低,可以安全地作为唯一标识
     *
     * @param code the source code / 源代码
     * @param language the language identifier / 语言标识
     * @param theme the theme name / 主题名称
     * @return the cache key / 缓存键
     */
    private String computeCacheKey(String code, String language, String theme) {
        MessageDigest digest = digestThreadLocal.get();
        digest.reset();

        // 计算代码内容的 hash
        byte[] codeHash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
        String codeHashHex = HexFormat.of().formatHex(codeHash);

        // 组合 hash + language + theme 作为最终的 key
        // 格式: {hash}:{language}:{theme}
        return codeHashHex + ":" + language + ":" + theme;
    }

    /**
     * Cache entry with expiration time.
     * 带过期时间的缓存条目。
     */
    private static class CacheEntry {
        private final String html;
        private final Instant expiresAt;

        CacheEntry(String html, Instant expiresAt) {
            this.html = html;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
