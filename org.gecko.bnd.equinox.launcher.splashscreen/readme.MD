# SplashScreen

To show a splash screen, `org.gecko.bnd.equinox.launcher.splashscreen` must be on the `-runpath` of the bndrun.
Right after the framework start, the bundle displays the BMP image configured with the **splash.location** property,
using the native Eclipse launcher library. The splash is therefore only shown when the application is started
with the native launcher of an exported product (the `launcher.library` property must be set).

The splash is taken down

- automatically when an Eclipse application reports itself as running: the bundle registers an
  `org.eclipse.osgi.service.runnable.StartupMonitor`, which Equinox calls when the application is running
  (this is the case for E4 applications started by `org.gecko.eclipse.compatibility`), or
- when the registered `Runnable` service (service property `splash=true`) is executed.

**Please note:** for applications that do not report themselves as running, somebody needs to call the `Runnable`
service at some point. Otherwise the splash screen is displayed all the time.

The splash is not shown if the `--nosplash` property is `true`.

## Example for a bndrun

```properties
-runpath: \
	org.gecko.bnd.eclipse.launcher.plugin,\
	org.gecko.bnd.equinox.launcher.splashscreen

-runproperties.splash: \
	splash.location=splash/splash.bmp

# this includeresource causes the exporter to package the given splash image
-includeresource: \
	/splash/splash.bmp=${.}/splash/splash.bmp
```
