package top.howiehz.halo.plugin.extra.api.service.core.config;

import lombok.Data;

/**
 * Configuration class for V8 engine pool.
 * V8 引擎池配置类。
 */
@Data
public class JsEnginePoolConfig {

    /**
     * Minimum pool size.
     * 最小池大小。
     */
    private int poolMinSize;

    /**
     * Maximum pool size.
     * 最大池大小。
     */
    private int poolMaxSize;
}
