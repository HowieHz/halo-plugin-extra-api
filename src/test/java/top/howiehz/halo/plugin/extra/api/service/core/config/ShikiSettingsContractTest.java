package top.howiehz.halo.plugin.extra.api.service.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class ShikiSettingsContractTest {
    @Test
    void shouldKeepJavaDefaultsInSyncWithSettingsSchemaDefaults() {
        ShikiConfig config = new ShikiConfig();

        assertEquals(expectedDefaults(config), shikiDefaultsFromSettingsYaml());
    }

    private static Map<String, Object> expectedDefaults(ShikiConfig config) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("enabledShikiRender", config.isEnabledShikiRender());
        defaults.put("inlineStyle", normalizeString(config.getInlineStyle()));
        defaults.put("extraInjectPaths", config.getExtraInjectPaths());
        defaults.put("theme", config.getTheme());
        defaults.put("enabledDoubleRenderMode", config.isEnabledDoubleRenderMode());
        defaults.put("lightCodeClass", config.getLightCodeClass());
        defaults.put("darkCodeClass", config.getDarkCodeClass());
        defaults.put("lightTheme", config.getLightTheme());
        defaults.put("darkTheme", config.getDarkTheme());
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> shikiDefaultsFromSettingsYaml() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = ShikiSettingsContractTest.class.getClassLoader()
            .getResourceAsStream("extensions/settings.yaml")) {
            Map<String, Object> root = yaml.load(inputStream);
            Map<String, Object> spec = (Map<String, Object>) root.get("spec");
            List<Map<String, Object>> forms = (List<Map<String, Object>>) spec.get("forms");
            Map<String, Object> shikiGroup = forms.stream()
                .filter(form -> "shiki".equals(form.get("group")))
                .findFirst()
                .orElseThrow();
            List<Map<String, Object>> formSchema =
                (List<Map<String, Object>>) shikiGroup.get("formSchema");

            Map<String, Object> defaults = new LinkedHashMap<>();
            for (Map<String, Object> item : formSchema) {
                Object key = item.get("key");
                if (key != null) {
                    Object value = item.get("value");
                    defaults.put(String.valueOf(key),
                        value instanceof String stringValue ? normalizeString(stringValue) : value);
                }
            }
            return defaults;
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse shiki defaults from settings.yaml",
                exception);
        }
    }

    private static String normalizeString(String value) {
        return value == null ? null : value.stripTrailing();
    }
}
