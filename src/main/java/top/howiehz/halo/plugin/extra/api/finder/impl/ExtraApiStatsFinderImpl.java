package top.howiehz.halo.plugin.extra.api.finder.impl;

import java.util.*;
import java.math.BigInteger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.finders.Finder;
import top.howiehz.halo.plugin.extra.api.finder.ExtraApiStatsFinder;
import top.howiehz.halo.plugin.extra.api.service.PostWordCountService;

/**
 * Implementation of ExtraApiStatsFinder.
 * 统计 Finder 的实现，用于为主题提供字数统计能力。
 */
@Component
@Finder("extraApiStatsFinder")
public class ExtraApiStatsFinderImpl implements ExtraApiStatsFinder {

    private final PostWordCountService postWordCountService;

    /**
     * Constructor to initialize ExtraApiStatsFinderImpl with required dependencies.
     * 构造函数，注入所需依赖。
     */
    public ExtraApiStatsFinderImpl(PostWordCountService postWordCountService) {
        this.postWordCountService = postWordCountService;
    }

    /**
     * Unified word count API without slug support.
     * If name provided, count by name; otherwise sum word counts across all posts
     * (release/draft selectable by version).
     * 统一的字数统计 API
     * 若提供 name 参数则按名称统计，否则统计所有文章的总字数（version 可选 release/draft）。
     *
     * @param params parameter map: name? version? ('release'|'draft', default 'release') /
     * 参数映射：name？version？（'release' 或 'draft'，默认 'release'）
     * @return word count as Mono (non-negative) / 返回字数（非负）的 Mono
     */
    @Override
    public Mono<BigInteger> postWordCount(Map<String, Object> params) {
        Map<String, Object> map = params == null ? java.util.Collections.emptyMap() : params;
        String postName = String.valueOf(map.get("name"));
        boolean isDraft =
            String.valueOf(map.getOrDefault("version", "release")).equalsIgnoreCase("draft");

        if ("null".equals(postName) || postName.isBlank()) {
            return postWordCountService.getTotalPostWordCount(isDraft);
        }

        return postWordCountService.getPostWordCount(postName, isDraft);
    }
}
