export const DISPLAY_ORDER = [
  "extra-api-lite",
  "extra-api-linux-x86_64",
  "extra-api-linux-arm64",
  "extra-api-macos-arm64",
  "extra-api-macos-x86_64",
  "extra-api-windows-x86_64",
  "extra-api-all-platforms",
  "nodejs-runtime-linux-x86_64",
  "nodejs-runtime-linux-arm64",
  "nodejs-runtime-macos-arm64",
  "nodejs-runtime-macos-x86_64",
  "nodejs-runtime-windows-x86_64",
  "nodejs-runtime-all-platforms",
  "minify-html-linux-x86_64",
  "minify-html-linux-arm64",
  "minify-html-macos-arm64",
  "minify-html-macos-x86_64",
  "minify-html-windows-x86_64",
  "minify-html-all-platforms",
];

export const PREFIX_BY_VARIANT = {
  "extra-api-lite": "extra-api-lite-",
  "extra-api-linux-x86_64": "extra-api-full-linux-x86_64-",
  "extra-api-linux-arm64": "extra-api-full-linux-arm64-",
  "extra-api-macos-arm64": "extra-api-full-macos-arm64-",
  "extra-api-macos-x86_64": "extra-api-full-macos-x86_64-",
  "extra-api-windows-x86_64": "extra-api-full-windows-x86_64-",
  "extra-api-all-platforms": "extra-api-full-all-platforms-",
  "nodejs-runtime-linux-x86_64": "nodejs-runtime-linux-x86_64-",
  "nodejs-runtime-linux-arm64": "nodejs-runtime-linux-arm64-",
  "nodejs-runtime-macos-arm64": "nodejs-runtime-macos-arm64-",
  "nodejs-runtime-macos-x86_64": "nodejs-runtime-macos-x86_64-",
  "nodejs-runtime-windows-x86_64": "nodejs-runtime-windows-x86_64-",
  "nodejs-runtime-all-platforms": "nodejs-runtime-all-platforms-",
  "minify-html-linux-x86_64": "minify-html-linux-x86_64-",
  "minify-html-linux-arm64": "minify-html-linux-arm64-",
  "minify-html-macos-arm64": "minify-html-macos-arm64-",
  "minify-html-macos-x86_64": "minify-html-macos-x86_64-",
  "minify-html-windows-x86_64": "minify-html-windows-x86_64-",
  "minify-html-all-platforms": "minify-html-all-platforms-",
};

export function resolveAssetVariant(fileName) {
  for (const variant of DISPLAY_ORDER) {
    if (fileName.startsWith(PREFIX_BY_VARIANT[variant])) {
      return variant;
    }
  }
  return null;
}

function compareByDisplayOrder(left, right) {
  const leftVariant = resolveAssetVariant(left);
  const rightVariant = resolveAssetVariant(right);
  const leftIndex = leftVariant === null ? Number.MAX_SAFE_INTEGER : DISPLAY_ORDER.indexOf(leftVariant);
  const rightIndex = rightVariant === null ? Number.MAX_SAFE_INTEGER : DISPLAY_ORDER.indexOf(rightVariant);

  if (leftIndex !== rightIndex) {
    return leftIndex - rightIndex;
  }

  return left.localeCompare(right, "en");
}

export function assertKnownAssets(fileNames) {
  const unknown = fileNames.filter((fileName) => resolveAssetVariant(fileName) === null);
  if (unknown.length > 0) {
    throw new Error(`Unknown release assets: ${unknown.join(", ")}`);
  }
}

export function sortByDisplayOrder(fileNames) {
  assertKnownAssets(fileNames);
  return [...fileNames].sort(compareByDisplayOrder);
}

export function buildReleaseAssetNames(extraApiVersion, nodejsRuntimeVersion = extraApiVersion, minifyHtmlVersion = extraApiVersion) {
  return DISPLAY_ORDER.map((variant) => {
    const version = variant.startsWith("nodejs-runtime-")
      ? nodejsRuntimeVersion
      : variant.startsWith("minify-html-")
        ? minifyHtmlVersion
        : extraApiVersion;
    return `${PREFIX_BY_VARIANT[variant]}${version}.jar`;
  });
}
