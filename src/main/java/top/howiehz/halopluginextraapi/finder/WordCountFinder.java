package top.howiehz.halopluginextraapi.finder;

import reactor.core.publisher.Mono;

/**
 * Finder for calculating post word/character counts for themes to use.
 */
public interface WordCountFinder {
    /**
     * Get word count of the latest released content by post name (metadata.name).
     *
     * @param name post metadata.name
     * @return word count (non-negative)
     */
    Mono<Integer> releaseCountByName(String name);

    /**
     * Get word count of the latest head content by post name (including drafts).
     *
     * @param name post metadata.name
     * @return word count (non-negative)
     */
    Mono<Integer> headCountByName(String name);

    /**
     * Get word count of the latest released content by post slug.
     *
     * @param slug post spec.slug
     * @return word count (non-negative)
     */
    Mono<Integer> releaseCountBySlug(String slug);

    /**
     * Get word count of the latest head content by post slug (including drafts).
     *
     * @param slug post spec.slug
     * @return word count (non-negative)
     */
    Mono<Integer> headCountBySlug(String slug);
}

