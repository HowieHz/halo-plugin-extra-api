const fs = require("node:fs");
const path = require("node:path");

const { extractReleaseVersions, syncChangelogCompareLinks } = require("./update-changelog-links");

const releaseVersion = process.argv[2];
const previousVersion = process.argv[3];
const releaseDate = process.argv[4];

if (!releaseVersion || !previousVersion || !releaseDate) {
  console.error("Usage: node .github/scripts/prepare-changelog-release.js <version> <previous-version> <date>");
  process.exit(1);
}

const changelogFilePath = path.join(process.cwd(), "CHANGELOG.md");
const content = fs.readFileSync(changelogFilePath, "utf8");
const unreleasedHeading = "## [Unreleased]";
const unreleasedIndex = content.indexOf(unreleasedHeading);

if (unreleasedIndex === -1) {
  throw new Error(`Missing ${unreleasedHeading} in ${changelogFilePath}`);
}

const versions = extractReleaseVersions(content);
if (versions.length === 0) {
  throw new Error(`No historical release headings found in ${changelogFilePath}`);
}

if (versions[0] !== previousVersion) {
  throw new Error(
    `Latest recorded release in ${changelogFilePath} is ${versions[0]}, but previous version argument is ${previousVersion}`,
  );
}

const afterHeadingIndex = unreleasedIndex + unreleasedHeading.length;
const nextHeadingIndex = content.indexOf("\n## [", afterHeadingIndex);

if (nextHeadingIndex === -1) {
  throw new Error(`Unable to locate next release heading in ${changelogFilePath}`);
}

const unreleasedBody = content.slice(afterHeadingIndex, nextHeadingIndex).trim();
if (!unreleasedBody) {
  throw new Error(`Unreleased section is empty in ${changelogFilePath}`);
}

const newReleaseBlock = `${unreleasedHeading}\n\n## [${releaseVersion}] - ${releaseDate}\n\n${unreleasedBody}\n\n`;
const updatedContent = `${content.slice(0, unreleasedIndex)}${newReleaseBlock}${content.slice(nextHeadingIndex + 1)}`;
fs.writeFileSync(changelogFilePath, updatedContent);
syncChangelogCompareLinks(changelogFilePath);
