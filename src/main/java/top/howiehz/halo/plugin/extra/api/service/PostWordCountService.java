package top.howiehz.halo.plugin.extra.api.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import top.howiehz.halo.plugin.extra.api.utils.PostWordCountUtil;

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
public class PostWordCountService {

    private final ReactiveExtensionClient client; // 响应式扩展客户端 / Reactive extension client
    private final PostContentService postContentService; // 文章内容服务 / Post content service
    private final PostStatsDataCacheManager postStatsDataCacheManager;

    public PostWordCountService(ReactiveExtensionClient client,
        PostContentService postContentService,
        PostStatsDataCacheManager postStatsDataCacheManager) {
        this.client = client; // 注入响应式扩展客户端 / Inject reactive extension client
        this.postContentService = postContentService; // 注入文章内容服务 / Inject post content service
        this.postStatsDataCacheManager = postStatsDataCacheManager;
    }

    /**
     * Get the word count for a specific post, from cache if available; otherwise compute and cache it.
     * 获取指定文章的字数，若缓存可用则直接返回，否则计算后写入缓存。
     *
     * <p>
     * If {@code postName} is null or blank, returns 0.
     * 若 {@code postName} 为空或空白，返回 0。
     * </p>
     *
     * @param postName the post name / 文章名称
     * @param isDraft whether to use draft content (true) or released content (false) / 是否使用草稿内容（true）或发布内容（false）
     * @return Mono emitting the word count; never null / 发出字数的 Mono；不为 null
     */
    public Mono<Integer> getPostWordCount(String postName, boolean isDraft) {
        if (postName == null || postName.isBlank()) {
            return Mono.just(0); // 空名称直接返回0 / Return 0 for empty name
        }
        // 尝试从缓存获取文章字数
        Integer cached = postStatsDataCacheManager.getCachedPostWordCount(postName, isDraft);
        if (cached != null) {
            return Mono.just(cached);
        }

        return refreshPostCountCache(postName, isDraft);
    }

    /**
     * Get the total word count across all posts, using cached total if available; otherwise recompute and cache it.
     * 获取所有文章的总字数，若总数缓存可用则直接返回，否则重新计算后写入缓存。
     *
     * @param isDraft whether to use draft content (true) or released content (false) / 是否使用草稿内容（true）或发布内容（false）
     * @return Mono emitting the total word count / 发出总字数的 Mono
     */
    public Mono<Integer> getTotalPostWordCount(boolean isDraft) {
        // 尝试从缓存获取总字数
        Integer cached = postStatsDataCacheManager.getCachedTotalWordCount(isDraft);
        if (cached != null) {
            return Mono.just(cached);
        }
        // 没缓存，直接全部计算
        return refreshAllPostCountCache(isDraft);
    }

    /**
     * Get the word count of content by post name and draft status, then update the cache.
     * 根据文章名称和草稿状态获取内容的字数统计，并更新缓存。
     *
     * @param postName the post name / 文章名称
     * @param isDraft whether to get draft content (true) or release content (false) / 是否获取草稿内容（true）或发布内容（false）
     * @return word count as Mono / 返回字数统计的 Mono
     */
    private Mono<Integer> refreshPostCountCache(String postName, boolean isDraft) {
        if (postName == null || postName.isBlank()) {
            return Mono.just(0); // 空名称直接返回0 / Return 0 for empty name
        }

        // 根据草稿状态选择对应的内容获取方法 / Choose content retrieval method based on draft status
        Mono<ContentWrapper> contentMono = isDraft ? postContentService.getHeadContent(postName)
            : postContentService.getReleaseContent(postName);

        return contentMono.map(ContentWrapper::getContent) // 提取 content 字段 / Extract content field
            .map(PostWordCountUtil::countHTMLWords) // 从 HTML 提取文本并计数 / Extract text from HTML and
            // count words
            .onErrorReturn(0) // 出错时返回 0 / Return 0 on error
            .defaultIfEmpty(0)  // 空结果时返回 0 / Return 0 if empty
            .doOnNext(count -> {
                // 更新缓存
                postStatsDataCacheManager.setPostWordCount(postName, count, isDraft);
            });
    }

    /**
     * Recalculate word counts for all posts and update both per-post cache and the total.
     * 重新计算所有文章的字数，并更新单篇缓存与总字数缓存。
     *
     * @param isDraft whether to process draft content (true) or released content (false) / 是否处理草稿内容（true）或发布内容（false）
     * @return Mono that emits the recomputed total when finished / 完成时发出重新计算后的总字数的 Mono
     */
    public Mono<Integer> refreshAllPostCountCache(boolean isDraft){
        return client.listAll(Post.class, ListOptions.builder().build(), Sort.unsorted())
            .map(post -> post.getMetadata().getName()) // 提取需要的名称
            .flatMapSequential(postName -> refreshPostCountCache(postName, isDraft),
                1024) // 1024 并发
            .reduce(0, Integer::sum) // 直接累加
            .doOnNext(totalCount -> {
                // 更新总字数缓存
                postStatsDataCacheManager.setTotalPostWordCount(totalCount, isDraft);
            })
            .onErrorReturn(0).defaultIfEmpty(0);
    }

    /**
     * Recalculate total word count from already cached per-post data.
     * 基于已缓存的单篇文章数据，重新计算总字数。
     *
     * @param isDraft whether to sum draft content counts (true) or released content counts (false) / 是否统计草稿内容字数（true）或发布内容字数（false）
     * @return Mono emitting the recalculated total word count / 发出重新计算的总字数的 Mono
     */
    public Mono<Integer> refreshTotalPostCountFromCache(boolean isDraft){
        return Mono.fromCallable(() -> {
            int total = 0;
            if (isDraft) {
                for (Integer count : postStatsDataCacheManager.draftPostWordCounts.values()) {
                    total += count;
                }
            } else {
                for (Integer count : postStatsDataCacheManager.releasePostWordCounts.values()) {
                    total += count;
                }
            }
            return total;
        }).doOnNext(totalCount -> {
            // 更新总字数缓存
            postStatsDataCacheManager.setTotalPostWordCount(totalCount, isDraft);
        }).onErrorReturn(0).defaultIfEmpty(0);
    }
}
