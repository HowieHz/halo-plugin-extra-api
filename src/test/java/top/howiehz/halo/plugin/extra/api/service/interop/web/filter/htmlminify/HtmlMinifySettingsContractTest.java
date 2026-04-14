package top.howiehz.halo.plugin.extra.api.service.interop.web.filter.htmlminify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class HtmlMinifySettingsContractTest {
    @Test
    void shouldKeepJavaDefaultsInSyncWithSettingsSchemaDefaults() {
        HtmlMinifyConfig config = new HtmlMinifyConfig();

        assertEquals(expectedDefaults(config), htmlMinifyDefaultsFromSettingsYaml());
    }

    private static Map<String, Object> expectedDefaults(HtmlMinifyConfig config) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("enabledHtmlMinify", config.isEnabledHtmlMinify());
        defaults.put("excludePaths", config.getExcludePaths());
        defaults.put("minifyCss", config.isMinifyCss());
        defaults.put("minifyJs", config.isMinifyJs());
        defaults.put("preserveBraceTemplateSyntax", config.isPreserveBraceTemplateSyntax());
        defaults.put("preserveChevronPercentTemplateSyntax",
            config.isPreserveChevronPercentTemplateSyntax());
        defaults.put("keepClosingTags", config.isKeepClosingTags());
        defaults.put("keepComments", config.isKeepComments());
        defaults.put("keepHtmlAndHeadOpeningTags", config.isKeepHtmlAndHeadOpeningTags());
        defaults.put("keepInputTypeTextAttr", config.isKeepInputTypeTextAttr());
        defaults.put("keepSsiComments", config.isKeepSsiComments());
        defaults.put("removeBangs", config.isRemoveBangs());
        defaults.put("removeProcessingInstructions", config.isRemoveProcessingInstructions());
        defaults.put("minifyDoctype", config.isMinifyDoctype());
        defaults.put("allowNoncompliantUnquotedAttributeValues",
            config.isAllowNoncompliantUnquotedAttributeValues());
        defaults.put("allowOptimalEntities", config.isAllowOptimalEntities());
        defaults.put("allowRemovingSpacesBetweenAttributes",
            config.isAllowRemovingSpacesBetweenAttributes());
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> htmlMinifyDefaultsFromSettingsYaml() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = HtmlMinifySettingsContractTest.class.getClassLoader()
            .getResourceAsStream("extensions/settings.yaml")) {
            Map<String, Object> root = yaml.load(inputStream);
            Map<String, Object> spec = (Map<String, Object>) root.get("spec");
            List<Map<String, Object>> forms = (List<Map<String, Object>>) spec.get("forms");
            Map<String, Object> htmlMinifyGroup = forms.stream()
                .filter(form -> "htmlMinify".equals(form.get("group")))
                .findFirst()
                .orElseThrow();
            List<Map<String, Object>> formSchema =
                (List<Map<String, Object>>) htmlMinifyGroup.get("formSchema");

            Map<String, Object> defaults = new LinkedHashMap<>();
            for (Map<String, Object> item : formSchema) {
                Object key = item.get("key");
                if (key != null) {
                    defaults.put(String.valueOf(key), item.get("value"));
                }
            }
            return defaults;
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse htmlMinify defaults from settings.yaml",
                exception);
        }
    }
}
