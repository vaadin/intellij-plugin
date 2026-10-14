import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

import { buildCatalog, buildVersionedCatalog, parseArguments, releaseVersions } from './generate-vaadin-css-catalog.mjs';

const source = await mkdtemp(join(tmpdir(), 'vaadin-css-catalog-'));
await mkdir(join(source, 'packages', 'button', 'src'), { recursive: true });
await mkdir(join(source, 'packages', 'vaadin-lumo-styles', 'src'), { recursive: true });

await writeFile(join(source, 'custom-elements.json'), JSON.stringify({
  modules: [{
    declarations: [{
      customElement: true,
      tagName: 'vaadin-button',
      cssProperties: [
        { name: '--vaadin-button-background' },
        { name: '--vaadin-private-property', privacy: 'private' }
      ],
      cssParts: [{ name: 'label' }, { name: 'internal', visibility: 'private' }]
    }, {
      customElement: true,
      tagName: 'private-element',
      privacy: 'private'
    }]
  }]
}));
await writeFile(join(source, 'packages', 'button', 'src', 'button-styles.js'), `
  /**
   * @customElement
   *
   * The following shadow DOM parts are available for styling:
   *
   * Part name | Description
   * ----------|-------------
   * \`label\`   | The button label.
   * \`prefix\`  | Content before the label.
   *
   * The following attributes are available for styling:
   */
  class Button { static get is() { return 'vaadin-button'; } }
  /** @customElement @private */
  class InternalButton { static get is() { return 'vaadin-internal-button'; } }
  const styles = 'color: var(--vaadin-button-text-color);';
  const ignored = '--not-a-vaadin-property';
`);
await writeFile(join(source, 'packages', 'vaadin-lumo-styles', 'src', 'colors.css'), `
  :root { --lumo-primary-color: #00f; }
`);

const catalog = await buildCatalog({ source });

assert.deepEqual(catalog.variables, [
  { name: '--lumo-primary-color' },
  { name: '--vaadin-button-background' },
  { name: '--vaadin-button-text-color' }
]);
assert.deepEqual(catalog.components, [
  {
    name: 'vaadin-button',
    parts: [{ name: 'label' }, { name: 'prefix' }]
  }
]);
const versionedCatalog = buildVersionedCatalog([
  { version: '25.0.0', catalog },
  {
    version: '25.1.0',
    catalog: {
      variables: [{ name: '--lumo-primary-color' }, { name: '--vaadin-new-property' }],
      components: [{ name: 'vaadin-button', parts: [{ name: 'label' }] }]
    }
  }
]);
assert.deepEqual(versionedCatalog.variables, [
  { name: '--lumo-primary-color', introducedIn: '25.0.0', availableIn: [{ from: '25.0.0' }] },
  { name: '--vaadin-button-background', introducedIn: '25.0.0', availableIn: [{ from: '25.0.0', until: '25.1.0' }] },
  { name: '--vaadin-button-text-color', introducedIn: '25.0.0', availableIn: [{ from: '25.0.0', until: '25.1.0' }] },
  { name: '--vaadin-new-property', introducedIn: '25.1.0', availableIn: [{ from: '25.1.0' }] }
]);
assert.deepEqual(versionedCatalog.components, [{
  name: 'vaadin-button',
  introducedIn: '25.0.0',
  availableIn: [{ from: '25.0.0' }],
  parts: [
    {
      name: 'label',
      introducedIn: '25.0.0',
      availableIn: [{ from: '25.0.0' }]
    },
    {
      name: 'prefix',
      introducedIn: '25.0.0',
      availableIn: [{ from: '25.0.0', until: '25.1.0' }]
    }
  ]
}]);
assert.deepEqual(parseArguments(['--from', '25.0.0', '--output', 'catalog.json']), {
  fromVersion: '25.0.0',
  output: resolve('catalog.json'),
});
assert.deepEqual(releaseVersions(['v25.1.0', 'v25.0.0', 'v25.0.0-alpha1', 'v24.9.9'], '25.0.0'), [
  '25.0.0',
  '25.1.0'
]);

console.log('generate-vaadin-css-catalog: ok');
