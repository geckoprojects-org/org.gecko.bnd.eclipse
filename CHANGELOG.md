# Changelog

## 2.0.0

### Requirements

- **Java 21** is required, also for the JVM running Bndtools / Gradle (Eclipse 4.41 and the Equinox
  launcher 1.8 are Java 21).
- **bnd / Bndtools 7.4** or newer.
- The target platform is **Eclipse 4.41 (2026-09)**. The `geckoBndEclipse` library now adds the
  Eclipse Platform p2 repository instead of the Eclipse 2020-06 index.
- New Maven group id: **`org.geckoprojects.bnd.eclipse`** (1.x: `org.geckoprojects.bnd`).

### Changes

- Native launcher executables and libraries of Eclipse 4.41 (`eclipse_11924.dll` / `.so`).
- Fixed the exported `.ini`: 1.x wrote `-library` instead of `--launcher.library`, so the native
  launcher could not find its companion library and the exported product did not start.
- Command line arguments processed by the Eclipse launcher are passed through to the application.
- The splash screen works with the Equinox launcher 1.8, where `JNIBridge` is no longer public, and is
  taken down automatically when an Eclipse application is running.
- `@RequireEclipseE4` no longer requires `com.google.guava`, which is not part of Eclipse anymore.
- New **E3 RCP** project template; the **E4 RCP** template contains sample parts and handlers.
  Both use `jakarta.inject`, Java 21 and run bundles resolved against Eclipse 4.41, and come with
  Windows, Linux and macOS run descriptors.
- Fixed the help pages of the project templates in the Bndtools wizard.

### Migrating from 1.x

1. Run Bndtools / Gradle with Java 21 and update to bnd 7.4; set `-runee: JavaSE-21` and
   `-Dosgi.requiredJavaVersion=21` (if used) in your bndrun files.
2. Switch to `org.geckoprojects.bnd.eclipse:org.gecko.bnd.eclipse.library:2.0.0` and adjust version
   ranges on the `-runpath` to `[2.0.0,3)`.
3. Update `-runfw` to `org.eclipse.osgi;version='[3.24.300,4)'`.
4. E4 dependency injection uses `jakarta.inject` only: replace `javax.inject.Inject` with
   `jakarta.inject.Inject` (and `javax.annotation.*` with `jakarta.annotation.*`), and use
   `jakarta.inject.jakarta.inject-api` / `jakarta.annotation-api` on the `-buildpath`.
5. Several platform fragments were removed or renamed in Eclipse; update the OS specific
   `-runrequires` / `-runbundles`:

   | Old | New |
   |---|---|
   | `org.eclipse.equinox.security.win32.x86_64` | `org.eclipse.equinox.security.win32` |
   | `org.eclipse.equinox.security.linux.x86_64` | `org.eclipse.equinox.security.linux` |
   | `org.eclipse.e4.ui.swt.win32`, `org.eclipse.e4.ui.swt.gtk` | removed |
   | `org.eclipse.core.filesystem.win32.x86_64`, `org.eclipse.core.resources.win32.x86_64` | removed |
   | `org.eclipse.core.net.win32.x86_64`, `org.eclipse.core.net.linux.x86_64` | removed |
   | `org.eclipse.osgi.services` | removed, use the `org.osgi.service.*` bundles |

   The security fragments need their host `org.eclipse.equinox.security` in the run bundles.
6. Re-resolve your bndrun files. `org.osgi.service.condition` is no longer needed as a bundle,
   Equinox 3.24 provides it.

## 1.2.0

Last release for Java 8 (bytecode level) and Eclipse 2020-06, group id `org.geckoprojects.bnd`.
