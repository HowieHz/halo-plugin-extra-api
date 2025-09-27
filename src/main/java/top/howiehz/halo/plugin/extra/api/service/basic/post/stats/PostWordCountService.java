package top.howiehz.halo.plugin.extra.api.service.basic.post.stats;

import java.math.BigInteger;
import reactor.core.publisher.Mono;

public interface PostWordCountService {
    Mono<BigInteger> getPostWordCount(String postName, boolean isDraft);

    Mono<BigInteger> getTotalPostWordCount(boolean isDraft);

    void warmUpAllCache();

    Mono<BigInteger> refreshPostCountCache(String postName, boolean isDraft);

    Mono<BigInteger> refreshAllPostCountCache(boolean isDraft);

    Mono<BigInteger> refreshTotalPostCountFromCache(boolean isDraft);
}
