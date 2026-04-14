package top.howiehz.halo.plugin.extra.api.service.core.config;

import java.util.List;
import lombok.Data;

/**
 * Configuration class for Shiki code highlighting.
 * Shiki 代码高亮配置文件类。
 */
@Data
public class ShikiConfig {
    private static final String DEFAULT_INLINE_STYLE = """
        div pre.shiki {
          border-radius: 1rem;
          padding: 1rem; !important
        }

        div pre.shiki code {
          counter-reset: count;
          padding: 0; !important
        }

        div.light pre.shiki code {
          display: block;
        }

        div.dark pre.shiki code {
          display: none;
        }

        @media (prefers-color-scheme: dark) {
          div.light pre.shiki code {
            display: none;
          }

          div.dark pre.shiki code {
            display: block;
          }
        }

        div pre.shiki code .line:before {
          content: counter(count);
          counter-increment: count;
          text-align: right;
          color: #24292e80;
          width: 1em;
          margin-right: 1.5rem;
          display: inline-block;
        }

        div.dark pre.shiki code .line:before {
          content: counter(count);
          counter-increment: count;
          text-align: right;
          color: #e1e4e880;
          width: 1rem;
          margin-right: 1.5rem;
          display: inline-block;
        }
        """;

    /**
     * Whether to enable Shiki rendering.
     * 是否启用 Shiki 渲染。
     */
    private boolean enabledShikiRender = false;

    /**
     * Whether to enable double render mode (light and dark themes).
     * 是否启用双渲染模式（浅色和深色主题）。
     */
    private boolean enabledDoubleRenderMode = true;

    /**
     * Extra paths where CSS styles should be injected.
     * 额外需要注入 CSS 样式的路径列表。
     */
    private List<String> extraInjectPaths = List.of("/moments/**", "/docs/**");

    /**
     * Inline CSS styles for code highlighting.
     * 内联 CSS 样式，用于代码高亮。
     */
    private String inlineStyle = DEFAULT_INLINE_STYLE;

    /**
     * Theme name for single theme mode.
     * 单主题模式下的主题名称。
     */
    private String theme = "github-light";

    /**
     * CSS class name for light theme code blocks.
     * 浅色主题代码块的 CSS 类名。
     */
    private String lightCodeClass = "light";

    /**
     * CSS class name for dark theme code blocks.
     * 深色主题代码块的 CSS 类名。
     */
    private String darkCodeClass = "dark";

    /**
     * Theme name for light theme in double render mode.
     * 双渲染模式下浅色主题的主题名称。
     */
    private String lightTheme = "github-light";

    /**
     * Theme name for dark theme in double render mode.
     * 双渲染模式下深色主题的主题名称。
     */
    private String darkTheme = "github-dark";
}
