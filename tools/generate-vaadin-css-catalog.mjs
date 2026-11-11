#!/usr/bin/env node

/**
 * Builds the CSS completion catalog from a tagged vaadin/web-components checkout.
 *
 * The generated file is deliberately committed to the IntelliJ plugin. This tool is run by
 * maintainers when upgrading the Vaadin baseline; it is never invoked by the plugin at runtime.
 *
 * Usage:
 *   node tools/generate-vaadin-css-catalog.mjs \
 *     --from 25.0.0 \
 *     --output plugin/src/main/resources/vaadin/css-catalog.json
 */

import { mkdir, mkdtemp, readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { execFile as execFileCallback } from 'node:child_process';
import { promisify } from 'node:util';
import { tmpdir } from 'node:os';
import { basename, dirname, extname, join, relative, resolve, sep } from 'node:path';
import { pathToFileURL } from 'node:url';

const VARIABLE_PATTERN = /--(?:vaadin|lumo|aura)-[a-z0-9-]+/g;
const DOCUMENTED_COMPONENT_PATTERN = /\/\*\*([\s\S]*?@customElement[\s\S]*?)\*\/\s*(?:export\s+)?class\b([\s\S]{0,1600}?static\s+get\s+is\s*\(\s*\)\s*\{\s*return\s+['"](vaadin-[a-z0-9-]+)['"])/g;
const DOCUMENTED_TAG_PATTERN = /@customElement\s*\{?(vaadin-[a-z0-9-]+)\}?/;
const CSS_PART_PATTERN = /@csspart\s+([a-z0-9-]+)/g;
const SHADOW_PARTS_SECTION_PATTERN = /The following shadow DOM parts are available for styling:\s*([\s\S]*?)(?=The following attributes|@customElement)/i;
const CODE_IDENTIFIER_PATTERN = /`([a-z][a-z0-9-]*)`/g;
const SOURCE_EXTENSIONS = new Set(['.css', '.js', '.ts']);
const IGNORED_DIRECTORIES = new Set(['.git', 'node_modules', 'coverage', 'dist', 'test', 'tests', 'demo']);
const execFile = promisify(execFileCallback);

function timestamp() {
  return new Date().toISOString();
}

function usage() {
  return `Usage: node tools/generate-vaadin-css-catalog.mjs --from <first Vaadin version> --output <catalog.json>`;
}

function parseArguments(args) {
  const values = new Map();
  for (let index = 0; index < args.length; index += 2) {
    const option = args[index];
    const value = args[index + 1];
    if (!['--from', '--output'].includes(option) || !value || values.has(option)) {
      throw new Error(usage());
    }
    values.set(option, value);
  }

  if (!['--from', '--output'].every(option => values.has(option))) {
    throw new Error(usage());
  }

  return {
    output: resolve(values.get('--output')),
    fromVersion: values.get('--from')
  };
}

async function walk(directory, predicate) {
  const entries = await readdir(directory, { withFileTypes: true });
  const paths = [];
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      if (!IGNORED_DIRECTORIES.has(entry.name)) {
        paths.push(...await walk(path, predicate));
      }
    } else if (entry.isFile() && predicate(path)) {
      paths.push(path);
    }
  }
  return paths;
}

function isPublic(item) {
  return item && item.privacy !== 'private' && item.visibility !== 'private' && item.visibility !== 'protected';
}

function addCssMetadata(declaration, variables) {
  for (const property of declaration.cssProperties ?? declaration.cssCustomProperties ?? []) {
    if (isPublic(property) && typeof property.name === 'string' && VARIABLE_PATTERN.test(property.name)) {
      variables.add(property.name);
    }
    VARIABLE_PATTERN.lastIndex = 0;
  }
}

function collectManifestMetadata(value, components, variables) {
  if (!value || typeof value !== 'object') {
    return;
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      collectManifestMetadata(item, components, variables);
    }
    return;
  }

  const tagName = value.tagName ?? (value.customElement === true ? value.name : undefined);
  if (isPublic(value) && typeof tagName === 'string' && tagName.startsWith('vaadin-')) {
    components.set(tagName, value);
    addCssMetadata(value, variables);
  }

  for (const [key, child] of Object.entries(value)) {
    // The remaining traversal finds declarations nested in CEM modules and exports. Skip the
    // descriptive fields so a CSS-looking string in documentation cannot become a candidate.
    if (!['description', 'summary', 'readme'].includes(key)) {
      collectManifestMetadata(child, components, variables);
    }
  }
}

function isRelevantSourceFile(sourceRoot, path) {
  if (!SOURCE_EXTENSIONS.has(extname(path))) {
    return false;
  }
  const segments = relative(sourceRoot, path).split(sep);
  return segments[0] === 'packages' && !segments.some(segment => IGNORED_DIRECTORIES.has(segment));
}

function collectSourceComponents(content, components, sourceParts) {
  for (const match of content.matchAll(DOCUMENTED_COMPONENT_PATTERN)) {
    const documentation = match[1];
    if (/@(?:private|protected)\b/.test(documentation)) {
      continue;
    }

    const component = documentation.match(DOCUMENTED_TAG_PATTERN)?.[1] ?? match[3];
    if (!components.has(component)) {
      components.set(component, {});
    }
    const parts = sourceParts.get(component) ?? new Set();
    for (const partMatch of documentation.matchAll(CSS_PART_PATTERN)) {
      parts.add(partMatch[1]);
    }
    const shadowPartsSection = documentation.match(SHADOW_PARTS_SECTION_PATTERN)?.[1] ?? '';
    for (const partMatch of shadowPartsSection.matchAll(CODE_IDENTIFIER_PATTERN)) {
      parts.add(partMatch[1]);
    }
    sourceParts.set(component, parts);
  }
}

async function buildCatalog({ source }) {
  const manifestFiles = await walk(source, path => basename(path) === 'custom-elements.json');
  const components = new Map();
  const sourceParts = new Map();
  const variables = new Set();

  for (const manifestFile of manifestFiles) {
    const manifest = JSON.parse(await readFile(manifestFile, 'utf8'));
    collectManifestMetadata(manifest, components, variables);
  }

  // Theme and component style sources are the fallback for theme tokens that the custom-elements
  // manifest does not model. Only Vaadin's documented public namespaces are collected.
  const sourceFiles = await walk(source, path => isRelevantSourceFile(source, path));
  for (const sourceFile of sourceFiles) {
    const content = await readFile(sourceFile, 'utf8');
    for (const match of content.matchAll(VARIABLE_PATTERN)) {
      variables.add(match[0]);
    }
    // Vaadin's source JSDoc marks the element with @customElement and declares its tag in
    // static get is(). This fallback is needed because release checkouts do not contain the
    // generated Custom Elements Manifest.
    collectSourceComponents(content, components, sourceParts);
  }

  return {
    variables: [...variables].sort().map(name => ({ name })),
    components: [...components.keys()].sort().map(component => {
      const componentParts = components.get(component).cssParts ?? [];
      const publicParts = [...new Set([
        ...componentParts
          .filter(part => isPublic(part) && typeof part.name === 'string')
          .map(part => part.name),
        ...sourceParts.get(component) ?? []
      ])];
      return {
        name: component,
        parts: publicParts.sort().map(name => ({ name }))
      };
    })
  };
}

function compareVersions(left, right) {
  const leftParts = left.split('.').map(Number);
  const rightParts = right.split('.').map(Number);
  for (let index = 0; index < 3; index += 1) {
    if (leftParts[index] !== rightParts[index]) {
      return leftParts[index] - rightParts[index];
    }
  }
  return 0;
}

function releaseVersions(tags, fromVersion) {
  if (!/^\d+\.\d+\.\d+$/.test(fromVersion)) {
    throw new Error(`Starting version must be a stable semantic version, received ${fromVersion}`);
  }
  const versions = [...new Set(tags
    .map(tag => tag.match(/^v(\d+\.\d+\.\d+)$/)?.[1])
    .filter(Boolean)
    .filter(version => compareVersions(version, fromVersion) >= 0))]
    .sort(compareVersions);
  if (!versions.includes(fromVersion)) {
    throw new Error(`Vaadin Web Components does not contain release tag v${fromVersion}`);
  }
  return versions;
}

function addAvailability(snapshots, field, identityField) {
  const entries = new Map();
  let active = new Set();
  for (const { version, catalog } of snapshots) {
    const current = new Set(catalog[field].map(entry => entry[identityField]));
    for (const identity of current) {
      if (!active.has(identity)) {
        const snapshotEntry = catalog[field].find(entry => entry[identityField] === identity);
        const entry = entries.get(identity) ?? { ...snapshotEntry, introducedIn: version, availableIn: [] };
        entry.availableIn.push({ from: version });
        entries.set(identity, entry);
      }
    }
    for (const identity of active) {
      if (!current.has(identity)) {
        entries.get(identity).availableIn.at(-1).until = version;
      }
    }
    active = current;
  }
  return [...entries.values()].sort((left, right) => left[identityField].localeCompare(right[identityField]));
}

function buildVersionedCatalog(snapshots) {
  if (snapshots.length === 0) {
    throw new Error('At least one release snapshot is required');
  }
  return {
    schemaVersion: 2,
    fromVersion: snapshots[0].version,
    latestVersion: snapshots.at(-1).version,
    variables: addAvailability(snapshots, 'variables', 'name'),
    components: addComponentAvailability(snapshots)
  };
}

function addComponentAvailability(snapshots) {
  return addAvailability(snapshots, 'components', 'name').map(component => {
    const { parts: ignoredParts, ...componentWithoutParts } = component;
    const partSnapshots = snapshots.map(({ version, catalog }) => {
      const snapshotComponent = catalog.components.find(entry => entry.name === component.name);
      return { version, catalog: { parts: snapshotComponent?.parts ?? [] } };
    });
    return {
      ...componentWithoutParts,
      parts: addAvailability(partSnapshots, 'parts', 'name')
    };
  });
}

async function withTemporaryCheckout(action) {
  const temporaryDirectory = await mkdtemp(join(tmpdir(), 'vaadin-web-components-'));
  const checkout = join(temporaryDirectory, 'web-components');
  const pullStartedAt = Date.now();
  try {
    console.log(`[${timestamp()}] Pulling Vaadin Web Components into a temporary checkout`);
    await execFile('git', ['clone', '--filter=blob:none', '--no-checkout', 'https://github.com/vaadin/web-components.git', checkout]);
    await execFile('git', ['-C', checkout, 'fetch', '--tags', '--force']);
    console.log(`[${timestamp()}] Pulled Vaadin Web Components in ${Date.now() - pullStartedAt} ms`);
    return await action(checkout);
  } catch (error) {
    const details = error.stderr?.trim() || error.message;
    throw new Error(`Could not clone Vaadin Web Components: ${details}`);
  } finally {
    await rm(temporaryDirectory, { recursive: true, force: true });
  }
}

async function scanReleaseHistory(checkout, fromVersion) {
  const { stdout } = await execFile('git', ['-C', checkout, 'tag', '--list', 'v*']);
  const versions = releaseVersions(stdout.trim().split('\n'), fromVersion);
  const snapshots = [];
  console.log(`Scanning ${versions.length} Vaadin Web Components releases from ${versions[0]} through ${versions.at(-1)}`);
  for (const [index, version] of versions.entries()) {
    console.log(`[${index + 1}/${versions.length}] v${version}`);
    await execFile('git', ['-C', checkout, 'checkout', '--force', '--quiet', `v${version}`]);
    snapshots.push({ version, catalog: await buildCatalog({ source: checkout }) });
  }
  return buildVersionedCatalog(snapshots);
}

export { buildCatalog, buildVersionedCatalog, parseArguments, releaseVersions, withTemporaryCheckout };

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const options = parseArguments(process.argv.slice(2));
    const catalog = await withTemporaryCheckout(source => scanReleaseHistory(source, options.fromVersion));
    await mkdir(dirname(options.output), { recursive: true });
    await writeFile(options.output, `${JSON.stringify(catalog, null, 2)}\n`);
    console.log(`Scanned ${catalog.fromVersion} through ${catalog.latestVersion}; wrote ${catalog.variables.length} variables and ${catalog.components.length} components to ${options.output}`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
