package top.howiehz.halo.plugin.extra.api.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import top.howiehz.halo.plugin.extra.api.utils.PostWordCountUtil;
import java.math.BigInteger;

/**
 * Service for computing and caching word counts for Halo posts.
 * 计算并缓存 Halo 文章字数的服务。
 *
 * <p>
 * It provides per-post and total word counts for draft or released content,
 * leveraging a cache to avoid repeated computation.
 * 提供草稿或已发布内容的单篇与总字数统计，并利用缓存避免重复计算。
 * </p>
 */
@Component
public class PostWordCountServiceImpl implements PostWordCountService {

    private final ReactiveExtensionClient client; // 响应式扩展客户端 / Reactive extension client
    private final PostContentService postContentService; // 文章内容服务 / Post content service
    private final PostStatsDataCacheManager postStatsDataCacheManager;

    public PostWordCountServiceImpl(ReactiveExtensionClient client,
                                    PostContentService postContentService,
                                    PostStatsDataCacheManager postStatsDataCacheManager) {
        this.client = client; // 注入响应式扩展客户端 / Inject reactive extension client
        this.postContentService = postContentService; // 注入文章内容服务 / Inject post content service
        this.postStatsDataCacheManager = postStatsDataCacheManager;
    }

    /**
     * Get the word count for a specific post, from cache if available; otherwise compute and
     * cache it.
     * 获取指定文章的字数，若缓存可用则直接返回，否则计算后写入缓存。
     *
     * <p>
     * If {@code postName} is null or blank, returns 0.
     * 若 {@code postName} 为空或空白，返回 0。
     * </p>
     *
     * @param postName the post name / 文章名称
     * @param isDraft whether to use draft content (true) or released content (false) /
     * 是否使用草稿内容（true）或发布内容（false）
     * @return Mono emitting the word count; never null / 发出字数的 Mono；不为 null
     */
    @Override
    public Mono<BigInteger> getPostWordCount(String postName, boolean isDraft) {
        if (postName == null || postName.isBlank()) {
            return Mono.just(
                BigInteger.ZERO);  // Return 0 for null or blank post name / 对于空或空白的文章名称返回 0
        }
        BigInteger cached = postStatsDataCacheManager.getCachedPostWordCount(postName, isDraft);
        if (cached != null) {
            return Mono.just(cached);
        }
        return refreshPostCountCache(postName, isDraft);
    }

    /**
     * Get the total word count across all posts, using cached total if available; otherwise
     * recompute and cache it.
     * 获取所有文章的总字数，若总数缓存可用则直接返回，否则重新计算后写入缓存。
     *
     * @param isDraft whether to use draft content (true) or released content (false) /
     * 是否使用草稿内容（true）或发布内容（false）
     * @return Mono emitting the total word count / 发出总字数的 Mono
     */
    @Override
    public Mono<BigInteger> getTotalPostWordCount(boolean isDraft) {
        BigInteger cached = postStatsDataCacheManager.getCachedTotalWordCount(isDraft);
        if (cached != null) {
            return Mono.just(cached);
        }
        return refreshAllPostCountCache(isDraft);
    }

    /**
     * Preheat all word count caches for both draft and released content.
     * 预热草稿与发布内容的所有字数缓存。
     */
    @Override
    public void warmUpAllCache() {
        refreshAllPostCountCache(false).subscribe();
        refreshAllPostCountCache(true).subscribe();
    }

    /**
     * Get the word count of content by post name and draft status, then update the cache.
     * 根据文章名称和草稿状态获取内容的字数统计，并更新缓存。
     *
     * @param postName the post name / 文章名称
     * @param isDraft whether to get draft content (true) or release content (false) /
     * 是否获取草稿内容（true）或发布内容（false）
     * @return word count as Mono / 返回字数统计的 Mono
     */
    @Override
    public Mono<BigInteger> refreshPostCountCache(String postName, boolean isDraft) {
        if (postName == null || postName.isBlank()) {
            return Mono.just(BigInteger.ZERO);
        }
        Mono<ContentWrapper> contentMono = isDraft ? postContentService.getHeadContent(postName)
            : postContentService.getReleaseContent(postName);

        return contentMono.publishOn(Schedulers.parallel()) // 切换下游到并行线程池
            .map(ContentWrapper::getContent).map(PostWordCountUtil::countHTMLWords)
            .onErrorReturn(BigInteger.ZERO).defaultIfEmpty(BigInteger.ZERO).doOnNext(
                count -> postStatsDataCacheManager.setPostWordCount(postName, count,
                    isDraft)); // 更新缓存
    }

    /**
     * Recalculate word counts for all posts and update both per-post cache and the total.
     * 重新计算所有文章的字数，并更新单篇缓存与总字数缓存。
     *
     * @param isDraft whether to process draft content (true) or released content (false) /
     * 是否处理草稿内容（true）或发布内容（false）
     * @return Mono that emits the recomputed total when finished / 完成时发出重新计算后的总字数的 Mono
     */
    @Override
    public Mono<BigInteger> refreshAllPostCountCache(boolean isDraft) {
        return client.listAll(Post.class, ListOptions.builder().build(), Sort.unsorted())
            .subscribeOn(Schedulers.parallel()).map(post -> post.getMetadata().getName())
            .flatMapSequential(postName -> refreshPostCountCache(postName, isDraft),
                1024) // 并发处理以提升性能 / Process concurrently for better performance
            .reduce(BigInteger.ZERO, BigInteger::add).onErrorReturn(BigInteger.ZERO).doOnNext(
                total -> postStatsDataCacheManager.setTotalPostWordCount(total,
                    isDraft))  // 更新总字数缓存 / Update total word count cache
            ;
    }

    /**
     * Recalculate total word count from already cached per-post data.
     * 基于已缓存的单篇文章数据，重新计算总字数。
     *
     * @param isDraft whether to sum draft content counts (true) or released content counts
     * (false) / 是否统计草稿内容字数（true）或发布内容字数（false）
     * @return Mono emitting the recalculated total word count / 发出重新计算的总字数的 Mono
     */
    @Override
    public Mono<BigInteger> refreshTotalPostCountFromCache(boolean isDraft) {
        return Mono.fromSupplier(() -> {
                var values = (isDraft ? postStatsDataCacheManager.draftPostWordCounts
                    : postStatsDataCacheManager.releasePostWordCounts).values();

                BigInteger total = BigInteger.ZERO;
                for (BigInteger v : values) {
                    if (v != null) {
                        total = total.add(v);
                    }
                }
                return total;
            }).subscribeOn(Schedulers.parallel()).onErrorReturn(BigInteger.ZERO)
            .doOnNext(total -> postStatsDataCacheManager.setTotalPostWordCount(total, isDraft));
    }
}
