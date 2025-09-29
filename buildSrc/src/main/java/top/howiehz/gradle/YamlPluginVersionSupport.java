package top.howiehz.gradle;

import static org.gradle.api.Project.DEFAULT_VERSION;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSetContainer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML Plugin Version Support utility
 * 为什么需要：确保 plugin.yaml 中的版本与构建版本一致，避免部署时版本不匹配
 * 参考官方 Halo 插件工具的实现，操作输出目录中的 plugin.yaml 文件
 */
public class YamlPluginVersionSupport {
    
    /**
     * 配置 plugin.yaml 版本同步逻辑
     * 关键修复：操作输出目录中的文件，而不是源码目录中的原始文件
     * 这样版本注入会在 processResources 阶段生效，确保最终 JAR 中包含正确版本
     */
    public static Action<Task> configurePluginYamlVersion(Project project, File manifestFile) {
        // 获取输出目录 - 关键：这是 processResources 处理后的位置
        File outputResourcesDir = project.getExtensions().getByType(SourceSetContainer.class)
            .getByName(MAIN_SOURCE_SET_NAME)
            .getOutput().getResourcesDir();
            
        if (outputResourcesDir == null) {
            throw new RuntimeException("Can not find resources output dir.");
        }
        
        Path outputPluginYaml = outputResourcesDir.toPath().resolve(manifestFile.getName());
        
        return task -> {
            try {
                rewritePluginYaml(outputPluginYaml.toFile(), project);
                project.getLogger().lifecycle("Plugin version updated to: {}", getProjectVersion(project));
            } catch (Exception e) {
                project.getLogger().error("Failed to update plugin.yaml version", e);
                throw new RuntimeException("Failed to update plugin.yaml version", e);
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private static void rewritePluginYaml(File outputPluginYaml, Project project) throws IOException {
        if (!outputPluginYaml.exists()) {
            project.getLogger().warn("Output plugin.yaml not found: {}", outputPluginYaml);
            return;
        }
        
        // 配置 YAML 处理选项 - 保持格式和可读性
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        
        Yaml yaml = new Yaml(options);
        
        // 读取输出目录中的 YAML 内容
        Map<String, Object> yamlData;
        try (FileInputStream fis = new FileInputStream(outputPluginYaml)) {
            yamlData = yaml.load(fis);
        }
        
        if (yamlData == null) {
            yamlData = new LinkedHashMap<>();
        }
        
        // 更新版本信息 - 遵循 Halo 插件规范
        Map<String, Object> spec = (Map<String, Object>) yamlData.computeIfAbsent("spec", k -> new LinkedHashMap<>());
        spec.put("version", getProjectVersion(project));
        
        // 写回输出文件 - 保持 UTF-8 编码
        try (FileWriter writer = new FileWriter(outputPluginYaml, StandardCharsets.UTF_8)) {
            yaml.dump(yamlData, writer);
        }
    }
    
    private static String getProjectVersion(Project project) {
        String version = String.valueOf(project.getVersion());
        if (StringUtils.equals(DEFAULT_VERSION, version)) {
            throw new IllegalStateException("Project version must be set.");
        }
        return version;
    }
}
