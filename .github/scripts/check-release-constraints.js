import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { buildCompareLinks, extractReleaseVersions } from "./update-changelog-links.js";

const baseSha = process.env.BASE_SHA;
const isReleasePr = process.env.IS_RELEASE_PR === "true";
const changelogFilePath = path.join(process.cwd(), "CHANGELOG.md");

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

function validateReleaseLinks(content, filePath) {
  const versions = extractReleaseVersions(content);
  if (versions.length === 0) {
    throw new Error(`${filePath} must contain at least one release heading like ## [x.y.z] - yyyy-mm-dd`);
  }

  const links = new Map();
  const linkRegex =
    /^\[(Unreleased|unreleased|\d+\.\d+\.\d+)\]:\s*(https:\/\/github\.com\/HowieHz\/halo-plugin-extra-api\/(?:compare\/\S+|releases\/tag\/\S+))\s*$/gmu;

  for (const match of content.matchAll(linkRegex)) {
    const rawKey = match[1];
    const key = rawKey.toLowerCase() === "unreleased" ? "Unreleased" : rawKey;
    links.set(key, match[2]);
  }

  const expectedLinks = buildCompareLinks(versions);
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
const baseVersion = readVersionFromGradleProperties(readFromGit(baseSha, "gradle.properties"));

if (isReleasePr) {
  if (currentVersion === baseVersion) {
    throw new Error(`release PR must change gradle.properties version: ${baseVersion}`);
  }
  if (!/^\d+\.\d+\.\d+$/u.test(currentVersion)) {
    throw new Error(`gradle.properties version in release PR must be semantic version (x.y.z), got: ${currentVersion}`);
  }
  if (!isGreaterThan(currentVersion, baseVersion)) {
    throw new Error(`release PR version must be greater than base version: ${baseVersion} -> ${currentVersion}`);
  }
} else if (currentVersion !== baseVersion) {
  throw new Error(`gradle.properties version must not be changed in non-release PRs: ${baseVersion} -> ${currentVersion}`);
}

const changelogContent = fs.readFileSync(changelogFilePath, "utf8");
if (!changelogContent.includes("## [Unreleased]")) {
  throw new Error(`${changelogFilePath} must keep the ## [Unreleased] section`);
}
validateReleaseLinks(changelogContent, changelogFilePath);
