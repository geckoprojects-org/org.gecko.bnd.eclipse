/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.bnd.eclipse.launcher.pre;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.function.Consumer;

import org.gecko.bnd.eclipse.launcher.util.CommonUtil;
import org.osgi.framework.Constants;

import aQute.bnd.exceptions.Exceptions;

import static org.gecko.eclipse.api.BndEclipseConstants.*; 

public class EclipseLauncherConstants {

	/**
	 * The ID of the Launcher Bundle
	 */
	public static final String BUNDLE_ID = "org.gecko.bnd.eclipse.launcher";
	
	public boolean debug = false;
	public boolean initialize = false;
	public String[] vmargs;
	public String[] commands;
	public String framework;
	public String exitData;
	public String library;
	public String launcherProperties;
	public String vm;
	public String endSplash;
	public String nl;
	public String configArea;
	public URL installationLocation;
	public List<String> passThrough;

	//BND collcts its classpath for the actuall Launcher from an entry in the Manifest and expects everything to be in the executable jar or somehwere close by.
	//This extra classpath should allow for some more flexibility 
	public List<String> propBasedRunPath = new LinkedList<>();
	private boolean clean = false;
	
	public EclipseLauncherConstants(String[] originalArgs) {
		debug = Boolean.getBoolean(LAUNCH_TRACE);
		commands = originalArgs;
		passThrough = new ArrayList<>();
		if(originalArgs.length > 0) {
			Queue<String> args = new ArrayDeque<>(Arrays.asList(originalArgs));
			while (!args.isEmpty()) {
				// check for args without parameters (i.e., a flag arg)
				// check if debug should be enabled for the entire platform
				String key = args.remove();
				if (!key.startsWith("-")) { //$NON-NLS-1$
					passThrough.add(key);
					continue;
				}
				boolean processed = false;
				switch (key.toLowerCase(Locale.ROOT)) {

					// Args that would be processed by org.eclipse.equinox.launcher.Main.
					// Cover them all, even if we ignore them:

					case DEBUG:
						debug = true;
						// processed = false because we want to pass it through. If it has an arg we'll pass that through next time
						break;
					case NOSPLASH: // look for and consume the nosplash directive.
						// This supercedes any -showsplash command that might be present.
						CommonUtil.log(getClass(), "Found no Splash");
						System.setProperty(NOSPLASH, "true");
						processed = true;
						break;
					case NOEXIT:
						System.setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
						// processed = false because we want to pass it through
						break;
					case APPEND_VMARGS:
					case OVERRIDE_VMARGS:
						//just consume the --launcher.overrideVmargs and --launcher.appendVmargs
						processed = true;
						break;
					case INITIALIZE: // check if this is initialization pass
						initialize = true;
						passThrough.add(key); // pass thru this arg
						processed = true;
						break;
					case DEV: // check if development mode should be enabled for the entire platform
						// processed = false because we want to pass it through. If it has an arg we'll pass that through next time
						break;
					case SHOWSPLASH: // look for the command to use to show the splash screen
						System.setProperty(SHOWSPLASH, "true");
						consumeParameter(args, arg -> { //consume optional parameter for showsplash
							System.setProperty(PROP_SPLASHLOCATION, arg);
						});
						processed = true;
						break;
					case PROTECT:
						// Currently not supported in Gecko
						args.remove(); //consume next parameter
						processed = true;
						break;
					case VMARGS:
						// look for the VM args arg. We have to do that before looking to see
						// if the next element is a -arg as the thing following -vmargs may in
						// fact be another -arg.
						vmargs = args.toArray(new String[0]);
						args.clear(); // abort the loop after this
						processed = true;
						break;
					// All keys below expect a suitable parameter and are ignored (and passed-through) if no value is available
					case FRAMEWORK:
						processed = consumeParameter(args, arg -> { // look for the framework to run
							framework = arg;
						});
						break;
					case OS:
						// Supplied by the native executable. Pass through along with its arg
						break;
					case WS:
						// Supplied by the native executable. Pass through along with its arg
						break;
					case ARCH:
						// Supplied by the native executable. Pass through along with its arg
						break;
					case INSTALL:
						processed = consumeParameter(args, arg -> { // look for explicitly set install root
							// Consume the arg here to ensure that the launcher and Eclipse get the
							// same value as each other.
							System.setProperty(PROP_INSTALL_AREA, arg);
						});
						break;
					case CONFIGURATION:
						passThrough.add(key);
						processed = consumeParameter(args, arg -> { // look for the configuration to use.
							configArea = arg;
							passThrough.add(arg); // also pass the arg through
						});
						break;
					case EXITDATA:
						processed = consumeParameter(args, arg -> {
							exitData = arg;
						});
						break;
					case NAME:
						processed = consumeParameter(args, arg -> { // look for the name to use by the launcher
							System.setProperty(PROP_LAUNCHER_NAME, arg);
						});
						break;
					case STARTUP:
						processed = consumeParameter(args, arg -> { // look for the startup jar used
							// not doing anything with this right now, but still consume it
							//startup = arg;
						});
						break;
					case LAUNCHER:
						passThrough.add(key);
						processed = consumeParameter(args, arg -> { // look for the launcher location
							System.setProperty(PROP_LAUNCHER, arg);
							passThrough.add(arg);
						});
						break;
					case LIBRARY:
						processed = consumeParameter(args, arg -> {
							library = arg;
						});
						break;
					case ENDSPLASH:
						processed = consumeParameter(args, arg -> { // look for the command to use to end the splash screen
							endSplash = arg;
						});
						break;
					case VM:
						processed = consumeParameter(args, arg -> { // look for the VM location arg
							vm = arg;
						});
						break;
					case NL:
						passThrough.add(key);
						processed = consumeParameter(args, arg -> { // look for the nl setting
							nl = arg;
							passThrough.add(arg);
						});
						break;

					// Equinox doesn't have a case for this
					case CLEAN:
						clean = true;
						// processed = false because we want to pass it through
						break;

					// Args that originate from bnd and are not recognised by standard Eclipse:

					//XXX MANDATORY
					case LAUNCHER_PROPERTIES:
						processed = consumeParameter(args, arg -> {
							launcherProperties = arg;
						});
						break;
					//XXX MANDATORY
					case PROP_RUNPATH:
						processed = consumeParameter(args, arg -> {
							propBasedRunPath.addAll(Arrays.asList(arg.split(",")));
						});
						break;
					default:
						break;
				};
				if (!processed) {
					passThrough.add(key);
				}
			}
		}
		installationLocation = getInstallLocation();
//		splashLocation = System.getProperty(EclipseLauncherConstants.PROP_SPLASHLOCATION, splashLocation);
//		splashPath = System.getProperty(EclipseLauncherConstants.PROP_SPLASHPATH, splashPath);
		handleConfigArea();
		System.getProperties().putIfAbsent("launch.keep", !clean + "");
		
	}

	private static boolean consumeParameter(Queue<String> arguments, Consumer<String> consumer) {
		// If we are at the last argument or if the next one has a '-' as the first character, then we can't have an arg with a parameter
		if (!arguments.isEmpty() && !arguments.peek().startsWith("-")) { //$NON-NLS-1$
			consumer.accept(arguments.remove());
			return true;
		}
		return false;
	}

	/**
	 * OSGi, Equinox and bnd have different styles of handling the configuration are. The default is the {@link Constants#FRAMEWORK_STORAGE}.
	 * The BND Launcher gets the config area as the "launcher.storage.dir" and will set the {@link Constants#FRAMEWORK_STORAGE} after it created and
	 * cleaned (if set) the directory. Equinox can also have the -configuration or osgi.configuration.area.
	 * Long story short: This tries to satisfy Equinox, Felix and BND equally and still support the -configuration in the ini 
	 */
	private void handleConfigArea() {
		configArea = System.getProperty(PROP_CONFIG_AREA, getConfigArea(installationLocation, configArea));
		configArea = System.getProperty(Constants.FRAMEWORK_STORAGE, configArea);
		configArea = getConfigArea(installationLocation, configArea);
		System.getProperties().putIfAbsent(PROP_CONFIG_AREA, configArea);
		System.getProperties().putIfAbsent(Constants.FRAMEWORK_STORAGE, configArea);
		System.setProperty("launch.storage.dir", configArea);
	}
	
	private String getConfigArea(URL installationLocation, String configArea) {
		if(configArea == null) {
			configArea = "configuration/framework/";
		}
		File area = new File(configArea);
		//if it is absolute, we take it as is
		if (area.isAbsolute()) {
			return area.getAbsolutePath();
		}
		//if it is relative, we need it relative to the exe 
		URL url;
		try {
			url = new URL(installationLocation.toString() + "/" + configArea);
			area = new File(url.toURI());
			return area.getAbsolutePath();
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns url of the location this class was loaded from
	 */
	private URL getInstallLocation() {
		URL installLocation = getInstallLocationInternal();
		try {
			System.getProperties().putIfAbsent("launcher.installLocation", new File(installLocation.toURI()).getAbsolutePath());
		} catch (URISyntaxException e) {
			Exceptions.duck(e);
		}
		return installLocation;
	}
	
	private URL getInstallLocationInternal() {
		URL installLocation = null;
		// value is not set so compute the default and set the value
		String installArea = System.getProperty(PROP_INSTALL_AREA);
		if (installArea != null) {
			installLocation = CommonUtil.buildURL(installArea, true);
			if (installLocation == null)
				throw new IllegalStateException("Install location is invalid: " + installArea); //$NON-NLS-1$
			System.setProperty(PROP_INSTALL_AREA, installLocation.toExternalForm());
			if (debug)
				System.out.println("Install location:\n    " + installLocation); //$NON-NLS-1$
			return installLocation;
		}

		ProtectionDomain domain = EclipseStyleEmbeddedLauncher.class.getProtectionDomain();
		CodeSource source = null;
		URL result = null;
		if (domain != null)
			source = domain.getCodeSource();
		if (source == null || domain == null) {
			if (debug)
				System.out.println("CodeSource location is null. Defaulting the install location to file:startup.jar"); //$NON-NLS-1$
			try {
				result = new URL("file:startup.jar"); //$NON-NLS-1$
			} catch (MalformedURLException e2) {
				//Ignore
			}
		}
		if (source != null)
			result = source.getLocation();

		String path = CommonUtil.decode(result.getFile());
		// normalize to not have leading / so we can check the form
		File file = new File(path);
		path = file.toString().replace('\\', '/');
		// TODO need a better test for windows
		// If on Windows then canonicalize the drive letter to be lowercase.
		// remember that there may be UNC paths 
		if (File.separatorChar == '\\')
			if (Character.isUpperCase(path.charAt(0))) {
				char[] chars = path.toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				path = new String(chars);
			}
		if (path.toLowerCase().endsWith(".jar")) //$NON-NLS-1$
			path = path.substring(0, path.lastIndexOf('/') + 1); //$NON-NLS-1$
		if (path.toLowerCase().endsWith("/plugins/")) //$NON-NLS-1$ 
			path = path.substring(0, path.length() - "/plugins/".length()); //$NON-NLS-1$
		try {
			try {
				// create a file URL (via File) to normalize the form (e.g., put 
				// the leading / on if necessary)
				path = new File(path).toURI().toURL().getFile();
			} catch (MalformedURLException e1) {
				// will never happen.  The path is straight from a URL.  
			}
			installLocation = new URL(result.getProtocol(), result.getHost(), result.getPort(), path);
			System.setProperty(PROP_INSTALL_AREA, installLocation.toExternalForm());
		} catch (MalformedURLException e) {
			// TODO Very unlikely case.  log here.  
		}
		if (debug)
			System.out.println("Install location:\n    " + installLocation); //$NON-NLS-1$
		return installLocation;
	}
	
	/**
	 * Returns the <code>URL</code>-based class path describing where the boot classes are located.
	 * 
	 * @return the url-based class path
	 * @param base the base location
	 * @exception MalformedURLException if a problem occurs computing the class path
	 */
//	protected URL getBootPath(String base) throws IOException {
//		URL url = null;
//		if (base != null) {
//			url = CommonUtil.buildURL(base, true);
//		} else {
//			// search in the root location
//			url = getInstallLocation();
//			String path = new File(url.getFile(), "plugins").toString(); //$NON-NLS-1$
//			path = CommonUtil.searchFor(framework, path);
//			if (path == null)
//				throw new RuntimeException("Could not find framework"); //$NON-NLS-1$
//			if (url.getProtocol().equals("file")) //$NON-NLS-1$
//				url = new File(path).toURL();
//			else
//				url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
//		}
//		if (System.getProperty(PROP_FRAMEWORK) == null)
//			System.getProperties().put(PROP_FRAMEWORK, url.toExternalForm());
//		if (debug)
//			CommonUtil.log(EclipseLauncherConstants.class, "Framework located:\n    " + url.toExternalForm()); //$NON-NLS-1$
//		return url;
//	}
	
}
