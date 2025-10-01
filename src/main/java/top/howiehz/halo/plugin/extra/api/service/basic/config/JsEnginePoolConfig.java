package top.howiehz.halo.plugin.extra.api.service.basic.config;

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
     * <p>Default is 1 to minimize memory usage while ensuring at least one engine is always
     * available.</p>
     * <p>默认为 1,以最小化内存使用,同时确保至少有一个引擎始终可用。</p>
     */
    private int poolMinSize = 1;

    /**
     * Maximum pool size.
     * 最大池大小。
     * <p>Default is 2.
     * Setting this too high may cause OutOfMemoryError.</p>
     * <p>默认为 2。
     * 设置过高可能导致内存溢出错误。</p>
     */
    private int poolMaxSize = 2;
}
