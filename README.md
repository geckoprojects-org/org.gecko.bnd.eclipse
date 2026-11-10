# Gecko BND RCP Exporter/Launcher

Bndtools support for Eclipse and Eclipse RCP development: develop, launch and export Eclipse (E3 and E4)
applications from a plain [bnd workspace](https://bnd.bndtools.org/chapters/123-tour-workspace.html),
without PDE or Tycho. This entails:

* a bnd **launcher plugin** that starts Equinox like the Eclipse launcher does and exports a ready-to-run
  **Eclipse-style product** using the native Equinox launcher,
* **splash screen** support,
* the **in-framework start** of the Eclipse product/application (`eclipse.application` / `eclipse.product`),
* **requirement annotations** (`@RequireEclipseE4`, …) that pull typical Eclipse dependency sets into resolution,
* **Bndtools templates** for E3 and E4 RCP projects and an Equinox launch file,
* a **bnd workspace library** (`geckoBndEclipse`) that wires everything into your workspace.

**Please note:** this is explicitly not the exact product export the PDE would produce. The original product
export and Eclipse/Equinox start mechanism has a lot of non-standard idiosyncrasies that have largely historic
reasons and are nowadays unnecessary. Thus this product export works a bit differently from what you know.

Everything described here targets the bnd workspace model. It should work with the Maven model as well, but is
untested at this point.

See the [CHANGELOG](CHANGELOG.md) for release notes and the migration guide to 2.0.

## Requirements

| | Version 2.x |
|---|---|
| Java | 21 or newer, also for the JVM running Bndtools / Gradle (the launcher plugin is loaded into bnd) |
| bnd / Bndtools | 7.4 or newer |
| Eclipse target platform | Eclipse 4.41 (2026-09) |
| Product export | Windows x86_64, Linux x86_64 (no macOS executable yet) |

## Getting started

1. Make the library bundle available in a repository of your workspace, e.g. in the index of a
   [Maven Bnd Repository](https://bnd.bndtools.org/plugins/maven.html) pointing to Maven Central:

   ```properties
   # cnf/central.maven (repository index)
   org.geckoprojects.bnd.eclipse:org.gecko.bnd.eclipse.library:2.0.0
   ```

2. Activate the library in `cnf/build.bnd`:

   ```properties
   -library: geckoBndEclipse
   ```

   This adds two repositories to the workspace:
   - **Eclipse 4.41 (2026-09)** – the Eclipse Platform p2 repository,
   - **Bnd Eclipse Central** – all bundles of this project from Maven Central (launcher plugin, native
     executables, splash screen, runtime bundles and templates).

3. Create a project with the Bndtools wizard and choose **Eclipse E4 RCP Application Template** or
   **Eclipse E3 RCP Application Template** (category *Gecko BND Eclipse RCP Templates*), or add the
   **Gecko Equinox BND Run** launch template (category *Gecko RCP Templates*) to an existing project.

## The RCP project templates

The generated project contains a small example application and these run descriptors:

| File | Purpose |
|---|---|
| `launch_base.bndrun` | Everything that is OS independent. Add your `-runrequires` here and **resolve only this file**. |
| `launch_win32.bndrun` | `-include`s the base and adds the Windows SWT/fragments and the Windows native capability. Use it to run and export on Windows. |
| `launch_linux.bndrun` | Same for Linux (GTK, x86_64). |
| `launch_mac.bndrun` | Same for macOS (x86_64). A product export for macOS is not supported yet. |

`-runsystemcapabilities` (via the `native_capability` macro) decides which native launcher executable the export
uses. `${native_capability}` without parameters uses the system you are running on.

## Writing your own bndrun

The minimal setup (this is what the *Gecko Equinox BND Run* template creates):

```properties
-runfw: org.eclipse.osgi
-runee: JavaSE-21

# selects the native launcher executable for the export
-runsystemcapabilities: ${native_capability}

# Eclipse style launcher (local launch + product export) and the optional splash screen
-runpath: \
	org.gecko.bnd.eclipse.launcher.plugin,\
	org.gecko.bnd.equinox.launcher.splashscreen

# starts the Eclipse application / product once Equinox is configured
-runrequires: \
	bnd.identity;id='org.gecko.eclipse.compatibility'

# Eclipse UI events must be delivered on the sending thread, which the Felix EventAdmin does not do
-runblacklist: \
	bnd.identity;id='org.apache.felix.eventadmin'

-runproperties: \
	eclipse.product=my.product.id,\
	eclipse.application=org.eclipse.e4.ui.workbench.swt.E4Application
```

The workspace location (formerly `-data`) is `osgi.instance.area`. Program arguments in `-runprogramargs` and VM
arguments in `-runvm` end up in the exported `.ini` file.

### Bndrun instructions

| instruction      | Description                                                  |
| ---------------- | ------------------------------------------------------------ |
| `-launchericons` | A list of paths to one or more icon files. As this uses the default Eclipse launcher mechanism it must either be one `.ico` file containing the following set of icons:<br />1. 256x256, 32 bit (RGB / Alpha Channel)<br/>2. 48x48, 32 bit (RGB / Alpha Channel)<br/>3. 32x32, 32 bit (RGB / Alpha Channel)<br/>4. 16x16, 32 bit (RGB / Alpha Channel)<br/>5. 48x48, 8 bit (256 colors)<br/>6. 32x32, 8 bit (256 colors)<br/>7. 16x16, 8 bit (256 colors)<br />Or 7 `.bmp` files in the above mentioned format, in any order.<br /><br />*Providing the wrong files can corrupt the launcher!*<br />**Icons are currently only supported for Windows exports.** |
| `-preLauncher`   | Path to your own pre-launcher jar, see [Change the launch mechanism](#change-the-launch-mechanism). |

## Exporting a product

Export the OS specific bndrun as an *executable JAR*:

- Bndtools: open the `.bndrun`, **Export…** → *Executable JAR*
- Gradle: `./gradlew :my.project:export.launch_win32`
- bnd CLI: `bnd export -E bnd.executablejar launch_win32.bndrun`

The resulting archive is the product. Unzip it and start the executable named after the bndrun
(e.g. `launch_win32.exe`):

```
launch_win32.exe              native Eclipse launcher
launch_win32.ini              launcher configuration (pre-launcher, launcher library, runpath, VM args)
libs/eclipse_*.dll            native launcher library
plugins/                      framework, runpath and all run bundles
configuration/                launcher properties
splash/                       resources from -includeresource (e.g. the splash image)
```

A Java 21 runtime must be available on the `PATH` of the target machine.

### Configuration area

Eclipse provides a couple of possibilities to set the configuration area.

1. Edit the `.ini` and add

   ```
   -configuration
   <myFancyPath>
   ```

2. Append it to the start command: `myexecutable -configuration <myFancyPath>`
3. Set `-Dosgi.configuration.area=<myFancyPath>` in the `.ini` or as Java property
4. The OSGi default mechanism by setting the system or framework property `org.osgi.framework.storage=<myFancyPath>`

The launcher supports all options. If multiple options are set they are evaluated in the given order, where 1 has
the lowest priority and 4 the highest. Relative paths are handled relative to the install location.

***Please note that the Equinox variables like e.g. `@user.home` are currently not supported!***

## Splash screen

The splash screen works with any OSGi framework. Put the following bundle on the runpath:

```properties
-runpath: \
	org.gecko.bnd.equinox.launcher.splashscreen

-runproperties.splash: splash.location=splash/splash.bmp
-includeresource: /splash/splash.bmp=${.}/splash/splash.bmp
```

If the Gecko launcher is used, the native library is unpacked (if run from bnd or exported) and set via the
`launcher.library` property. If it is present, the property `splash.location` must point to the splash image. A
relative path is first looked up relative to where the Java process is started from, and if this fails relative to
the install location. If the argument `--nosplash` is given, the splash screen is suppressed.

The splash is taken down automatically when an Eclipse application reports itself as running (the bundle registers
an `org.eclipse.osgi.service.runnable.StartupMonitor`). For other applications call the `Runnable` service with the
property `splash=true`. See [the splash screen readme](org.gecko.bnd.equinox.launcher.splashscreen/readme.MD).

## Change the launch mechanism

How does launching work with bnd in a nutshell? If you use the normal exporter, you get a self-executable jar. It
contains a class with a main method, the pre-launcher. All your bundles are packaged in the jar in a directory called
`jar`. Besides the bundles from your bndrun, bnd adds `biz.aQute.launcher` as well. This jar is loaded by the
pre-launcher and the contained launcher is triggered, which creates and runs the actual framework and installs and
starts all your bundles.

The original bnd pre-launcher expects to find the launcher and bundles inside its jar. Our pre-launcher can handle
bundles in other locations, and it translates some Eclipse specific properties as well.

If you want to use your own pre-launcher, use `-preLauncher: <somefilepath>`. Because the Eclipse executable starts
the pre-launcher, you need to provide a `Main-Class` manifest header and a `public int run(String... args) throws
Throwable` method in your main class.

## Product definition vs bndrun

### Configure the about dialog

The PDE product build itself doesn't do anything in regard to the about dialog. The magic happens the moment you
click on the *Synchronize* link in the product editor, which simply updates the product defining `plugin.xml`. An
example of your `plugin.xml` would then look as follows:

```xml
      <product
            application="org.eclipse.e4.ui.workbench.swt.E4Application"
            name="test.app">
         <property
               name="applicationCSS"
               value="platform:/plugin/test.app/css/default.css">
         </property>
         <property
               name="appName"
               value="test.app">
         </property>
         <property
               name="aboutText"
               value="test">
         </property>
         <property
               name="aboutImage"
               value="test.png">
         </property>
      </product>
```

## Requirement annotations

Add one of these annotations (bundle `org.gecko.eclipse.api`) to any class of your bundle to pull a dependency set
into resolution:

| Annotation | Requires |
|---|---|
| `@RequireEclipseCompatibility` | `org.gecko.eclipse.compatibility` (application/product start) |
| `@RequireEclipseE4` | E4 workbench and Equinox event admin |
| `@RequireEclipseConsole` | Equinox and Eclipse UI console with Gogo |
| `@RequireEclipseIDE` | Eclipse SDK, IDE application and Ant |
| `@RequireEclipsePDE` | PDE UI, build and launching |
| `@RequireEclipseEMF` | EMF SDK (needs an EMF repository in addition to the Eclipse Platform one) |

## Maven coordinates

Group id `org.geckoprojects.bnd.eclipse`, released to Maven Central, snapshots in the
[Central snapshot repository](https://central.sonatype.com/repository/maven-snapshots/).

| Artifact | Content |
|---|---|
| `org.gecko.bnd.eclipse.library` | bnd workspace library `geckoBndEclipse` |
| `org.gecko.bnd.eclipse.launcher.plugin` | bnd launcher plugin (`-runpath`) |
| `org.gecko.bnd.equinox.launcher.splashscreen` | splash screen (`-runpath`) |
| `org.gecko.bnd.equinox.executable.win32`, `...linux` | native launcher executables and libraries |
| `org.gecko.eclipse.compatibility`, `org.gecko.eclipse.compatibility.equinox.config` | runtime bundles |
| `org.gecko.eclipse.api` | constants and requirement annotations |
| `org.gecko.eclipse.product.template` | Bndtools templates |

## Building this project

```sh
./gradlew build      # requires JDK 21+
```

`snapshot` is the development branch (publishes `-SNAPSHOT` versions), `main` holds the latest release. Releases
are built by Jenkins; the release setup comes from the `geckoDIMC` bnd library. The Nexus login is the Jenkins
credential `dim-nexus`, passed to bnd as `NEXUS_USERNAME`/`NEXUS_PASSWORD` (`-connection-settings` in `cnf/build.bnd`).

## License

[Eclipse Public License 2.0](LICENSE)
