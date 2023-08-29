# Gecko BND RCP Exporter/Launcher

This aims toward enabling pure OSGi Projects and or bnd to better work with different specialties of Eclipse Equinox and the Eclipse RCP Framework. This entails:

* A BND Plugin, that allows product exports that use the native equinox launcher
* Splashscreen support
* In Framework start of the Eclipse Product/Application

!Please Note: This is explicitly no exact Product Export as the PDE would produce. The original Product export and Eclipse/Equinox start mechanism has a lot of non standard idiosyncrasies that have largely historic reasons and are nowadays unnecessary. Thus this product export works a bit different from what you currently know.! 

Everything described here will target the BND Workspace model. It should work with the maven model as well, but is untested at this point.

# HowTo

## BND Workspace template

Add the following repository as bnd workspace template: `https://gitlab.com/gecko.io/bnd-rcp-workspace-template.git`

How? open the Eclipse Preferences and go to: BNDTools -> "Workspace Template" and add it as a "Raw Git Clone URIs"  

## Get started

1.  Follow the steps here to get a preconfigured Eclipse installed: https://gitlab.com/gecko.io/bnd-workspace/-/wikis/HowTo (alternatively add the workspace template yourself. I needs to point here: https://gitlab.com/gecko.io/bnd-rcp-workspace-template.git)
2. Create a new Workspace using the Gecko RCP Workspace
3. Create a new bnd Project using the Gecko RCP Application template.
4. Have fun!

## Repositories

All Resources are provided as Maven Nexus and OBR repositories.

### Maven as BOMRepository

```
-plugin.geckorcp:\
	aQute.bnd.repository.maven.pom.provider.BndPomRepository;\
        name="Gecko Equinox RCP BOM";\
        releaseUrls="https://repo.maven.apache.org/maven2/";\
        snapshotUrls=https://devel.data-in-motion.biz/nexus/repository/dim-snapshot/;\
        revision=" org.geckoprojects.equinox:org.gecko.bnd.eclipse.bom:1.1.0"

```

### OBR

```
-plugin.geckorcpobr: \
	aQute.bnd.repository.osgi.OSGiRepository;\
		name="Gecko Equinox RCP OBR";\
		locations=https://devel.data-in-motion.biz/repository/gecko/release/geckoBNDEquinox/index.xml
```

## Bndrun configurations

| instruction      | Description                                                  |
| ---------------- | ------------------------------------------------------------ |
| `-launchericons` | A list of paths to one or more icon files. As this uses the default Eclipse launchable and mechanism it must either be one `.ico` file containing the following set of icons:<br />	 * 1. 256x256, 32 bit (RGB / Alpha Channel)<br/>	 * 2. 48x48, 32 bit (RGB / Alpha Channel)<br/>	 * 3. 32x32, 32 bit (RGB / Alpha Channel)<br/>	 * 4. 16x16, 32 bit (RGB / Alpha Channel)<br/>	 * 5. 48x48, 8 bit (256 colors)<br/>	 * 6. 32x32, 8 bit (256 colors)<br/>	 * 7. 16x16, 8 bit (256 colors)<br />Or 7 `.bmp` files in the above mentioned format, in any order.<br /><br />*!!!Providing the wrong files, can corrupt the launcher!!!*<br />**Icons are currently only supported for Windows exports** |

### Configuration Area

Eclipse provides a couple of possiblities to set the configuration area.

1. Edit the `-ini` and add 

   ``` 
   -configuration
   <myFancyPath>
   ```

2. Append the start command `myexecutable -configuration <myFancyPath>`
3. Set `-Dosgi.configuration.area=<myFancyPath> ` in the `.ini` or as java property
4. The OSGi default mechanism by setting the System or Framework property `org.osgi.framework.storage=<myFancyPath>`

The Launcher supports all options. If multiple options are set they will be evaluated in the given order, where 1 has the lowest priority and 4 the highest. Relative paths will be handled realtive to the install location.

***Please note that the use of the equinox variables like e.g. `@user.home` is currently not supported!*** 

### Splashscreen

The splash screen will now work with any OSGi Framework you want. the requirement the following bundle present on the runpath:
```
-runpath: \
	org.gecko.bnd.equinox.launcher.splashscreen
```
If our RCP Exporter is used, the native dll will be unpacked (if run from bnd or exported) and set via the `launcher.library` property. This needs to point to the necessary native `dll` or `so`. If this is present, the property `splash.location` needs to be present and point to a splash screen. If the splash is a relaitve path, it will first look for a splash realtive to where ever the java process is started from and if this fails relative to the install location.
If the argument `--nosplash` is given, the splashscreen will be supressed.

### Change the Launch mechanism

First of, how does launching work with BND in a nutshell? If you use the normal exporter, you get a self executable jar. This contains a Class that has a main method called the prelauncher. All your bundles are package the in the jar in a directory called jar. Besides the bundles from your bndrun, BND adds the biz.aQute.launcher as well. This jar is loaded by the prelauncher and the contained Launcher is triggered. This in turn creates and runs the actual Framework and installs and start all your bundles.

The original BND PreLauncher expects to to find the Launcher and bundles to be contained in its jar. The main difference is, that our PreLauncher can handle bundles in other locations. In addition to that we translate some eclipse specific properties as well.

If you want to use your own prelauncher, you can use the `-prelauncher: <somefilepath>`. Because the eclipse executable starts the PreLauncher, you need to provide a `Main-Class` Manifest header and a `public int run(String... args) throws Throwable` method in your Main Class.


## Product definition vs bndrun

This section will describe, how things you can do with the .product file can be done with bnd.

## Configure the about dialog

The PDE Product build itself doesn't do anything in regard to the about dialog. The magic happens the moment you click on the Synchronize link in the product editor. This simply updates the product defining plugin.xml. An example of your plugin xml would then look as follows:

```
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
