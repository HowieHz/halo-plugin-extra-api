import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const REPO_OWNER = "HowieHz";
const REPO_NAME = "halo-plugin-extra-api";
const REPO_PATH_RE = /^https:\/\/github\.com\/howiehz\/halo-plugin-extra-api\//iu;
const DEFAULT_CHANGELOG_FILE = path.join(process.cwd(), "CHANGELOG.md");
const RELEASE_HEADING_RE = /^## \[(\d+\.\d+\.\d+)\]\s*-\s*/gmu;
const TRAILING_LINK_LINE_RE =
  /^\[(?:Unreleased|unreleased|\d+\.\d+\.\d+)\]:\s*https:\/\/github\.com\/howiehz\/halo-plugin-extra-api\/(?:compare\/\S+|releases\/tag\/\S+)\s*$/iu;

export function extractReleaseVersions(content) {
  const versions = [];
  for (const match of content.matchAll(RELEASE_HEADING_RE)) {
    versions.push(match[1]);
  }
  return versions;
}

export function buildCompareLinks(versions) {
  if (versions.length === 0) {
    return [];
  }

  const links = [`[Unreleased]: https://github.com/${REPO_OWNER}/${REPO_NAME}/compare/v${versions[0]}...HEAD`];

  for (let index = 0; index < versions.length - 1; index += 1) {
    const currentVersion = versions[index];
    const previousVersion = versions[index + 1];
    links.push(
      `[${currentVersion}]: https://github.com/${REPO_OWNER}/${REPO_NAME}/compare/v${previousVersion}...v${currentVersion}`,
    );
  }

  const oldestVersion = versions[versions.length - 1];
  links.push(`[${oldestVersion}]: https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/tag/v${oldestVersion}`);
  return links;
}

export function stripTrailingCompareLinks(content) {
  const lines = content.replace(/\r\n/gu, "\n").split("\n");
  let index = lines.length - 1;

  while (index >= 0 && lines[index].trim() === "") {
    index -= 1;
  }

  let foundLinkLines = false;
  while (index >= 0 && TRAILING_LINK_LINE_RE.test(lines[index].trim())) {
    const url = lines[index].split(": ")[1] ?? "";
    if (!REPO_PATH_RE.test(url.trim())) {
      break;
    }
    foundLinkLines = true;
    index -= 1;
  }

  if (!foundLinkLines) {
    return content.replace(/\r\n/gu, "\n").trimEnd();
  }

  while (index >= 0 && lines[index].trim() === "") {
    index -= 1;
  }

  return lines
    .slice(0, index + 1)
    .join("\n")
    .trimEnd();
}

export function syncChangelogCompareLinks(filePath = DEFAULT_CHANGELOG_FILE) {
  const content = fs.readFileSync(filePath, "utf8");
  const baseContent = stripTrailingCompareLinks(content);
  const versions = extractReleaseVersions(baseContent);

  if (versions.length === 0) {
    throw new Error(`No release headings found in ${filePath}`);
  }

  const compareLinks = buildCompareLinks(versions);
  const updatedContent = `${baseContent}\n\n${compareLinks.join("\n")}\n`;
  fs.writeFileSync(filePath, updatedContent);
}

const currentFilePath = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === currentFilePath) {
  const targetFile = process.argv[2];
  syncChangelogCompareLinks(targetFile || DEFAULT_CHANGELOG_FILE);
}
