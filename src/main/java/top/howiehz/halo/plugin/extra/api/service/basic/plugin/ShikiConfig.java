package top.howiehz.halo.plugin.extra.api.service.basic.plugin;

import java.util.List;
import lombok.Data;

@Data
public class ShikiConfig {
    private boolean enabledShikiRender = true;
    private boolean enabledLineNumbers = true;
    private boolean enabledDoubleRenderMode = false;
    private List<String> extraInjectPaths;
    private String inlineStyle;
    private String theme;
    private String lightCodeClass;
    private String darkCodeClass;
    private String lightTheme;
    private String darkTheme;
}
