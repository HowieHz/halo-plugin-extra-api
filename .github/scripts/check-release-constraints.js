import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { buildCompareLinks, extractReleaseVersions } from "./update-changelog-links.js";

const baseSha = process.env.BASE_SHA;
const isReleasePr = process.env.IS_RELEASE_PR === "true";
const changelogs = [
  {
    filePath: path.join(process.cwd(), "packages/halo-plugin-extra-api/CHANGELOG.md"),
    tagPrefix: "halo-plugin-extra-api@",
  },
  {
    filePath: path.join(process.cwd(), "packages/halo-plugin-nodejs-runtime/CHANGELOG.md"),
    tagPrefix: "halo-plugin-nodejs-runtime@",
  },
  {
    filePath: path.join(process.cwd(), "packages/halo-plugin-minify-html/CHANGELOG.md"),
    tagPrefix: "halo-plugin-minify-html@",
  },
];

if (!baseSha) {
  throw new Error("Missing BASE_SHA environment variable");
}

function readFromGit(ref, filePath) {
  return execFileSync("git", ["show", `${ref}:${filePath}`], { encoding: "utf8" });
}

function readVersionFromGradleProperties(content) {
  const match = content.match(/^version=(.+)$/mu);
  if (!match || !match[1] || !match[1].trim()) {
    throw new Error("Missing version entry in gradle.properties");
  }
  return match[1].trim();
}

function readPropertyFromGradleProperties(content, propertyName) {
  const escapedPropertyName = propertyName.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");
  const match = content.match(new RegExp(`^${escapedPropertyName}=(.+)$`, "mu"));
  if (!match || !match[1] || !match[1].trim()) {
    throw new Error(`Missing ${propertyName} entry in gradle.properties`);
  }
  return match[1].trim();
}

function parseSemVer(version) {
  const match = /^(\d+)\.(\d+)\.(\d+)$/u.exec(version);
  if (!match) {
    throw new Error(`Invalid semantic version: ${version}`);
  }

  return match.slice(1).map((segment) => Number(segment));
}

function isGreaterThan(left, right) {
  const [leftMajor, leftMinor, leftPatch] = parseSemVer(left);
  const [rightMajor, rightMinor, rightPatch] = parseSemVer(right);

  if (leftMajor !== rightMajor) {
    return leftMajor > rightMajor;
  }
  if (leftMinor !== rightMinor) {
    return leftMinor > rightMinor;
  }
  return leftPatch > rightPatch;
}

function validateReleaseLinks(content, filePath, tagPrefix) {
  const versions = extractReleaseVersions(content);
  if (versions.length === 0) {
    throw new Error(`${filePath} must contain at least one release heading like ## [x.y.z] - yyyy-mm-dd`);
  }

  const links = new Map();
  const linkRegex =
    /^\[(Unreleased|unreleased|\d+\.\d+\.\d+)\]:\s*(https:\/\/github\.com\/HowieHz\/halo-plugins\/(?:compare\/\S+|releases\/tag\/\S+))\s*$/gmu;

  for (const match of content.matchAll(linkRegex)) {
    const rawKey = match[1];
    const key = rawKey.toLowerCase() === "unreleased" ? "Unreleased" : rawKey;
    links.set(key, match[2]);
  }

  const expectedLinks = buildCompareLinks(versions, tagPrefix);
  const expectedMap = new Map();

  for (const line of expectedLinks) {
    const splitIndex = line.indexOf(": ");
    const key = line.slice(1, line.indexOf("]"));
    const value = line.slice(splitIndex + 2);
    expectedMap.set(key, value);
  }

  for (const [key, expectedValue] of expectedMap.entries()) {
    if (links.get(key) !== expectedValue) {
      throw new Error(`${filePath} must contain [${key}]: ${expectedValue}`);
    }
  }
}

const currentVersion = readVersionFromGradleProperties(fs.readFileSync("gradle.properties", "utf8"));
const currentNodejsRuntimeVersion = readPropertyFromGradleProperties(
  fs.readFileSync("gradle.properties", "utf8"),
  "nodejsRuntimeVersion",
);
const currentMinifyHtmlVersion = readPropertyFromGradleProperties(
  fs.readFileSync("gradle.properties", "utf8"),
  "minifyHtmlVersion",
);
const baseGradleProperties = readFromGit(baseSha, "gradle.properties");
const baseVersion = readVersionFromGradleProperties(baseGradleProperties);
let baseNodejsRuntimeVersion;
try {
  baseNodejsRuntimeVersion = readPropertyFromGradleProperties(baseGradleProperties, "nodejsRuntimeVersion");
} catch {
  baseNodejsRuntimeVersion = isReleasePr ? "0.0.0" : currentNodejsRuntimeVersion;
}
let baseMinifyHtmlVersion;
try {
  baseMinifyHtmlVersion = readPropertyFromGradleProperties(baseGradleProperties, "minifyHtmlVersion");
} catch {
  baseMinifyHtmlVersion = isReleasePr ? "0.0.0" : currentMinifyHtmlVersion;
}

if (isReleasePr) {
  const extraApiChanged = currentVersion !== baseVersion;
  const nodejsRuntimeChanged = currentNodejsRuntimeVersion !== baseNodejsRuntimeVersion;
  const minifyHtmlChanged = currentMinifyHtmlVersion !== baseMinifyHtmlVersion;

  if (!extraApiChanged && !nodejsRuntimeChanged && !minifyHtmlChanged) {
    throw new Error("release PR must change gradle.properties version, nodejsRuntimeVersion, or minifyHtmlVersion");
  }
  if (extraApiChanged && !/^\d+\.\d+\.\d+$/u.test(currentVersion)) {
    throw new Error(`gradle.properties version in release PR must be semantic version (x.y.z), got: ${currentVersion}`);
  }
  if (extraApiChanged && !isGreaterThan(currentVersion, baseVersion)) {
    throw new Error(`release PR version must be greater than base version: ${baseVersion} -> ${currentVersion}`);
  }
  if (nodejsRuntimeChanged && !/^\d+\.\d+\.\d+$/u.test(currentNodejsRuntimeVersion)) {
    throw new Error(
      `gradle.properties nodejsRuntimeVersion in release PR must be semantic version (x.y.z), got: ${currentNodejsRuntimeVersion}`,
    );
  }
  if (nodejsRuntimeChanged && !isGreaterThan(currentNodejsRuntimeVersion, baseNodejsRuntimeVersion)) {
    throw new Error(
      `release PR nodejsRuntimeVersion must be greater than base version when changed: ${baseNodejsRuntimeVersion} -> ${currentNodejsRuntimeVersion}`,
    );
  }
  if (minifyHtmlChanged && !/^\d+\.\d+\.\d+$/u.test(currentMinifyHtmlVersion)) {
    throw new Error(
      `gradle.properties minifyHtmlVersion in release PR must be semantic version (x.y.z), got: ${currentMinifyHtmlVersion}`,
    );
  }
  if (minifyHtmlChanged && !isGreaterThan(currentMinifyHtmlVersion, baseMinifyHtmlVersion)) {
    throw new Error(
      `release PR minifyHtmlVersion must be greater than base version when changed: ${baseMinifyHtmlVersion} -> ${currentMinifyHtmlVersion}`,
    );
  }
} else if (currentVersion !== baseVersion) {
  throw new Error(`gradle.properties version must not be changed in non-release PRs: ${baseVersion} -> ${currentVersion}`);
} else if (currentNodejsRuntimeVersion !== baseNodejsRuntimeVersion) {
  throw new Error(
    `gradle.properties nodejsRuntimeVersion must not be changed in non-release PRs: ${baseNodejsRuntimeVersion} -> ${currentNodejsRuntimeVersion}`,
  );
} else if (currentMinifyHtmlVersion !== baseMinifyHtmlVersion) {
  throw new Error(
    `gradle.properties minifyHtmlVersion must not be changed in non-release PRs: ${baseMinifyHtmlVersion} -> ${currentMinifyHtmlVersion}`,
  );
}

for (const changelog of changelogs) {
  const changelogContent = fs.readFileSync(changelog.filePath, "utf8");
  if (!changelogContent.includes("## [Unreleased]")) {
    throw new Error(`${changelog.filePath} must keep the ## [Unreleased] section`);
  }
  validateReleaseLinks(changelogContent, changelog.filePath, changelog.tagPrefix);
}
