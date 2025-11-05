package top.howiehz.halo.plugin.extra.api.service.basic.config;

import lombok.Data;

/**
 * Configuration class for Pangu text spacing.
 * Pangu 中英文混排格式化配置文件类。
 *
 * @author HowieXie
 * @since 1.0.0
 */
@Data
public class PanguConfig {

    /**
     * Whether to enable Pangu rendering.
     * 是否启用 Pangu 自动渲染。
     */
    private boolean enabledPanguRender = true;
}
