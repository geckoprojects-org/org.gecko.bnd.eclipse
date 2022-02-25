/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *      Data In Motion - initial API and implementation
 */
package org.gecko.bnd.eclipse.launcher.pre;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

	//BND collcts its classpath for the actuall Launcher from an entry in the Manifest and expects everything to be in the executable jar or somehwere close by.
	//This extra classpath should allow for some more flexibility 
	public List<String> propBasedRunPath = new LinkedList<>();
	private boolean clean = false;
	
	public EclipseLauncherConstants(String[] args) {
		debug = Boolean.getBoolean(LAUNCH_TRACE);
		commands = args;
		if(args.length > 0) {
			int[] configArgs = new int[args.length];
			configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
			int configArgIndex = 0;
			for (int i = 0; i < args.length; i++) {
				boolean found = false;
				// check for args without parameters (i.e., a flag arg)
				// check if debug should be enabled for the entire platform
				if (args[i].equalsIgnoreCase(DEBUG)) {
					debug = true;
					// passed thru this arg (i.e., do not set found = true)
					continue;
				}
	
				// look for and consume the nosplash directive.  This supercedes any
				// -showsplash command that might be present.
				if (args[i].equalsIgnoreCase(NOSPLASH)) {
					CommonUtil.log(getClass(), "Found no Splash");
					System.setProperty(NOSPLASH, "true");
					found = true;
				}

				// look for and consume the showsplash directive.
				if (args[i].equalsIgnoreCase(SHOWSPLASH)) {
					System.setProperty(SHOWSPLASH, "true");
					found = true;
				}

				// look for and consume the initialize directive.
				if (args[i].equalsIgnoreCase(INITIALIZE)) {
					initialize = true;
					found = true;
				}

				// look for and consume the initialize directive.
				if (args[i].equalsIgnoreCase(CLEAN)) {
					clean = true;
					found = true;
				}
	
				// look for the command to use to show the splash screen
//				if (args[i].equalsIgnoreCase(SHOWSPLASH)) {
//					showSplash = true;
//					found = true;
//					//consume optional parameter for showsplash
//					if (i + 1 < args.length && !args[i + 1].startsWith("-")) { //$NON-NLS-1$
//						configArgs[configArgIndex++] = i++;
//						splashLocation = args[i];
//					}
//				}
//	
				// look for the command to use to show the splash screen
	//			if (args[i].equalsIgnoreCase(PROTECT)) {
	//				found = true;
	//				//consume next parameter
	//				configArgs[configArgIndex++] = i++;
	//				if (args[i].equalsIgnoreCase(PROTECT_MASTER) || args[i].equalsIgnoreCase(PROTECT_BASE)) {
	//					protectBase = true;
	//				}
	//			}
	
				// done checking for args.  Remember where an arg was found 
				if (found) {
					configArgs[configArgIndex++] = i;
					continue;
				}
	
				// look for the VM args arg.  We have to do that before looking to see
				// if the next element is a -arg as the thing following -vmargs may in
				// fact be another -arg.
				if (args[i].equalsIgnoreCase(VMARGS)) {
					// consume the -vmargs arg itself
					args[i] = null;
					i++;
					vmargs = new String[args.length - i];
					for (int j = 0; i < args.length; i++) {
						vmargs[j++] = args[i];
						args[i] = null;
					}
					continue;
				}
	
				// check for args with parameters. If we are at the last argument or if the next one
				// has a '-' as the first character, then we can't have an arg with a parm so continue.
				if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
					continue;
				String arg = args[++i];
	
				// look for the name to use by the launcher
				if (args[i - 1].equalsIgnoreCase(NAME)) {
					System.getProperties().put(PROP_LAUNCHER_NAME, arg);
					found = true;
				}
	
				// look for the startup jar used 
				if (args[i - 1].equalsIgnoreCase(STARTUP)) {
					//not doing anything with this right now, but still consume it
					//startup = arg;
					found = true;
				}
	
				// look for the launcher location
				if (args[i - 1].equalsIgnoreCase(LAUNCHER)) {
					//not doing anything with this right now, but still consume it
	//				launcher = arg;
					System.getProperties().put(PROP_LAUNCHER, arg);
					found = true;
				}
	
				
				//XXX MANDATORY
				if (args[i - 1].equalsIgnoreCase(LIBRARY)) {
					library = arg;
					found = true;
				}

				//XXX MANDATORY
				if (args[i - 1].equalsIgnoreCase(LAUNCHER_PROPERTIES)) {
					launcherProperties = arg;
					found = true;
				}
	
				//XXX MANDATORY
				if (args[i - 1].equalsIgnoreCase(PROP_RUNPATH)) {
					String runPath = arg;
					propBasedRunPath.addAll(Arrays.asList(runPath.split(",")));
					found = true;
				}
	
				//XXX MANDATORY if A Splashscreen needs showing
//				if (args[i - 1].equalsIgnoreCase(PROP_SPLASHLOCATION)) {
//					splashLocation = arg;
//					found = true;
//				}
	
				//XXX if no Splashlocation is set, we might use a SplashPath
//				if (args[i - 1].equalsIgnoreCase(PROP_SPLASHPATH)) {
//					splashPath = arg;
//					found = true;
//				}
	
				//XXX the language setting used to determine the splash image  
				if (args[i - 1].equalsIgnoreCase(NL)) {
					nl = arg;
					found = true;
				}
	
				
				
				if (args[i - 1].equalsIgnoreCase(NL)) {
					nl = arg;
					found = true;
				}
	
				// look for the configuration location .  
				if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
					configArea = arg;
					found = true;
				}
	
				// look for the command to use to end the splash screen
				if (args[i - 1].equalsIgnoreCase(ENDSPLASH)) {
					endSplash = arg;
					found = true;
				}
	
				// done checking for args.  Remember where an arg was found 
				if (found) {
					configArgs[configArgIndex++] = i - 1;
					configArgs[configArgIndex++] = i;
				}
			}
			// remove all the arguments consumed by this argument parsing
			String[] passThruArgs = new String[args.length - configArgIndex - (vmargs == null ? 0 : vmargs.length + 1)];
			configArgIndex = 0;
			int j = 0;
			for (int i = 0; i < args.length; i++) {
				if (i == configArgs[configArgIndex])
					configArgIndex++;
				else if (args[i] != null)
					passThruArgs[j++] = args[i];
			}
		}
		installationLocation = getInstallLocation();
//		splashLocation = System.getProperty(EclipseLauncherConstants.PROP_SPLASHLOCATION, splashLocation);
//		splashPath = System.getProperty(EclipseLauncherConstants.PROP_SPLASHPATH, splashPath);
		handleConfigArea();
		System.getProperties().putIfAbsent("launch.keep", !clean + "");
		
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
