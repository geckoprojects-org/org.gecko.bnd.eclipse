# SplashScreen

In Order to show a SplashScreen the ```org.gecko.bnd.equinox.launcher.splashscreen```must be part of the projects runpath. This bundle displays the Splash screen set via the **splash.location** property right after the framework start and registers a SplashHandler as ```Runnable``` service in the Framework. When the Runnable is executed, the  SplashScreen will be taken down. 

**Please Note:**  Somebody needs to call the run method at some point. If not, the SplashScreen will be displayed all the time.



## Example for a bndrun

```properties
-runpath: \
	org.gecko.bnd.equinox.launcher.splashscreen

-runbundles: \
	org.apache.felix.gogo.command;version='[1.0.2,1.0.3)',\
	org.apache.felix.gogo.runtime;version='[1.1.0,1.1.1)',\
	org.apache.felix.gogo.shell;version='[1.1.0,1.1.1)',\
	<yourBundleThatRemovesTheSplash>;version='[1.0.0,1.0.1)'



-runproperties:\
	splash.location=splash/splash.bmp
# this includeresource, will cause the exporter to package the given splash 	
-includeresource: \
	/splash/splash.bmp=${.}/splash/splash.bmp
```

