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
package org.gecko.eclipse.compatibility.equinox.config;

import static org.gecko.eclipse.api.BndEclipseConstants.ARCH;
import static org.gecko.eclipse.api.BndEclipseConstants.CLEAN;
import static org.gecko.eclipse.api.BndEclipseConstants.CONDITION_EQUINOX_CONFIG;
import static org.gecko.eclipse.api.BndEclipseConstants.CONFIGURATION;
import static org.gecko.eclipse.api.BndEclipseConstants.CONSOLE;
import static org.gecko.eclipse.api.BndEclipseConstants.CONSOLE_LOG;
import static org.gecko.eclipse.api.BndEclipseConstants.DATA;
import static org.gecko.eclipse.api.BndEclipseConstants.DEBUG;
import static org.gecko.eclipse.api.BndEclipseConstants.DEV;
import static org.gecko.eclipse.api.BndEclipseConstants.LAUNCHER;
import static org.gecko.eclipse.api.BndEclipseConstants.NL;
import static org.gecko.eclipse.api.BndEclipseConstants.NL_EXTENSIONS;
import static org.gecko.eclipse.api.BndEclipseConstants.NOEXIT;
import static org.gecko.eclipse.api.BndEclipseConstants.OS;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_ARCH;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_CLEAN;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_CONSOLE;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_CONSOLE_LOG;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_DEBUG;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_DEV;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_LAUNCHER_ARGUMENTS;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_NL;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_NL_EXTENSIONS;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_NOSHUTDOWN;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_OS;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_WS;
import static org.gecko.eclipse.api.BndEclipseConstants.USER;
import static org.gecko.eclipse.api.BndEclipseConstants.WS;

import java.util.logging.Logger;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition; 

@Component(immediate = true, property = Condition.CONDITION_ID + "=" + CONDITION_EQUINOX_CONFIG)
// Capability for the bnd launcher
@Capability(namespace = "osgi.service", attribute = "objectClass:List<String>=\"java.lang.Object\"")
public class EquinoxConfigInitializerImpl implements Condition {

	private static String[] allArgs;
	private static String[] frameworkArgs;
	private static String[] appArgs;

	@Reference
	private EnvironmentInfo envInfo;
	
	@Reference(target="(" + PROP_LAUNCHER_ARGUMENTS + "=*)")
	ServiceReference<Object> launcher;
	
	private Logger logger = Logger.getLogger(EquinoxConfigInitializerImpl.class.getName());
	
	@Activate
	public void activate(ComponentContext ctx) {
		logger.fine("Setting missing Equinox Arguments");
		EquinoxConfiguration equinoxConfig = (EquinoxConfiguration) envInfo;
		
		String[] args = (String[]) launcher.getProperty(PROP_LAUNCHER_ARGUMENTS);
		
		processCommandLine(args, equinoxConfig);;
		
		equinoxConfig.setAllArgs(allArgs);
		equinoxConfig.setFrameworkArgs(frameworkArgs);
		equinoxConfig.setAppArgs(appArgs);
	}

	private static void processCommandLine(String[] args, EquinoxConfiguration equinoxConfig) {
		allArgs = args;
		if (args.length == 0) {
			frameworkArgs = args;
			return;
		}
		int[] configArgs = new int[args.length];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)

			// check if debug should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
			// simply enable debug.  Otherwise, assume that that the following arg is
			// actually the filename of an options file.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEBUG) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				equinoxConfig.setProperty(PROP_DEBUG, ""); //$NON-NLS-1$
				found = true;
			}

			// check if development mode should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
			// simply enable development mode.  Otherwise, assume that that the following arg is
			// actually some additional development time class path entries.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				equinoxConfig.setProperty(PROP_DEV, ""); //$NON-NLS-1$
				found = true;
			}
			
			// look for the clean flag.
			if (args[i].equalsIgnoreCase(CLEAN)) {
				equinoxConfig.setProperty(PROP_CLEAN, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the consoleLog flag
			if (args[i].equalsIgnoreCase(CONSOLE_LOG)) {
				equinoxConfig.setProperty(PROP_CONSOLE_LOG, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the console with no port.  
			if (args[i].equalsIgnoreCase(CONSOLE) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				equinoxConfig.setProperty(PROP_CONSOLE, ""); //$NON-NLS-1$
				found = true;
			}

			if (args[i].equalsIgnoreCase(NOEXIT)) {
				equinoxConfig.setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
				found = true;
			}

			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}
			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) { //$NON-NLS-1$
				continue;
			}
			String arg = args[++i];

			// look for the console and port.  
			if (args[i - 1].equalsIgnoreCase(CONSOLE)) {
				equinoxConfig.setProperty(PROP_CONSOLE, arg);
				found = true;
			}

			// look for the configuration location .  
			if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
				equinoxConfig.setProperty(EquinoxLocations.PROP_CONFIG_AREA, arg);
				found = true;
			}

			// look for the data location for this instance.  
			if (args[i - 1].equalsIgnoreCase(DATA)) {
				equinoxConfig.setProperty(EquinoxLocations.PROP_INSTANCE_AREA, arg);
				found = true;
			}

			// look for the user location for this instance.  
			if (args[i - 1].equalsIgnoreCase(USER)) {
				equinoxConfig.setProperty(EquinoxLocations.PROP_USER_AREA, arg);
				found = true;
			}

			// look for the launcher location
			if (args[i - 1].equalsIgnoreCase(LAUNCHER)) {
				equinoxConfig.setProperty(EquinoxLocations.PROP_LAUNCHER, arg);
				found = true;
			}
			// look for the development mode and class path entries.  
			if (args[i - 1].equalsIgnoreCase(DEV)) {
				equinoxConfig.setProperty(PROP_DEV, arg);
				found = true;
			}

			// look for the debug mode and option file location.  
			if (args[i - 1].equalsIgnoreCase(DEBUG)) {
				equinoxConfig.setProperty(PROP_DEBUG, arg);
				found = true;
			}

			// look for the window system.  
			if (args[i - 1].equalsIgnoreCase(WS)) {
				equinoxConfig.setProperty(PROP_WS, arg);
				found = true;
			}

			// look for the operating system
			if (args[i - 1].equalsIgnoreCase(OS)) {
				equinoxConfig.setProperty(PROP_OS, arg);
				found = true;
			}

			// look for the system architecture
			if (args[i - 1].equalsIgnoreCase(ARCH)) {
				equinoxConfig.setProperty(PROP_ARCH, arg);
				found = true;
			}

			// look for the nationality/language
			if (args[i - 1].equalsIgnoreCase(NL)) {
				equinoxConfig.setProperty(PROP_NL, arg);
				found = true;
			}

			// look for the locale extensions
			if (args[i - 1].equalsIgnoreCase(NL_EXTENSIONS)) {
				equinoxConfig.setProperty(PROP_NL_EXTENSIONS, arg);
				found = true;
			}

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0) {
			frameworkArgs = new String[0];
			appArgs = args;
			return;
		}
		appArgs = new String[args.length - configArgIndex];
		frameworkArgs = new String[configArgIndex];
		configArgIndex = 0;
		int j = 0;
		int k = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex]) {
				frameworkArgs[k++] = args[i];
				configArgIndex++;
			} else
				appArgs[j++] = args[i];
		}
		return;
	}
	
}
