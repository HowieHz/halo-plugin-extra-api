package top.howiehz.halo.plugin.extra.api.service.basic;

import org.springframework.lang.Nullable;
import java.math.BigInteger;

public interface PostStatsDataCacheManager {
    @Nullable
    BigInteger getCachedTotalWordCount(boolean isDraft);

    @Nullable
    BigInteger getCachedPostWordCount(String postName, boolean isDraft);

    void setTotalPostWordCount(BigInteger count, boolean isDraft);

    void setPostWordCount(String postName, BigInteger count, boolean isDraft);

    void clearTotalPostWordCountCache(boolean isDraft);

    void clearPostWordCountCache(String postName, boolean isDraft);
}
