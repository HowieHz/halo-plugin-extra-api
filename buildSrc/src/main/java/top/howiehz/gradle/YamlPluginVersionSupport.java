package top.howiehz.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML Plugin Version Support utility
 * 为什么需要：确保 plugin.yaml 中的版本与构建版本一致，避免部署时版本不匹配
 * 参考官方 Halo 插件工具的实现，使用 SnakeYAML 进行安全的 YAML 处理
 */
public class YamlPluginVersionSupport {
    
    /**
     * 配置 plugin.yaml 版本同步逻辑
     * 为什么使用 SnakeYAML：比正则表达式更安全，保持 YAML 格式和注释
     */
    public static Action<Task> configurePluginYamlVersion(Project project, File manifestFile) {
        return task -> {
            if (!manifestFile.exists()) {
                project.getLogger().warn("Plugin manifest file not found: {}", manifestFile);
                return;
            }
            
            String projectVersion = String.valueOf(project.getVersion());
            if ("unspecified".equals(projectVersion)) {
                project.getLogger().warn("Project version is unspecified, skipping version injection");
                return;
            }
            
            try {
                updatePluginYamlVersion(manifestFile, projectVersion, project);
                project.getLogger().info("Updated plugin.yaml version to: {}", projectVersion);
            } catch (IOException e) {
                project.getLogger().error("Failed to update plugin.yaml version", e);
                throw new RuntimeException("Failed to update plugin.yaml version", e);
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private static void updatePluginYamlVersion(File yamlFile, String version, Project project) throws IOException {
        // 配置 YAML 处理选项 - 保持格式和可读性
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        
        Yaml yaml = new Yaml(options);
        
        // 读取现有 YAML 内容
        Map<String, Object> yamlData;
        try (FileInputStream fis = new FileInputStream(yamlFile)) {
            yamlData = yaml.load(fis);
        }
        
        if (yamlData == null) {
            yamlData = new LinkedHashMap<>();
        }
        
        // 更新版本信息 - 遵循 Halo 插件规范
        Map<String, Object> spec = (Map<String, Object>) yamlData.computeIfAbsent("spec", k -> new LinkedHashMap<>());
        String oldVersion = (String) spec.get("version");
        spec.put("version", version);
        
        // 写回文件 - 保持 UTF-8 编码
        try (FileWriter writer = new FileWriter(yamlFile, StandardCharsets.UTF_8)) {
            yaml.dump(yamlData, writer);
        }
        
        if (!version.equals(oldVersion)) {
            project.getLogger().lifecycle("Plugin version updated: {} -> {}", oldVersion, version);
        }
    }
}
