# Development Guide

This guide covers how to set up, build, and debug the Vaadin IntelliJ Plugin locally.

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | 21 | Temurin distribution recommended |
| IntelliJ IDEA | **Ultimate** 2025.1 or newer | Community edition is not sufficient — the plugin depends on bundled Ultimate plugins (CSS, JavaScript, Microservices) |
| Git | any | — |

> The Gradle wrapper (`gradlew`) is included in the repo — you do not need to install Gradle separately.

## 1. Open the project

Clone the repository and open the **root folder** (`intellij-plugin/`) as a project in IntelliJ IDEA. Do **not** open the `plugin/` subfolder directly.

IntelliJ will detect the `settings.gradle.kts` at the root and offer to import it as a Gradle project. Accept the import.

## 2. Configure the JDK

Go to **File → Project Structure → SDK** and make sure a **JDK 21** SDK is selected. The Gradle build toolchain will automatically use this JDK for compilation.

If the SDK is missing, add it via **Add SDK → Download JDK** (choose Temurin 21).

## 3. Sync Gradle

After the initial import, IntelliJ will sync Gradle automatically. If it does not, or after pulling changes that touch `build.gradle.kts`, trigger a manual sync:

- Click the **elephant icon** in the Gradle tool window and choose **Reload All Gradle Projects**, or
- Run from the terminal: `./gradlew tasks`

The first sync downloads the IntelliJ Platform SDK (~1 GB). Subsequent syncs are cached and much faster.

## 4. Build the plugin

To compile and assemble the plugin JAR/ZIP:

```bash
./gradlew :plugin:build
```

To only compile (faster feedback):

```bash
./gradlew :plugin:compileKotlin
```

The distributable archive is written to `plugin/build/distributions/`.

### Code formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with ktfmt (Google style, max line width 120). Run before committing:

```bash
# Check for formatting violations
./gradlew :plugin:spotlessCheck

# Auto-fix formatting
./gradlew :plugin:spotlessApply
```

### Run tests

```bash
./gradlew :plugin:test
```

### Writing tests that need an IDE application

Tests that need a live `Application` or `Project` — anything touching services, VFS, documents,
editors or `UndoManager` — must extend the platform's JUnit 3 base classes:

| need | base class |
|---|---|
| a project on a real filesystem (files, VFS, documents) | `HeavyPlatformTestCase` |
| a project only, no real files | `BasePlatformTestCase` |
| neither | a plain JUnit 5 class — most tests belong here |

**Do not use `@TestApplication` or `projectFixture()`.** They read as the modern choice, and they
were used here until [#592](https://github.com/vaadin/intellij-plugin/pull/592). Their teardown,
`TestApplicationResource.close()`, wraps the whole shutdown in `withTimeout(20s)`, ten seconds of
which can go to `waitForAppLeakingThreads` alone. Miss the budget and `disposeTestApplication()` is
skipped — it is what clears `ApplicationManager.ourApplication` — so the Platform framework's second
teardown pass repeats every check against a disposed but still registered application and fails the
build with `AlreadyDisposedException`, while every test passes.

This is invisible on a developer machine and on GitHub PR validation. It fails on the marketplace
publishing agent, which benchmarks at roughly a quarter of reference CPU and an eighth of reference
I/O. `TestApplicationManager.disposeApplicationAndCheckForLeaks`, the path the JUnit 3 base classes
take, runs the same leak checks with no timeout at all.

One class still on `@TestApplication` is enough to arm the deadline for the whole run.

Things that bite when writing these:

- Test bodies run **on the EDT**. Overriding `runInDispatchThread()` to `false` deadlocks anything
  that calls back into the EDT — it waits for a write-intent permit the test thread holds. Override
  it only when the code under test must run *without* that lock, as `JdkUtilTest` does for the SDK
  popup.
- Methods are named `test*`, with `setUp`/`tearDown` overrides. No `@Test`, `@BeforeEach` or
  `@AfterEach` — mixing JUnit 5 annotations into a `TestCase` silently stops the class being
  collected, which is how `CopilotUndoManagerTest` sat dead and `@Disabled` for a long time.
- `junit-vintage-engine` is what lets `useJUnitPlatform()` collect a JUnit 3 `TestCase`. It is
  already declared; removing it makes these tests vanish from the run rather than fail.
- A project created by the fixture may have no base directory on disk yet. Create it in `setUp` if
  the code under test resolves paths through it.

## 5. Run the plugin in a sandbox IDE

The most important step for development: launch a **second IntelliJ IDEA instance** with the plugin installed into a sandboxed environment.

### Using the pre-configured run configuration (recommended)

The repository ships a run configuration at `.run/Run Plugin.run.xml`. It appears automatically in the run/debug configuration dropdown at the top of the IDE.

Select **"Run Plugin"** from the dropdown and click the **Run** or **Debug** button.

This executes `./gradlew runIde` behind the scenes and opens a fresh IntelliJ IDEA instance with the plugin loaded. The sandbox data lives in `build/idea-sandbox/` and is isolated from your development IDE.

### Using the terminal

```bash
./gradlew :plugin:runIde
```

Add `--debug-jvm` to expose a remote debug port (5005 by default):

```bash
./gradlew :plugin:runIde --debug-jvm
```

Then attach the IntelliJ debugger via **Run → Attach to Process** and select the running `runIde` process. Breakpoints in your plugin source files will be hit inside the sandbox IDE.

## 6. Debugging plugin code

The **"Run Plugin"** run configuration already has **"Debug server process"** and **"Reattach on restart"** enabled. This means:

1. Select the **"Run Plugin"** configuration and click the **Debug** button.
2. The sandbox IDE starts. IntelliJ automatically attaches the debugger.
3. Set breakpoints anywhere in `plugin/src/main/kotlin/`. They will be hit when the sandbox IDE triggers the corresponding code path.
4. If the sandbox IDE restarts (e.g. after a settings change), the debugger reattaches automatically.

Sandbox IDE logs are written to `build/idea-sandbox/system/log/idea.log` and are aliased in the Run console as **idea.log** for easy tailing.

## 7. Testing plugin API calls (Copilot endpoints)

The `plugin-api-test-client/` directory contains a Spring Boot application with sample Vaadin views. It exercises the HTTP endpoint API that the plugin exposes to Vaadin Copilot.

To use it:

1. Start the sandbox IDE first (step 5 above) so the plugin's HTTP server is running.
2. In a separate terminal, run the test client:
   ```bash
   cd plugin-api-test-client
   ./mvnw spring-boot:run
   ```
3. The test client's tests will talk to the plugin's REST API and let you debug handler code in the sandbox IDE.

## Project structure quick reference

```
intellij-plugin/
├── plugin/                        Main plugin module (Gradle)
│   ├── build.gradle.kts           Plugin build config (platform version, dependencies)
│   ├── src/main/kotlin/           Plugin source code
│   ├── src/main/resources/
│   │   └── META-INF/plugin.xml    Plugin manifest (actions, extensions, dependencies)
│   └── src/test/kotlin/           Unit tests
├── plugin-api-test-client/        Spring Boot app for manual API testing
├── .run/Run Plugin.run.xml        Pre-configured IntelliJ run/debug configuration
└── gradle.properties              Gradle build flags (caching, config cache)
```

## Common issues

**Gradle sync fails with "SDK not found"**
: Ensure a JDK 21 SDK is configured in Project Structure before syncing.

**`runIde` downloads the entire IntelliJ Platform on first run**
: This is expected (~1 GB). It is cached in `~/.gradle/caches/` and reused across runs.

**Breakpoints are not hit**
: Make sure you launched the "Run Plugin" configuration with the **Debug** button, not the Run button. Also confirm that the class file in the sandbox corresponds to your latest build — a Gradle incremental build runs automatically before `runIde`, but if the build is cached, trigger a clean with `./gradlew :plugin:clean runIde`.

**Spotless check fails in CI**
: Run `./gradlew :plugin:spotlessApply` locally and commit the formatted files.
