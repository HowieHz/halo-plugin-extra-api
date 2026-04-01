import fs from "node:fs/promises";
import path from "node:path";

const githubDirPath = ".github";
const workflowsDirPath = path.join(githubDirPath, "workflows");
const packageJsonPaths = [
  path.join("ui", "package.json"),
  path.join("js-modules", "shiki", "package.json"),
];
const managedWorkflowPaths = [
  path.join(workflowsDirPath, "build-and-release.yaml"),
  path.join(workflowsDirPath, "ci-test.yaml"),
  path.join(workflowsDirPath, "update-toolchain-versions.yml"),
];
const dryRun = process.argv.includes("--dry-run");
const releaseDelayMs = 24 * 60 * 60 * 1000;
const userAgent = "halo-plugin-extra-api-toolchain-updater";

const appendGitHubOutput = async (name, value) => {
  const outputPath = process.env.GITHUB_OUTPUT;

  if (!outputPath) {
    return;
  }

  await fs.appendFile(outputPath, `${name}=${value}\n`);
};

const detectJsonIndent = (content) => {
  const match = content.match(/\n( +)"/u);
  return match ? match[1].length : 2;
};

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");

const isNotFoundError = (error) =>
  Boolean(error) && typeof error === "object" && "code" in error && error.code === "ENOENT";

const parseTimestamp = (value) => {
  if (typeof value !== "string") {
    return null;
  }

  const normalizedValue = value.trim();

  if (normalizedValue === "") {
    return null;
  }

  const timestamp = Date.parse(normalizedValue);
  return Number.isNaN(timestamp) ? null : timestamp;
};

const parseSemVer = (version) => {
  const match = /^v?(\d+)\.(\d+)\.(\d+)$/u.exec(version);

  if (!match) {
    return null;
  }

  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
  };
};

const stripUtf8Bom = (content) => content.replace(/^\uFEFF/u, "");

const hasReleaseDelayElapsed = (publishedAt, now = Date.now()) => {
  const publishedTimestamp = parseTimestamp(publishedAt);

  if (publishedTimestamp === null) {
    throw new Error(`Invalid published timestamp: ${publishedAt || "<empty>"}`);
  }

  return now - publishedTimestamp >= releaseDelayMs;
};

// The updater tracks the exact current latest release for each ecosystem.
// If that latest has not aged for a full 24 hours yet, it must not fall back to
// an older version, because creating a PR for that older version would still
// violate the latest-only cooldown rule.
const logSkippedLatest = (ecosystem, version, publishedAt) => {
  console.log(
    [
      `Skipping ${ecosystem} update because the current latest release ${version} was published at ${publishedAt}.`,
      "The updater only adopts the current latest after a full 24-hour delay and never falls back to an older version while that latest is still cooling down.",
    ].join(" "),
  );
};

const fetchLatestNodeMajor = async () => {
  const response = await fetch("https://api.github.com/repos/nodejs/node/releases/latest", {
    headers: {
      Accept: "application/vnd.github+json",
      "User-Agent": userAgent,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch the latest Node.js release: ${response.status} ${response.statusText}`);
  }

  const latestRelease = await response.json();
  const latestVersion = typeof latestRelease.tag_name === "string" ? latestRelease.tag_name.trim() : "";
  const publishedAt = typeof latestRelease.published_at === "string" ? latestRelease.published_at.trim() : "";
  const parsedVersion = parseSemVer(latestVersion);

  if (!parsedVersion) {
    throw new Error(`Invalid latest Node.js release version: ${latestVersion || "<empty>"}`);
  }

  if (!hasReleaseDelayElapsed(publishedAt)) {
    logSkippedLatest("Node.js", latestVersion, publishedAt);
    return null;
  }

  return parsedVersion.major;
};

const fetchLatestPnpmVersion = async () => {
  const response = await fetch("https://registry.npmjs.org/pnpm", {
    headers: {
      Accept: "application/json",
      "User-Agent": userAgent,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch pnpm metadata: ${response.status} ${response.statusText}`);
  }

  const metadata = await response.json();
  const latestVersion = typeof metadata["dist-tags"]?.latest === "string" ? metadata["dist-tags"].latest.trim() : "";
  const publishedAt = typeof metadata.time?.[latestVersion] === "string" ? metadata.time[latestVersion].trim() : "";

  if (!parseSemVer(latestVersion)) {
    throw new Error(`Invalid pnpm latest version: ${latestVersion || "<empty>"}`);
  }

  if (!hasReleaseDelayElapsed(publishedAt)) {
    logSkippedLatest("pnpm", latestVersion, publishedAt);
    return null;
  }

  return latestVersion;
};

const readFileWithLineEnding = async (filePath) => {
  const content = stripUtf8Bom(await fs.readFile(path.resolve(process.cwd(), filePath), "utf8"));

  return {
    content,
    lineEnding: content.includes("\r\n") ? "\r\n" : "\n",
  };
};

const maybeWriteFile = async (filePath, nextContent) => {
  if (dryRun) {
    return;
  }

  await fs.writeFile(path.resolve(process.cwd(), filePath), nextContent, "utf8");
};

const collectExistingFiles = async (filePaths) => {
  const existingFiles = [];

  for (const filePath of filePaths) {
    try {
      await fs.access(path.resolve(process.cwd(), filePath));
      existingFiles.push(filePath);
    } catch (error) {
      if (!isNotFoundError(error)) {
        throw error;
      }
    }
  }

  return existingFiles;
};

const updatePackageJson = async (filePath, nodeMajor, pnpmVersion) => {
  const { content, lineEnding } = await readFileWithLineEnding(filePath);
  const packageJson = JSON.parse(content);
  const updatedFields = [];
  const indent = detectJsonIndent(content);

  packageJson.engines ??= {};

  if (nodeMajor !== null) {
    const nextNodeRange = `>=${nodeMajor}`;

    if (packageJson.engines.node !== nextNodeRange) {
      packageJson.engines.node = nextNodeRange;
      updatedFields.push(`engines.node -> ${nextNodeRange}`);
    }
  }

  if (pnpmVersion !== null) {
    const nextPnpmRange = `^${pnpmVersion}`;
    const nextPackageManager = `pnpm@${pnpmVersion}`;

    if (packageJson.engines.pnpm !== nextPnpmRange) {
      packageJson.engines.pnpm = nextPnpmRange;
      updatedFields.push(`engines.pnpm -> ${nextPnpmRange}`);
    }

    if (packageJson.packageManager !== nextPackageManager) {
      packageJson.packageManager = nextPackageManager;
      updatedFields.push(`packageManager -> ${nextPackageManager}`);
    }
  }

  if (updatedFields.length === 0) {
    return [];
  }

  const nextContent = `${JSON.stringify(packageJson, null, indent)}${lineEnding}`;
  await maybeWriteFile(filePath, nextContent.replace(/\n/gu, lineEnding));
  return updatedFields;
};

const replaceWorkflowStepInputValue = (content, actionPattern, inputName, nextValue) => {
  const lines = content.split(/\r?\n/u);
  const inputPattern = new RegExp(
    `^(\\s*${escapeRegExp(inputName)}:\\s*)(["']?)([^"'#\\s]+)\\2(\\s*(?:#.*)?)$`,
    "u",
  );
  let changed = false;
  let stepUsesTargetAction = false;
  let withinWithBlock = false;
  let withIndent = -1;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const normalizedLine = line.replace(/^(\s*)-\s+/u, "$1");
    const trimmed = line.trim();
    const indent = line.match(/^\s*/u)?.[0].length ?? 0;

    if (/^\s*-\s+/u.test(line)) {
      stepUsesTargetAction = false;
      withinWithBlock = false;
      withIndent = -1;
    }

    if (withinWithBlock && trimmed && indent <= withIndent) {
      withinWithBlock = false;
      withIndent = -1;
    }

    if (/^\s*uses:\s*/u.test(normalizedLine)) {
      stepUsesTargetAction = actionPattern.test(normalizedLine);
    }

    if (stepUsesTargetAction && /^\s*with:\s*$/u.test(normalizedLine)) {
      withinWithBlock = true;
      withIndent = indent;
      continue;
    }

    if (!stepUsesTargetAction || !withinWithBlock) {
      continue;
    }

    const match = line.match(inputPattern);

    if (!match || match[3] === nextValue) {
      continue;
    }

    lines[index] = `${match[1]}${match[2]}${nextValue}${match[2]}${match[4]}`;
    changed = true;
  }

  return {
    changed,
    nextContent: lines.join("\n"),
  };
};

const updateToolchainFile = async (filePath, nodeMajor, pnpmVersion) => {
  const { content, lineEnding } = await readFileWithLineEnding(filePath);
  const updates = [];
  let nextContent = content;

  if (nodeMajor !== null && content.includes("actions/setup-node@")) {
    const { changed, nextContent: replacedContent } = replaceWorkflowStepInputValue(
      nextContent,
      /uses:\s*actions\/setup-node@/u,
      "node-version",
      String(nodeMajor),
    );

    if (changed && replacedContent !== nextContent) {
      nextContent = replacedContent;
      updates.push(`node-version -> ${nodeMajor}`);
    }
  }

  if (pnpmVersion !== null && content.includes("pnpm/action-setup@")) {
    const { changed, nextContent: replacedContent } = replaceWorkflowStepInputValue(
      nextContent,
      /uses:\s*pnpm\/action-setup@/u,
      "version",
      pnpmVersion,
    );

    if (changed && replacedContent !== nextContent) {
      nextContent = replacedContent;
      updates.push(`pnpm version -> ${pnpmVersion}`);
    }
  }

  if (updates.length === 0) {
    return [];
  }

  await maybeWriteFile(filePath, nextContent.replace(/\n/gu, lineEnding));
  return updates;
};

const updatePackageJsonFiles = async (nodeMajor, pnpmVersion) => {
  const targetFiles = await collectExistingFiles(packageJsonPaths);
  const updateGroups = [];

  for (const filePath of targetFiles) {
    const updates = await updatePackageJson(filePath, nodeMajor, pnpmVersion);

    if (updates.length > 0) {
      updateGroups.push({ filePath, updates });
    }
  }

  return updateGroups;
};

const updateToolchainFiles = async (nodeMajor, pnpmVersion) => {
  const targetFiles = await collectExistingFiles(managedWorkflowPaths);
  const updateGroups = [];

  for (const filePath of targetFiles) {
    const updates = await updateToolchainFile(filePath, nodeMajor, pnpmVersion);

    if (updates.length > 0) {
      updateGroups.push({ filePath, updates });
    }
  }

  return updateGroups;
};

const nodeMajor = await fetchLatestNodeMajor();
const pnpmVersion = await fetchLatestPnpmVersion();

if (nodeMajor === null) {
  console.log("Resolved latest Node.js major: skipped because the current latest release has not reached the 24-hour delay");
} else {
  console.log(`Resolved latest Node.js major after 24-hour latest-only delay: ${nodeMajor}`);
}

if (pnpmVersion === null) {
  console.log("Resolved latest pnpm version: skipped because the current latest release has not reached the 24-hour delay");
} else {
  console.log(`Resolved latest pnpm version after 24-hour latest-only delay: ${pnpmVersion}`);
}

const updateGroups = [
  ...(await updatePackageJsonFiles(nodeMajor, pnpmVersion)),
  ...(await updateToolchainFiles(nodeMajor, pnpmVersion)),
];

if (updateGroups.length === 0) {
  console.log("No toolchain version updates were required");
} else {
  for (const { filePath, updates } of updateGroups) {
    console.log(`Updated ${filePath}`);

    for (const update of updates) {
      console.log(`- ${update}`);
    }
  }
}

await appendGitHubOutput("changed", updateGroups.length > 0 ? "true" : "false");
await appendGitHubOutput("node_major", nodeMajor === null ? "" : String(nodeMajor));
await appendGitHubOutput("pnpm_version", pnpmVersion ?? "");
await appendGitHubOutput("updated_files", updateGroups.map(({ filePath }) => filePath).join(","));
await appendGitHubOutput(
  "update_count",
  String(updateGroups.reduce((count, group) => count + group.updates.length, 0)),
);

if (dryRun && updateGroups.length > 0) {
  process.exitCode = 10;
}
