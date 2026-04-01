import fs from "node:fs/promises";
import path from "node:path";

const githubDirPath = ".github";
const workflowsDirPath = path.join(githubDirPath, "workflows");
const actionsDirPath = path.join(githubDirPath, "actions");
const setupActionPath = path.join(actionsDirPath, "setup-node-pnpm", "action.yml");
const packageJsonPaths = [
  path.join("ui", "package.json"),
  path.join("js-modules", "shiki", "package.json"),
];
const dryRun = process.argv.includes("--dry-run");
const releaseDelayMs = 24 * 60 * 60 * 1000;
const eligibleReleaseCutoff = new Date(Date.now() - releaseDelayMs);

const appendGitHubOutput = async (name, value) => {
  const outputPath = process.env.GITHUB_OUTPUT;

  if (!outputPath) {
    return;
  }

  await fs.appendFile(outputPath, `${name}=${value}\n`);
};

const compareNumbers = (left, right) => left - right;

const detectJsonIndent = (content) => {
  const match = content.match(/\n( +)"/u);
  return match ? match[1].length : 2;
};

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");

const isNotFoundError = (error) =>
  Boolean(error) && typeof error === "object" && "code" in error && error.code === "ENOENT";

const isDateOlderThanCutoff = (value) => {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return false;
  }

  return parsed.getTime() <= eligibleReleaseCutoff.getTime();
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

const compareSemVer = (left, right) =>
  compareNumbers(left.major, right.major) ||
  compareNumbers(left.minor, right.minor) ||
  compareNumbers(left.patch, right.patch);

const stripUtf8Bom = (content) => content.replace(/^\uFEFF/u, "");

const fetchLatestNodeMajor = async () => {
  const response = await fetch("https://nodejs.org/dist/index.json", {
    headers: {
      Accept: "application/json",
      "User-Agent": "halo-plugin-extra-api-toolchain-updater",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch Node.js releases: ${response.status} ${response.statusText}`);
  }

  const releases = await response.json();
  const stableVersions = releases
    .filter((release) => isDateOlderThanCutoff(`${release.date}T00:00:00.000Z`))
    .map((release) => parseSemVer(release.version))
    .filter(Boolean)
    .sort((left, right) => compareSemVer(right, left));

  if (stableVersions.length === 0) {
    throw new Error("No eligible Node.js versions found in release index after applying release delay");
  }

  return stableVersions[0].major;
};

const fetchLatestPnpmVersion = async () => {
  const response = await fetch("https://registry.npmjs.org/pnpm", {
    headers: {
      Accept: "application/json",
      "User-Agent": "halo-plugin-extra-api-toolchain-updater",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch pnpm metadata: ${response.status} ${response.statusText}`);
  }

  const metadata = await response.json();
  const versions = Object.entries(metadata.time ?? {})
    .map(([version, publishedAt]) => ({
      version,
      publishedAt,
      parsedVersion: parseSemVer(version),
    }))
    .filter(({ version }) => version !== "created" && version !== "modified")
    .filter(({ publishedAt, parsedVersion }) => typeof publishedAt === "string" && Boolean(parsedVersion))
    .filter(({ publishedAt }) => isDateOlderThanCutoff(publishedAt))
    .sort((left, right) => compareSemVer(right.parsedVersion, left.parsedVersion));

  if (versions.length === 0) {
    throw new Error("No eligible pnpm versions found after applying release delay");
  }

  return versions[0].version;
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

const collectFilesByName = async (directoryPath, fileName) => {
  let entries;

  try {
    entries = await fs.readdir(path.resolve(process.cwd(), directoryPath), { withFileTypes: true });
  } catch (error) {
    if (isNotFoundError(error)) {
      return [];
    }

    throw error;
  }

  const filePaths = [];

  for (const entry of entries) {
    const relativePath = path.join(directoryPath, entry.name);

    if (entry.isDirectory()) {
      filePaths.push(...(await collectFilesByName(relativePath, fileName)));
      continue;
    }

    if (entry.isFile() && entry.name === fileName) {
      filePaths.push(relativePath);
    }
  }

  return filePaths.sort((left, right) => left.localeCompare(right));
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

  const nextNodeRange = `>=${nodeMajor}`;
  const nextPnpmRange = `^${pnpmVersion}`;
  const nextPackageManager = `pnpm@${pnpmVersion}`;

  if (packageJson.engines.node !== nextNodeRange) {
    packageJson.engines.node = nextNodeRange;
    updatedFields.push(`engines.node -> ${nextNodeRange}`);
  }

  if (packageJson.engines.pnpm !== nextPnpmRange) {
    packageJson.engines.pnpm = nextPnpmRange;
    updatedFields.push(`engines.pnpm -> ${nextPnpmRange}`);
  }

  if (packageJson.packageManager !== nextPackageManager) {
    packageJson.packageManager = nextPackageManager;
    updatedFields.push(`packageManager -> ${nextPackageManager}`);
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

const replaceActionDefaultValue = (content, inputName, nextValue) => {
  const inputPattern = new RegExp(
    `(${escapeRegExp(inputName)}:\\s*\\n(?:\\s+.*\\n)*?\\s+default:\\s*)(["'])(.*?)\\2`,
    "u",
  );
  const match = content.match(inputPattern);

  if (!match || match[3] === nextValue) {
    return {
      changed: false,
      nextContent: content,
    };
  }

  return {
    changed: true,
    nextContent: content.replace(inputPattern, `${match[1]}${match[2]}${nextValue}${match[2]}`),
  };
};

const updateToolchainFile = async (filePath, nodeMajor, pnpmVersion) => {
  const { content, lineEnding } = await readFileWithLineEnding(filePath);
  const updates = [];
  let nextContent = content;

  if (content.includes("actions/setup-node@")) {
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

  if (content.includes("pnpm/action-setup@")) {
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

  if (filePath === setupActionPath) {
    const { changed, nextContent: replacedContent } = replaceActionDefaultValue(
      nextContent,
      "pnpm-version",
      pnpmVersion,
    );

    if (changed && replacedContent !== nextContent) {
      nextContent = replacedContent;
      updates.push(`inputs.pnpm-version.default -> ${pnpmVersion}`);
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
  const workflowEntries = await fs.readdir(path.resolve(process.cwd(), workflowsDirPath), { withFileTypes: true });
  const workflowFiles = workflowEntries
    .filter((entry) => entry.isFile() && (entry.name.endsWith(".yml") || entry.name.endsWith(".yaml")))
    .map((entry) => path.join(workflowsDirPath, entry.name))
    .sort((left, right) => left.localeCompare(right));
  const actionFiles = await collectFilesByName(actionsDirPath, "action.yml");
  const targetFiles = [...actionFiles, ...workflowFiles];
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

console.log(`Resolved latest Node.js major after 1 day cooldown: ${nodeMajor}`);
console.log(`Resolved latest pnpm version after 1 day cooldown: ${pnpmVersion}`);

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
await appendGitHubOutput("node_major", String(nodeMajor));
await appendGitHubOutput("pnpm_version", pnpmVersion);
await appendGitHubOutput("updated_files", updateGroups.map(({ filePath }) => filePath).join(","));
await appendGitHubOutput(
  "update_count",
  String(updateGroups.reduce((count, group) => count + group.updates.length, 0)),
);

if (dryRun && updateGroups.length > 0) {
  process.exitCode = 10;
}
