# Working in this repository

Read [DEVELOPMENT.md](DEVELOPMENT.md) before changing build or test configuration. It documents the
project's conventions; the points below are the ones most easily got wrong.

## Tests

- **Never use `@TestApplication` or `projectFixture()`.** Tests needing a live `Application` or
  `Project` extend `HeavyPlatformTestCase` (real files) or `BasePlatformTestCase` (no real files).
  See *Writing tests that need an IDE application* in DEVELOPMENT.md for why — the JUnit 5 test
  application has a 20-second teardown budget that the marketplace publishing agent misses, which
  fails the build with `AlreadyDisposedException` while every test passes. A single class using it
  arms the deadline for the whole run.
- Those base classes are JUnit 3: `test*` method names, `setUp`/`tearDown` overrides, no `@Test`.
  JUnit 5 annotations on a `TestCase` stop it being collected, silently.
- Tests that need neither an application nor a project stay plain JUnit 5.

## Before committing

```bash
./gradlew :plugin:spotlessApply    # ktfmt, Google style, 120 columns
./gradlew :plugin:test
```

## Platform API

Prefer public API over `impl` packages and anything `@ApiStatus.Internal`; the marketplace verifier
reports internal usage, and it has had to be undone before (#566). When an `impl` class looks
necessary, check whether the method is declared on the public interface first.
