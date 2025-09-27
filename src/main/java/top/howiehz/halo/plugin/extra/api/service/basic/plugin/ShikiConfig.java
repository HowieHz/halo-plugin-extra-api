package top.howiehz.halo.plugin.extra.api.service.basic.plugin;

import java.util.List;
import lombok.Data;

/**
 * Configuration class for Shiki code highlighting.
 * Shiki 代码高亮配置文件类。
 */
@Data
public class ShikiConfig {

    /**
     * Whether to enable Shiki rendering.
     * 是否启用 Shiki 渲染。
     */
    private boolean enabledShikiRender = true;

    /**
     * Whether to enable double render mode (light and dark themes).
     * 是否启用双渲染模式（浅色和深色主题）。
     */
    private boolean enabledDoubleRenderMode = false;

    /**
     * Extra paths where CSS styles should be injected.
     * 额外需要注入 CSS 样式的路径列表。
     */
    private List<String> extraInjectPaths;

    /**
     * Inline CSS styles for code highlighting.
     * 内联 CSS 样式，用于代码高亮。
     */
    private String inlineStyle;

    /**
     * Theme name for single theme mode.
     * 单主题模式下的主题名称。
     */
    private String theme;

    /**
     * CSS class name for light theme code blocks.
     * 浅色主题代码块的 CSS 类名。
     */
    private String lightCodeClass;

    /**
     * CSS class name for dark theme code blocks.
     * 深色主题代码块的 CSS 类名。
     */
    private String darkCodeClass;

    /**
     * Theme name for light theme in double render mode.
     * 双渲染模式下浅色主题的主题名称。
     */
    private String lightTheme;

    /**
     * Theme name for dark theme in double render mode.
     * 双渲染模式下深色主题的主题名称。
     */
    private String darkTheme;
}
