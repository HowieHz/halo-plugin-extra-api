const DISPLAY_ORDER = [
  "lite",
  "linux-x86_64",
  "linux-arm64",
  "macos-arm64",
  "macos-x86_64",
  "windows-x86_64",
  "all-platforms",
];

const PREFIX_BY_VARIANT = {
  lite: "extra-api-lite-",
  "linux-x86_64": "extra-api-full-linux-x86_64-",
  "linux-arm64": "extra-api-full-linux-arm64-",
  "macos-arm64": "extra-api-full-macos-arm64-",
  "macos-x86_64": "extra-api-full-macos-x86_64-",
  "windows-x86_64": "extra-api-full-windows-x86_64-",
  "all-platforms": "extra-api-full-all-platforms-",
};

function resolveAssetVariant(fileName) {
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

function sortForGitHubRelease(fileNames) {
  return [...fileNames].sort(compareByDisplayOrder);
}

function sortForHaloStoreUpload(fileNames) {
  return sortForGitHubRelease(fileNames).reverse();
}

function assertKnownReleaseAssets(fileNames) {
  const unknown = fileNames.filter((fileName) => resolveAssetVariant(fileName) === null);
  if (unknown.length > 0) {
    throw new Error(`Unknown release assets: ${unknown.join(", ")}`);
  }
}

module.exports = {
  DISPLAY_ORDER,
  PREFIX_BY_VARIANT,
  resolveAssetVariant,
  sortForGitHubRelease,
  sortForHaloStoreUpload,
  assertKnownReleaseAssets,
};
