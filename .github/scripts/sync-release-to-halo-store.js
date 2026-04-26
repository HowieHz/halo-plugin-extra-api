import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

import { assertKnownReleaseAssets, sortForHaloStoreUpload } from "./release-asset-order.js";

const {
  ASSETS_DIR,
  GITHUB_RELEASE_TAG,
  GITHUB_REPOSITORY,
  GITHUB_TOKEN,
  HALO_APP_ID,
  HALO_BACKEND_BASEURL = "https://www.halo.run",
  HALO_PAT,
  RELEASE_NAME,
  RELEASE_NOTES_FILE,
  RELEASE_PRERELEASE,
} = process.env;

const hasDirectReleaseMetadata = Boolean(RELEASE_NAME && RELEASE_NOTES_FILE && RELEASE_PRERELEASE);

if (!ASSETS_DIR || !GITHUB_REPOSITORY || !GITHUB_TOKEN || !HALO_PAT) {
  console.error("Missing required environment variables for Halo App Store sync");
  process.exit(1);
}

if (!hasDirectReleaseMetadata && !GITHUB_RELEASE_TAG) {
  console.error("Either direct release metadata or GITHUB_RELEASE_TAG must be provided");
  process.exit(1);
}

const repoApiBase = `https://api.github.com/repos/${GITHUB_REPOSITORY}`;
const haloApiBase = HALO_BACKEND_BASEURL.replace(/\/$/u, "");

function extractYamlScalar(content, key) {
  const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");
  const match = content.match(new RegExp(`^\\s*${escapedKey}:\\s*["']?([^"'\\n#]+)["']?`, "mu"));
  return match?.[1]?.trim() || "";
}

function listAssets() {
  const assets = fs
    .readdirSync(ASSETS_DIR)
    .filter((fileName) => fs.statSync(path.join(ASSETS_DIR, fileName)).isFile());

  if (assets.length === 0) {
    throw new Error(`Assets directory is empty: ${ASSETS_DIR}`);
  }

  assertKnownReleaseAssets(assets);
  return sortForHaloStoreUpload(assets);
}

function readPluginManifest(assetPath) {
  const yamlContent = execFileSync("unzip", ["-p", assetPath, "plugin.yaml"], {
    encoding: "utf8",
  });

  const version = extractYamlScalar(yamlContent, "version");
  const requires = extractYamlScalar(yamlContent, "requires");
  const appId = HALO_APP_ID || extractYamlScalar(yamlContent, "store.halo.run/app-id");

  if (!version || !requires) {
    throw new Error(`Unable to read spec.version or spec.requires from ${assetPath}`);
  }

  return {
    version,
    requires,
    appId,
  };
}

async function githubRequest(pathname, init = {}) {
  const response = await fetch(`${repoApiBase}${pathname}`, {
    ...init,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${GITHUB_TOKEN}`,
      "User-Agent": "halo-plugin-extra-api-release-bot",
      ...(init.headers ?? {}),
    },
  });

  if (!response.ok) {
    throw new Error(`GitHub API request failed: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

async function haloRequest(pathname, init = {}) {
  const response = await fetch(`${haloApiBase}${pathname}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${HALO_PAT}`,
      ...(init.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Halo API request failed: ${response.status} ${response.statusText} - ${body}`);
  }

  return response.json();
}

async function renderMarkdown(markdown) {
  const response = await fetch("https://api.github.com/markdown", {
    method: "POST",
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${GITHUB_TOKEN}`,
      "Content-Type": "application/json",
      "User-Agent": "halo-plugin-extra-api-release-bot",
    },
    body: JSON.stringify({
      text: markdown,
      mode: "gfm",
      context: GITHUB_REPOSITORY,
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to render GitHub markdown: ${response.status} ${response.statusText}`);
  }

  return response.text();
}

async function resolveReleaseMetadata() {
  if (hasDirectReleaseMetadata) {
    return {
      name: RELEASE_NAME,
      body: fs.readFileSync(RELEASE_NOTES_FILE, "utf8"),
      prerelease: RELEASE_PRERELEASE === "true",
    };
  }

  return githubRequest(`/releases/tags/${GITHUB_RELEASE_TAG}`);
}

async function uploadAssets(releaseName, assets) {
  for (const asset of assets) {
    const assetPath = path.join(ASSETS_DIR, asset);
    const formData = new FormData();
    formData.append("releaseName", releaseName);
    formData.append("file", new Blob([fs.readFileSync(assetPath)]), asset);

    await haloRequest("/apis/uc.api.developer.store.halo.run/v1alpha1/assets", {
      method: "POST",
      body: formData,
    });
  }
}

async function main() {
  const assets = listAssets();
  const manifest = readPluginManifest(path.join(ASSETS_DIR, assets[0]));

  if (!manifest.appId) {
    throw new Error("Halo app id is missing. Set HALO_APP_ID or store.halo.run/app-id in plugin.yaml.");
  }

  const release = await resolveReleaseMetadata();
  const markdown = `${release.body || ""}`;
  const html = await renderMarkdown(markdown);

  const appRelease = await haloRequest(
    `/apis/uc.api.developer.store.halo.run/v1alpha1/releases?applicationName=${manifest.appId}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        release: {
          apiVersion: "store.halo.run/v1alpha1",
          kind: "Release",
          metadata: {
            generateName: "app-release-",
            name: "",
          },
          spec: {
            applicationName: "",
            displayName: release.name,
            draft: false,
            ownerName: "",
            preRelease: release.prerelease,
            requires: manifest.requires,
            version: manifest.version,
            notesName: "",
          },
        },
        notes: {
          apiVersion: "store.halo.run/v1alpha1",
          html,
          kind: "Content",
          metadata: {
            generateName: "app-release-notes-",
            name: "",
          },
          rawType: "MARKDOWN",
          raw: markdown,
        },
        makeLatest: !release.prerelease,
      }),
    },
  );

  await uploadAssets(appRelease.metadata.name, assets);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
