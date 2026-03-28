import { readFileSync, writeFileSync } from "node:fs";
import yaml from "js-yaml";
import { bundledThemesInfo } from "shiki/bundle/full";

/* thanks to https://github.com/halo-sigs/plugin-shiki/blob/main/scripts/generate-themes-for-settings-file.mjs */

const THEME_FIELD_NAMES = ["lightTheme", "darkTheme", "theme"];
const settingsFilePath = new URL(
  "../../../src/main/resources/extensions/settings.yaml",
  import.meta.url,
);

try {
  const settingsFileContent = readFileSync(settingsFilePath, "utf8");
  const settings = yaml.load(settingsFileContent);

  if (!settings?.spec?.forms) {
    console.warn("No forms found in settings.yaml");
    process.exit(0);
  }

  const themeOptions = bundledThemesInfo.map((theme) => ({
    label: `${theme.displayName}（${theme.type}）`,
    value: theme.id,
  }));

  let updatedCount = 0;
  settings.spec.forms.forEach((form) => {
    if (form.group!== "shiki") {
      return;
    }
    form.formSchema?.forEach((field) => {
      if (THEME_FIELD_NAMES.includes(field.name)) {
        field.options = themeOptions;
        updatedCount++;
      }
    });
  });

  const updatedYaml = yaml.dump(settings, {
    indent: 2,
    lineWidth: -1,
    noRefs: true,
    quotingType: '"',
    forceQuotes: false,
    flowLevel: -1,
    sortKeys: false,
  });

  writeFileSync(settingsFilePath, updatedYaml, "utf8");
  console.log(`✓ Updated ${updatedCount} theme fields in settings.yaml`);
} catch (error) {
  console.error("Error updating settings.yaml:", error.message);
  process.exit(1);
}