# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A bnd/Bndtools workspace that lets you develop, launch and export **Eclipse / Eclipse RCP (E3 and E4) applications with bnd** instead of PDE/Tycho. It ships a custom bnd launcher plugin that produces an Eclipse-style product layout (native launcher executable, `.ini`, `plugins/`, `configuration/`), runtime bundles that emulate what Eclipse's `EclipseStarter` normally does, and Bndtools project/bndrun templates. User docs: `README.md`, release notes + migration: `CHANGELOG.md`.

## Build

Gradle 9 with the `biz.aQute.bnd.workspace` plugin (bnd 7.4.0 in `gradle.properties`; the bnd jars on the buildpath are pinned via `bndbaseversion` in `cnf/ext/bndtools.bnd` — keep both in sync). Requires **JDK 21+** (Eclipse 4.41 / Equinox launcher 1.8 are Java 21). Each top-level `org.gecko.*` directory is a bnd project; `cnf/` is the workspace config.

```sh
./gradlew build                                  # compile, test, package all projects
./gradlew :org.gecko.bnd.eclipse.launcher:build  # single project
./gradlew :org.gecko.bnd.eclipse.launcher.pre:test --tests '*EclipseLauncherConstantsTest'   # single test
DO_RELEASE=true NEXUS_RELEASE=false ./gradlew clean build release   # local release trial into dist/bundles (unsigned)
```

- `cnf/build.bnd` enables the bnd libraries `fennec` (`org.eclipse.fennec.bnd.library` 0.0.9: manifest defaults, `DO_RELEASE` snapshot/release versioning, signing + Portal snapshot repo when `GPG_PASSPHRASE` is set) and `fennecTest` (JUnit 5 / AssertJ / Mockito / OSGi-Test repository); versions pinned in `cnf/central.maven`. Stay on fennec 0.0.9 like `geckoprojects-org/paho`: 0.0.10 adds its own repository named `Release`, which collides with ours. `cnf/build.bnd` overrides `Bundle-Vendor` and `Bundle-License` (the license link becomes the pom's license URL, which Maven Central checks).
- Releasing follows `geckoprojects-org/paho`, see `docs/RELEASING.md`: `cnf/ext/release.bnd` sets `-releaserepo` (snapshot: `Sonatype` + `Release` = DIM Nexus; release: `Release` + `Dist` = `dist/bundles`) and the connection settings from env (`NEXUS_*`, `CS_*`); the `Jenkinsfile` signs, releases and uploads `dist/bundles` to the Central Portal with bnd's `.github/scripts/sonatype-upload.sh`; `.github/workflows/release.yml` turns tag `v<version>` into a GitHub release once the version is on Central, with the `## <version>` section of `CHANGELOG.md` as notes.
- Java target 21 (`cnf/build.bnd`). Tests are JUnit 5, set up like `eclipse-fennec/emf.odata`: `fennecTest` no longer puts JUnit on the bnd `-testpath` (only Mockito), so the root `build.gradle` adds JUnit Jupiter, the JUnit Platform launcher (required by Gradle 9), Mockito and AssertJ as `testImplementation` dependencies for all subprojects, enables `useJUnitPlatform()` (without it the tests silently would not run) and disables Gradle 9's "no tests discovered" failure (several projects have empty test folders). In Eclipse/Bndtools add the JUnit 5 library to a project with tests.
- Tests must run headless (GitHub Actions and Jenkins have no display): no SWT, no launching of products. `EclipseStyleExportTest` (launcher) exports minimal Linux/Windows products from `testresources/*.bndrun` against this workspace and checks layout, `.ini` and `launcher.properties` without starting them; it needs the `equinox.executable` bundles, hence `-dependson` in the launcher's `bnd.bnd`. The DS components are tested with Mockito (`EquinoxConfiguration`/`BundleContext` mocks). Test reports: `*/generated/test-reports/test/TEST-*.xml`.
- Repositories: `cnf/central.maven` (Maven Central: OSGi, test libs, Eclipse launcher artifacts, bnd libraries) and `cnf/ext/eclipse.maven` (Eclipse 4.41 platform, EMF, Xtext from Maven Central, registered by `cnf/ext/eclipse.bnd`). Repository plugin keys must be unique (`-plugin.5.EclipseCentral` vs `-plugin.6.Central`), otherwise one silently overrides the other.
- Version: `base-version` in `cnf/ext/version.bnd`; bump it after every release. Group id: `org.geckoprojects.bnd.eclipse`.
- License headers (EPL-2.0 SPDX style, Data In Motion) are checked in CI per `.licenserc.yaml`; new `.java` files need the header.

## Architecture

The pieces cooperate across three phases — build time (bnd plugin), launch time (pre-launcher), and OSGi runtime (DS components):

1. **`org.gecko.bnd.eclipse.launcher`** — the bnd `ProjectLauncher` plugin (`EclipseStyleProjectLauncherImpl`, built via `-sub` from `plugin.bnd` as `org.gecko.bnd.eclipse.launcher.plugin`, declared with `Launcher-Plugin:`). A user's bndrun puts it on `-runpath`. It handles local launches from Bndtools and `executable()` export, which lays out the product: runbundles under `plugins/`, `configuration/launcher.properties`, the native launcher + `.ini`, and the pre-launcher jar. Supports `-launchericons` and `-preLauncher`. It embeds bnd's `aQute.launcher.*` packages and conditionally p2/PDE internals; `Import-Package: !*` — it must be self-contained.
2. **`org.gecko.bnd.eclipse.launcher.pre`** — the `Main-Class` jar (`EclipseStyleEmbeddedLauncher`) the native executable starts. It embeds `JNIBridge.class` from `org.eclipse.equinox.launcher` (its native methods are bound to that class name). The launcher plugin embeds the *built* pre jar via `${repo;org.gecko.bnd.eclipse.launcher.pre;latest}`, so the pre project must build first. `EclipseLauncherConstants` translates Eclipse command line args/properties.
3. **`org.gecko.bnd.equinox.executable`** — resource-only; `-sub` produces `...executable.linux` and `...executable.win32` bundles containing the native `launcher` binaries (`binaries/`) and `eclipse_*.so/.dll` from the Equinox launcher fragments. They advertise `equinox.launcher` capabilities with `osgi.native.*` attributes; the export picks one via `-runsystemcapabilities` / `native_capability`. macOS export is disabled (`macosx.bnd.bak`). The `binaries/` are the `equinox.executable_root.*` p2 artifacts of the same Eclipse release as the launcher fragments in `cnf/central.maven`, and `Equinox-Lib` must match the `eclipse_*` filename in that fragment — update all three together.
4. **`org.gecko.bnd.equinox.launcher.splashscreen`** — goes on `-runpath`; shows `splash.location` at framework start via `Embedded-Activator`. Since launcher 1.8 `JNIBridge` is package-private, so it is accessed reflectively via `SplashBridge`. The splash goes down via a registered `StartupMonitor` (application running) or the registered `Runnable` service.
5. **`org.gecko.eclipse.compatibility.equinox.config`** — DS component `EquinoxConfigInitializerImpl` that, once the launcher service (with `launcher.arguments`) is present, sets up the Equinox/Eclipse environment and registers an OSGi `Condition` with id `equinoxConfig`.
6. **`org.gecko.eclipse.compatibility`** — DS component `EclipsePlatformStarter` gated on that condition; it replaces `EclipseStarter` by tracking `ApplicationDescriptor`s and starting the configured `eclipse.application`/product.
7. **`org.gecko.eclipse.api`** — shared constants (`BndEclipseConstants`) and `@RequireEclipse*` bundle-requirement annotations that pull Eclipse dependency sets into resolution.
8. **`org.gecko.eclipse.product.template`** — resource-only Bndtools templates (mustache, `{{...}}` placeholders, metatype per template): `e3-rcp` and `e4-rcp` project templates with `launch_base.bndrun` + OS-specific `launch_linux`/`launch_win32`/`launch_mac` bndruns that `-include` it, and a `gecko-rcp-run-small` bndrun template. The `-runbundles` of `launch_base.bndrun` are resolved against the Eclipse release the library's `workspace.bnd` points to — re-resolve them (generate a project from the template in a scratch workspace) when that changes. Wizard help texts are the `help=` files referenced in `bnd.bnd`.
9. **`org.gecko.bnd.eclipse.library`** — resource-only bnd **library** (`bnd.library=geckoBndEclipse`) that consumers enable with `-library: geckoBndEclipse`; it contributes `resources/template/workspace.bnd` with two Maven repositories: the curated Eclipse RCP platform `eclipse-rcp.maven` (Maven Central; deliberately no full p2 site, which would leak JUnit 6 etc. into consumer workspaces, #14) and the generated `bnd-eclipse-central.maven` listing this project's bundles (releases from Central, snapshots from the DIM Nexus, #15). When the Eclipse release changes, update `eclipse-rcp.maven` and re-resolve the templates against it. Its `buildpath.bnd` lists every published bundle — update it when adding/removing a bundle.

## Conventions

- `-buildpath` references to other projects in this workspace must use `;version=project`. The library intentionally uses `;version=snapshot` for its own bundles.
- `EclipseStyleProjectLauncherImpl` inherits `aQute.bnd.osgi.Constants`, whose fields (e.g. `LIBRARY = "-library"`) shadow the static import of `BndEclipseConstants.*` — qualify colliding constants explicitly.
- `-sub` is used in `launcher` and `equinox.executable`: the produced BSNs come from the sub-`.bnd` files, not the project directory name.
- Branches: `snapshot` is development (PRs target it), `main` is the latest release. CI: GitHub Actions runs `./gradlew build`; Jenkins (`OpenJDK21` tool) releases on `main` and publishes snapshots from `snapshot`.
