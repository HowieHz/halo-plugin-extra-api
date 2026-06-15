package top.howiehz.halo.plugin.node.runtime;

import java.time.Duration;
import lombok.Data;

@Data
public class NodeRuntimeConfig {

    private int poolMinSize = 1;
    private int poolMaxSize = 2;
    private long defaultTimeoutSeconds = 30;
    private long maxTimeoutSeconds = -1;

    public Duration defaultTimeout() {
        return Duration.ofSeconds(Math.max(1, defaultTimeoutSeconds));
    }

    public Duration normalizeTimeout(Duration requested) {
        Duration timeout = requested == null ? defaultTimeout() : requested;
        if (timeout.isZero() || timeout.isNegative()) {
            timeout = defaultTimeout();
        }
        if (maxTimeoutSeconds > 0) {
            Duration maxTimeout = Duration.ofSeconds(maxTimeoutSeconds);
            if (timeout.compareTo(maxTimeout) > 0) {
                return maxTimeout;
            }
        }
        return timeout;
    }

    public int normalizedPoolMinSize() {
        return Math.max(1, poolMinSize);
    }

    public int normalizedPoolMaxSize() {
        return Math.max(normalizedPoolMinSize(), poolMaxSize);
    }
}
